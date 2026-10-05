package com.mohistmc.youer.threading;

import com.mojang.logging.LogUtils;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.ticks.LevelTicks;
import org.bukkit.World;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;

public final class ThreadedRegionScheduler {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final AtomicInteger THREAD_ID = new AtomicInteger();
    private static volatile ThreadedRegionScheduler instance;
    private static volatile boolean acceptingTasks = true;

    private final ExecutorService[] workers;
    private final int chunkShift;
    private final Object stateLock = new Object();
    private final Long2ObjectOpenHashMap<List<RegionTask>> tasksByDeadline = new Long2ObjectOpenHashMap<>();
    private final java.util.Set<RegionTask> tasks = ConcurrentHashMap.newKeySet();
    private volatile boolean shuttingDown;
    private long tickCount;
    private RegionTopology topology;
    private java.util.Set<RegionKey> reservedSections;
    private List<RegionTask> readyTasks = List.of();
    private CompletableFuture<Void> lastPhase = CompletableFuture.completedFuture(null);
    private boolean phasePrepared;
    private final List<InternalTask> internalTasks = new ArrayList<>();

    private record InternalTask(RegionKey key, Runnable action, CompletableFuture<Void> completion) {}

    private ThreadedRegionScheduler(int workerCount) {
        this.chunkShift = ThreadedServer.regionChunkShift();
        this.workers = new ExecutorService[Math.max(1, workerCount)];
        for (int i = 0; i < this.workers.length; i++) {
            int workerId = i;
            this.workers[i] = Executors.newSingleThreadExecutor(namedFactory("TanukiMC Region Worker " + workerId + " - "));
        }
    }

    public static ThreadedRegionScheduler get() {
        ThreadedRegionScheduler scheduler = instance;
        int workers = ThreadedServer.workerCount();
        if (scheduler == null) {
            synchronized (ThreadedRegionScheduler.class) {
                if (!acceptingTasks) {
                    throw new IllegalStateException("Region scheduler is shutting down");
                }
                scheduler = instance;
                if (scheduler == null) {
                    instance = scheduler = new ThreadedRegionScheduler(workers);
                }
            }
        }
        return scheduler;
    }

    static int activeChunkShift(int configured) {
        ThreadedRegionScheduler scheduler = instance;
        return scheduler == null ? configured : scheduler.chunkShift;
    }

    public static void shutdown() {
        synchronized (ThreadedRegionScheduler.class) {
            acceptingTasks = false;
            ThreadedRegionScheduler scheduler = instance;
            instance = null;
            if (scheduler != null) {
                scheduler.shutdownWorkers();
            }
        }
    }

    public static void cancelPluginTasks(Plugin plugin) {
        Objects.requireNonNull(plugin, "Plugin may not be null");
        ThreadedRegionScheduler scheduler = instance;
        if (scheduler != null) {
            scheduler.cancelTasks(plugin);
        }
    }

    public ScheduledTask execute(Plugin plugin, World world, int chunkX, int chunkZ, Consumer<ScheduledTask> task, long delayTicks, long periodTicks) {
        Objects.requireNonNull(plugin, "Plugin may not be null");
        Objects.requireNonNull(world, "World may not be null");
        Objects.requireNonNull(task, "Task may not be null");
        if (!plugin.isEnabled()) {
            throw new IllegalPluginAccessException("Plugin attempted to register task while disabled");
        }
        if (!acceptingTasks) {
            throw new IllegalPluginAccessException("Plugin attempted to register task while the region scheduler is shutting down");
        }
        if (this.shuttingDown) {
            throw new IllegalPluginAccessException("Plugin attempted to register task while the region scheduler is shutting down");
        }
        RegionTask regionTask = new RegionTask(plugin, RegionKey.fromChunk(world, chunkX, chunkZ), task, periodTicks);
        this.tasks.add(regionTask);
        this.schedule(regionTask, delayTicks);
        if (!plugin.isEnabled()) {
            regionTask.cancel();
        }
        return regionTask;
    }

    public CompletableFuture<Void> executeInternal(World world, int chunkX, int chunkZ, Runnable run) {
        Objects.requireNonNull(world, "World may not be null");
        Objects.requireNonNull(run, "Runnable may not be null");
        CompletableFuture<Void> future = new CompletableFuture<>();
        if (ThreadedServer.owns(world, chunkX, chunkZ)) {
            try {
                run.run();
                future.complete(null);
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
            return future;
        }
        RegionKey regionKey = RegionKey.fromChunk(world, chunkX, chunkZ);
        if (this.shuttingDown) {
            future.complete(null);
            return future;
        }
        if (!this.phasePrepared || Thread.currentThread() != net.minecraft.server.MinecraftServer.getServer().serverThread) {
            throw new IllegalStateException("Internal region tasks must be submitted by the prepared server tick");
        }
        // Finish collecting every world's entities before callbacks can mutate them.
        this.internalTasks.add(new InternalTask(regionKey, run, future));
        return future;
    }

    public void cancelTasks(Plugin plugin) {
        Objects.requireNonNull(plugin, "Plugin may not be null");
        for (RegionTask task : this.tasks) {
            if (task.plugin == plugin) {
                task.cancel();
            }
        }
        synchronized (this.stateLock) {
            this.tasksByDeadline.values().removeIf(deadlineTasks -> {
                deadlineTasks.removeIf(task -> task.plugin == plugin || task.isCancelled());
                return deadlineTasks.isEmpty();
            });
        }
    }

    public void beginTick() {
        net.minecraft.server.MinecraftServer server = net.minecraft.server.MinecraftServer.getServer();
        if (Thread.currentThread() != server.serverThread || this.phasePrepared || !this.lastPhase.isDone()) {
            throw new IllegalStateException("Region ownership can only change between tick phases on the server thread");
        }
        java.util.Set<RegionKey> sections = new java.util.HashSet<>();
        synchronized (this.stateLock) {
            this.tickCount++;
            List<RegionTask> due = this.tasksByDeadline.remove(this.tickCount);
            this.readyTasks = due == null ? List.of() : due;
            for (RegionTask task : this.readyTasks) {
                task.scheduledDeadline = 0L;
            }
        }
        for (RegionTask task : this.readyTasks) {
            sections.add(task.regionKey);
        }
        for (net.minecraft.server.level.ServerLevel level : server.getAllLevels()) {
            World world = level.getWorld();
            for (net.minecraft.server.level.ChunkHolder holder : level.getChunkSource().chunkMap.visibleChunkMap.values()) {
                sections.add(RegionKey.fromChunk(world, holder.getPos().x, holder.getPos().z));
            }
            for (net.minecraft.world.entity.Entity entity : level.getEntities().getAll()) {
                net.minecraft.world.level.ChunkPos pos = entity.chunkPosition();
                sections.add(RegionKey.fromChunk(world, pos.x, pos.z));
            }
        }
        this.reservedSections = sections;
        this.phasePrepared = true;
    }

    public CompletableFuture<Void> tick() {
        if (!this.phasePrepared) {
            throw new IllegalStateException("Region tick has no ownership snapshot");
        }
        this.phasePrepared = false;
        for (InternalTask task : this.internalTasks) {
            this.reservedSections.add(task.key());
        }
        this.topology = RegionTopology.create(this.reservedSections, this.chunkShift);
        this.reservedSections = null;
        List<CompletableFuture<Void>> completions = new ArrayList<>();
        for (InternalTask task : this.internalTasks) {
            completions.add(task.completion());
        }
        for (RegionTopology.Batch<InternalTask> batch : this.topology.partition(this.internalTasks, InternalTask::key)) {
            this.dispatchInternal(batch);
        }
        this.internalTasks.clear();
        for (RegionTask task : this.readyTasks) {
            completions.add(this.dispatch(task));
        }
        this.readyTasks = List.of();
        this.lastPhase = CompletableFuture.allOf(completions.toArray(CompletableFuture[]::new));
        return this.lastPhase;
    }

    public <T> void tickScheduled(ServerLevel level, LevelTicks<T> ticks, long time, int limit,
                                  BiConsumer<BlockPos, T> action) {
        net.minecraft.server.MinecraftServer server = net.minecraft.server.MinecraftServer.getServer();
        if (Thread.currentThread() != server.serverThread || this.phasePrepared || !this.lastPhase.isDone()) {
            throw new IllegalStateException("Scheduled region ticks require a completed task phase on the server thread");
        }
        World world = level.getWorld();
        java.util.concurrent.atomic.AtomicReference<RegionTopology> frame = new java.util.concurrent.atomic.AtomicReference<>();
        ticks.tickRegions(time, limit, action,
            pendingChunks -> {
                java.util.Set<RegionKey> workSections = new java.util.HashSet<>();
                for (long chunk : pendingChunks) {
                    workSections.add(RegionKey.fromChunk(world, ChunkPos.getX(chunk), ChunkPos.getZ(chunk)));
                }
                frame.set(this.worldTopology(level, workSections));
            },
            chunk -> {
                RegionKey key = RegionKey.fromChunk(world, ChunkPos.getX(chunk), ChunkPos.getZ(chunk));
                RegionKey identity = frame.get().owner(key).identity();
                return ChunkPos.asLong(identity.regionX(), identity.regionZ());
            },
            (region, work) -> this.dispatchOwned(frame.get(),
                new RegionKey(world.getUID(), ChunkPos.getX(region), ChunkPos.getZ(region)), work),
            completion -> awaitWorldPhase(server, completion));
    }

    public void tickChunks(ServerLevel level, List<LevelChunk> chunks, int randomTickSpeed) {
        World world = level.getWorld();
        this.tickOwned(level, chunks,
            chunk -> RegionKey.fromChunk(world, chunk.getPos().x, chunk.getPos().z),
            chunk -> level.getChunkSource().tickChunkRegion(chunk, randomTickSpeed));
    }

    public void tickBlockEntities(ServerLevel level, List<TickingBlockEntity> tickers,
                                  Consumer<TickingBlockEntity> action) {
        World world = level.getWorld();
        this.tickOwned(level, tickers,
            ticker -> RegionKey.fromChunk(world, ticker.getPos().getX() >> 4, ticker.getPos().getZ() >> 4), action);
    }

    public void tickEntities(ServerLevel level, List<Entity> entities, Consumer<Entity> action) {
        World world = level.getWorld();
        this.tickOwned(level, entities,
            entity -> RegionKey.fromChunk(world, entity.chunkPosition().x, entity.chunkPosition().z), action);
    }

    private <T> void tickOwned(ServerLevel level, List<T> items, Function<T, RegionKey> position, Consumer<T> action) {
        net.minecraft.server.MinecraftServer server = net.minecraft.server.MinecraftServer.getServer();
        if (Thread.currentThread() != server.serverThread || this.phasePrepared || !this.lastPhase.isDone()) {
            throw new IllegalStateException("World region ticks require a completed task phase on the server thread");
        }
        java.util.Set<RegionKey> workSections = new java.util.HashSet<>();
        for (T item : items) {
            workSections.add(position.apply(item));
        }
        RegionTopology frame = this.worldTopology(level, workSections);
        List<CompletableFuture<Void>> completions = new ArrayList<>();
        for (RegionTopology.Batch<T> batch : frame.partition(items, position)) {
            completions.add(this.dispatchOwned(frame, batch.owner().identity(), () -> {
                for (T item : batch.entries()) {
                    action.accept(item);
                }
            }));
        }
        CompletableFuture<Void> completion = CompletableFuture.allOf(completions.toArray(CompletableFuture[]::new));
        try {
            awaitWorldPhase(server, completion);
        } catch (RuntimeException | Error failure) {
            try {
                completion.join();
            } catch (RuntimeException | Error executionFailure) {
                if (executionFailure != failure) {
                    failure.addSuppressed(executionFailure);
                }
            }
            throw failure;
        }
        completion.join();
    }

    private RegionTopology worldTopology(ServerLevel level) {
        return this.worldTopology(level, List.of());
    }

    private RegionTopology worldTopology(ServerLevel level, java.util.Collection<RegionKey> workSections) {
        World world = level.getWorld();
        java.util.Set<RegionKey> sections = new java.util.HashSet<>(workSections);
        for (net.minecraft.server.level.ChunkHolder holder : level.getChunkSource().chunkMap.visibleChunkMap.values()) {
            sections.add(RegionKey.fromChunk(world, holder.getPos().x, holder.getPos().z));
        }
        for (net.minecraft.world.entity.Entity entity : level.getEntities().getAll()) {
            ChunkPos position = entity.chunkPosition();
            sections.add(RegionKey.fromChunk(world, position.x, position.z));
        }
        return RegionTopology.create(sections, this.chunkShift);
    }

    private static void awaitWorldPhase(net.minecraft.server.MinecraftServer server, CompletableFuture<Void> completion) {
        ThreadedServer.awaitRegionPhase(server, completion);
    }

    private CompletableFuture<Void> dispatchOwned(RegionTopology frame, RegionKey key, Runnable work) {
        RegionTopology.Owner owner = frame.owner(key);
        CompletableFuture<Void> completion = new CompletableFuture<>();
        if (this.shuttingDown) {
            completion.completeExceptionally(new java.util.concurrent.RejectedExecutionException("Region scheduler is shutting down"));
            return completion;
        }
        try {
            this.workers[frame.workerIndex(owner, this.workers.length)].execute(() -> {
                try {
                    ThreadedServer.enter(frame, owner);
                    try {
                        work.run();
                    } finally {
                        ThreadedServer.exit();
                    }
                    completion.complete(null);
                } catch (Throwable throwable) {
                    completion.completeExceptionally(throwable);
                }
            });
        } catch (java.util.concurrent.RejectedExecutionException ex) {
            completion.completeExceptionally(ex);
        }
        return completion;
    }

    private void schedule(RegionTask task, long delayTicks) {
        synchronized (this.stateLock) {
            if (this.shuttingDown || !task.plugin.isEnabled()) {
                task.cancel();
                return;
            }
            if (!task.isCancelled()) {
                // Even execute() must wait for the next task phase, away from world ticking.
                long deadline = this.tickCount + Math.max(1L, delayTicks);
                this.tasksByDeadline.computeIfAbsent(deadline, key -> new ArrayList<>()).add(task);
                task.scheduledDeadline = deadline;
            }
        }
    }

    private void unschedule(RegionTask task) {
        synchronized (this.stateLock) {
            long deadline = task.scheduledDeadline;
            if (deadline == 0L) {
                return;
            }
            List<RegionTask> scheduled = this.tasksByDeadline.get(deadline);
            if (scheduled != null && scheduled.remove(task) && scheduled.isEmpty()) {
                this.tasksByDeadline.remove(deadline);
            }
            task.scheduledDeadline = 0L;
        }
    }

    private CompletableFuture<Void> dispatch(RegionTask task) {
        if (task.isCancelled()) {
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture<Void> completion = new CompletableFuture<>();
        RegionTopology frame = this.topology;
        RegionTopology.Owner owner = frame.owner(task.regionKey);
        try {
            this.workers[frame.workerIndex(owner, this.workers.length)].execute(() -> {
                try {
                    ThreadedServer.enter(frame, owner);
                    try {
                        task.run();
                    } finally {
                        ThreadedServer.exit();
                    }
                    completion.complete(null);
                } catch (Throwable throwable) {
                    completion.completeExceptionally(throwable);
                }
            });
        } catch (java.util.concurrent.RejectedExecutionException ex) {
            task.cancel();
            completion.completeExceptionally(ex);
            if (!this.shuttingDown) {
                LOGGER.error("Region task for {} was rejected by worker", task.regionKey, ex);
            }
        }
        return completion;
    }

    private void dispatchInternal(RegionTopology.Batch<InternalTask> batch) {
        RegionTopology frame = this.topology;
        RegionTopology.Owner owner = batch.owner();
        try {
            this.workers[frame.workerIndex(owner, this.workers.length)].execute(() -> {
                try {
                    List<Throwable> failures = new ArrayList<>(batch.entries().size());
                    ThreadedServer.enter(frame, owner);
                    try {
                        for (InternalTask task : batch.entries()) {
                            Throwable failure = null;
                            try {
                                task.action().run();
                            } catch (Throwable throwable) {
                                failure = throwable;
                                LOGGER.error("Internal region task for {} generated an exception", task.key(), throwable);
                            }
                            failures.add(failure);
                        }
                    } finally {
                        ThreadedServer.exit();
                    }
                    for (int i = 0; i < batch.entries().size(); i++) {
                        CompletableFuture<Void> future = batch.entries().get(i).completion();
                        if (failures.get(i) == null) {
                            future.complete(null);
                        } else {
                            future.completeExceptionally(failures.get(i));
                        }
                    }
                } catch (Throwable throwable) {
                    batch.entries().forEach(task -> task.completion().completeExceptionally(throwable));
                    LOGGER.error("Internal region batch for {} generated an exception", owner.identity(), throwable);
                }
            });
        } catch (java.util.concurrent.RejectedExecutionException ex) {
            batch.entries().forEach(task -> task.completion().completeExceptionally(ex));
            if (!this.shuttingDown) {
                LOGGER.error("Internal region batch for {} was rejected by worker", owner.identity(), ex);
            }
        }
    }

    private void shutdownWorkers() {
        this.shuttingDown = true;
        synchronized (this.stateLock) {
            this.tasksByDeadline.clear();
        }
        this.tasks.clear();
        for (ExecutorService worker : this.workers) {
            worker.shutdownNow();
        }
        boolean interrupted = false;
        for (ExecutorService worker : this.workers) {
            try {
                if (!worker.awaitTermination(10L, java.util.concurrent.TimeUnit.SECONDS)) {
                    LOGGER.warn("Timed out waiting for TanukiMC region worker shutdown");
                }
            } catch (InterruptedException ex) {
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private static ThreadFactory namedFactory(String prefix) {
        return run -> {
            Thread thread = new Thread(run, prefix + THREAD_ID.incrementAndGet());
            thread.setDaemon(true);
            thread.setUncaughtExceptionHandler((failedThread, throwable) -> LOGGER.error("Uncaught exception in {}", failedThread.getName(), throwable));
            return thread;
        };
    }

    private final class RegionTask implements ScheduledTask, Runnable {
        private static final int IDLE = 0;
        private static final int RUNNING = 1;
        private static final int FINISHED = 2;
        private static final int CANCELLED = 3;
        private static final int CANCELLED_RUNNING = 4;
        private static final VarHandle STATE;

        static {
            try {
                STATE = MethodHandles.lookup().findVarHandle(RegionTask.class, "state", int.class);
            } catch (ReflectiveOperationException ex) {
                throw new ExceptionInInitializerError(ex);
            }
        }

        private final Plugin plugin;
        private final RegionKey regionKey;
        private final Consumer<ScheduledTask> task;
        private final long periodTicks;
        private volatile int state;
        private long scheduledDeadline;

        private RegionTask(Plugin plugin, RegionKey regionKey, Consumer<ScheduledTask> task, long periodTicks) {
            this.plugin = plugin;
            this.regionKey = regionKey;
            this.task = task;
            this.periodTicks = periodTicks;
        }

        @Override
        public void run() {
            if ((int) STATE.compareAndExchange(this, IDLE, RUNNING) != IDLE) {
                return;
            }
            try {
                this.task.accept(this);
            } catch (Throwable throwable) {
                if (ThreadedRegionScheduler.this.shuttingDown || net.minecraft.server.MinecraftServer.getServer().hasStopped()) {
                    return;
                }
                if (this.plugin != null) {
                    this.plugin.getLogger().log(java.util.logging.Level.WARNING, "Region task for " + this.plugin.getDescription().getFullName() + " generated an exception", throwable);
                } else {
                    LOGGER.error("Internal region task for {} generated an exception", this.regionKey, throwable);
                }
            } finally {
                if (this.periodTicks > 0L && (int) STATE.compareAndExchange(this, RUNNING, IDLE) == RUNNING) {
                    ThreadedRegionScheduler.this.schedule(this, this.periodTicks);
                } else if ((int) STATE.compareAndExchange(this, RUNNING, FINISHED) == RUNNING) {
                    ThreadedRegionScheduler.this.tasks.remove(this);
                } else if (this.state == CANCELLED_RUNNING) {
                    this.state = CANCELLED;
                    ThreadedRegionScheduler.this.tasks.remove(this);
                } else if (this.state == FINISHED || this.state == CANCELLED) {
                    ThreadedRegionScheduler.this.tasks.remove(this);
                }
            }
        }

        @Override
        public Plugin getOwningPlugin() {
            return this.plugin;
        }

        @Override
        public boolean isRepeatingTask() {
            return this.periodTicks > 0L;
        }

        @Override
        public CancelledState cancel() {
            for (;;) {
                int current = this.state;
                if (current == IDLE) {
                    if ((int) STATE.compareAndExchange(this, IDLE, CANCELLED) == IDLE) {
                        ThreadedRegionScheduler.this.unschedule(this);
                        ThreadedRegionScheduler.this.tasks.remove(this);
                        return CancelledState.CANCELLED_BY_CALLER;
                    }
                } else if (current == RUNNING) {
                    if (!this.isRepeatingTask()) {
                        return CancelledState.RUNNING;
                    }
                    if ((int) STATE.compareAndExchange(this, RUNNING, CANCELLED_RUNNING) == RUNNING) {
                        return CancelledState.NEXT_RUNS_CANCELLED;
                    }
                } else if (current == CANCELLED_RUNNING) {
                    return CancelledState.NEXT_RUNS_CANCELLED_ALREADY;
                } else if (current == FINISHED) {
                    return CancelledState.ALREADY_EXECUTED;
                } else if (current == CANCELLED) {
                    return CancelledState.CANCELLED_ALREADY;
                }
            }
        }

        @Override
        public ExecutionState getExecutionState() {
            return switch (this.state) {
                case IDLE -> ExecutionState.IDLE;
                case RUNNING -> ExecutionState.RUNNING;
                case FINISHED -> ExecutionState.FINISHED;
                case CANCELLED -> ExecutionState.CANCELLED;
                case CANCELLED_RUNNING -> ExecutionState.CANCELLED_RUNNING;
                default -> throw new IllegalStateException("Unknown state: " + this.state);
            };
        }
    }
}

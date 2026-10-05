package com.mohistmc.youer.threading;

import com.mohistmc.youer.YouerConfig;
import com.mojang.logging.LogUtils;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.slf4j.Logger;

public final class ThreadedServer {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ThreadLocal<RegionContext> CURRENT_REGION = new ThreadLocal<>();
    private static final Object WORLD_MUTATION_LOCK = new Object();
    private static final ConcurrentLinkedQueue<Runnable> MAIN_BRIDGES = new ConcurrentLinkedQueue<>();

    private ThreadedServer() {}

    public static boolean enabled() {
        return YouerConfig.threaded_enabled;
    }

    public static boolean relaxedAsyncCatcher() {
        return enabled() && YouerConfig.threaded_relaxed_async_catcher && isRegionThread();
    }

    public static int workerCount() {
        return Math.max(1, YouerConfig.threaded_worker_count);
    }

    public static int regionChunkShift() {
        return ThreadedRegionScheduler.activeChunkShift(Math.max(0, Math.min(8, YouerConfig.threaded_region_chunk_shift)));
    }

    public static boolean isRegionThread() {
        return CURRENT_REGION.get() != null;
    }

    public static boolean owns(World world, int chunkX, int chunkZ) {
        if (!enabled()) {
            return false;
        }
        RegionContext current = CURRENT_REGION.get();
        return current != null && current.topology.owns(current.owner, world.getUID(), chunkX, chunkZ, 0);
    }

    public static boolean owns(World world, int chunkX, int chunkZ, int squareRadiusChunks) {
        RegionContext current = CURRENT_REGION.get();
        return enabled() && current != null
            && current.topology.owns(current.owner, world.getUID(), chunkX, chunkZ, squareRadiusChunks);
    }

    public static boolean owns(Location location) {
        return location != null && owns(location.getWorld(), location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }

    public static boolean owns(Location location, int squareRadiusChunks) {
        return location != null && owns(location.getWorld(), location.getBlockX() >> 4, location.getBlockZ() >> 4, squareRadiusChunks);
    }

    public static boolean owns(Entity entity) {
        return entity != null && owns(entity.getLocation());
    }

    public static void requireOwnedForWrite(Level level, BlockPos pos) {
        if (!isRegionThread()) {
            return;
        }
        if (level instanceof ServerLevel serverLevel
            && owns(serverLevel.getWorld(), pos.getX() >> 4, pos.getZ() >> 4)) {
            return;
        }
        throw new IllegalStateException("Region worker cannot write block outside its owner: " + pos);
    }

    public static void requireOwnedForWrite(net.minecraft.world.entity.Entity entity) {
        if (!isRegionThread()) {
            return;
        }
        if (entity.level() instanceof ServerLevel serverLevel) {
            net.minecraft.world.level.ChunkPos pos = entity.chunkPosition();
            if (owns(serverLevel.getWorld(), pos.x, pos.z)) {
                return;
            }
        }
        throw new IllegalStateException("Region worker cannot write entity outside its owner: " + entity.getId());
    }

    public static <T> T withWorldMutationLock(Supplier<T> action) {
        if (!isRegionThread()) {
            return action.get();
        }
        synchronized (WORLD_MUTATION_LOCK) {
            return action.get();
        }
    }

    public static <T> T onMainThreadIfRegionThread(Supplier<T> action) {
        if (!isRegionThread()) {
            return action.get();
        }
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null || Thread.currentThread() == server.serverThread) {
            return action.get();
        }
        if (server.hasStopped()) {
            throw new IllegalStateException("Server is stopping");
        }
        CompletableFuture<T> future = new CompletableFuture<>();
        MAIN_BRIDGES.add(() -> {
            if (future.isCancelled()) {
                return;
            }
            try {
                if (server.hasStopped()) {
                    future.completeExceptionally(new IllegalStateException("Server is stopping"));
                    return;
                }
                future.complete(action.get());
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        });
        LockSupport.unpark(server.serverThread);
        for (;;) {
            if (server.hasStopped()) {
                throw new IllegalStateException("Server is stopping");
            }
            try {
                return future.get(50L, TimeUnit.MILLISECONDS);
            } catch (TimeoutException ignored) {
            } catch (InterruptedException ex) {
                future.cancel(false);
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for main thread task", ex);
            } catch (ExecutionException ex) {
                Throwable cause = ex.getCause();
                if (cause instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                if (cause instanceof Error error) {
                    throw error;
                }
                throw new IllegalStateException("Main thread task failed", cause);
            }
        }
    }

    public static void awaitRegionPhase(MinecraftServer server, CompletableFuture<Void> completion) {
        if (Thread.currentThread() != server.serverThread) {
            throw new IllegalStateException("Only the server thread may wait for region workers");
        }
        long started = System.nanoTime();
        long nextWarn = started + TimeUnit.SECONDS.toNanos(5L);
        while (!completion.isDone()) {
            MinecraftServer.throwIfFatalException();
            Runnable bridge;
            while ((bridge = MAIN_BRIDGES.poll()) != null) {
                bridge.run();
            }
            for (ServerLevel level : server.getAllLevels()) {
                level.getChunkSource().pollTask();
            }
            long now = System.nanoTime();
            if (!completion.isDone() && now >= nextWarn) {
                LOGGER.warn("Waited over {} seconds for region workers on tick {}", TimeUnit.NANOSECONDS.toSeconds(now - started), server.getTickCount());
                nextWarn = now + TimeUnit.SECONDS.toNanos(5L);
            }
            if (!completion.isDone()) {
                LockSupport.parkNanos(100_000L);
            }
        }
        Runnable bridge;
        while ((bridge = MAIN_BRIDGES.poll()) != null) {
            bridge.run();
        }
    }

    static void enter(RegionTopology topology, RegionTopology.Owner owner) {
        CURRENT_REGION.set(new RegionContext(topology, owner));
    }

    static void exit() {
        CURRENT_REGION.remove();
    }

    private record RegionContext(RegionTopology topology, RegionTopology.Owner owner) {}
}

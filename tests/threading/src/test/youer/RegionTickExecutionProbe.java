package test.youer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.LevelTicks;
import net.minecraft.world.ticks.ScheduledTick;

public final class RegionTickExecutionProbe {
    public static void main(String[] args) throws Exception {
        verifyExecution();
        verifyPreparedRegions();
        verifyRejectedDispatch();
        verifyMainThreadBridge();
        System.out.println("REGION_TICK_EXECUTION_PROBE PASS parallel ownership order reentry deferral cap prepared-regions rejection barrier recovery main-bridge context-cleanup");
    }

    private static void verifyPreparedRegions() {
        LevelTicks<String> ticks = create(3, 1);
        var prepared = new java.util.concurrent.atomic.AtomicBoolean();
        var snapshot = new java.util.concurrent.atomic.AtomicReference<java.util.Set<Long>>();
        AtomicInteger ran = new AtomicInteger();
        ticks.tickRegions(0, 2, (pos, type) -> ran.incrementAndGet(), chunks -> {
            require(prepared.compareAndSet(false, true), "Region preparation must happen once");
            snapshot.set(chunks);
        }, chunk -> {
            require(prepared.get() && snapshot.get().contains(chunk), "Ownership must be prepared before grouping");
            return chunk;
        }, (region, work) -> {
            require(prepared.get(), "Ownership must be prepared before dispatch");
            work.run();
            return java.util.concurrent.CompletableFuture.completedFuture(null);
        }, java.util.concurrent.CompletableFuture::join);
        require(snapshot.get().equals(java.util.Set.of(ChunkPos.asLong(0, 0), ChunkPos.asLong(1, 0))),
            "Preparation must include exactly the collected chunks, not every loaded container");
        require(ran.get() == 2 && ticks.count() == 1, "Preparation must preserve the global tick cap");
    }

    private static void verifyMainThreadBridge() {
        LevelTicks<String> ticks = create(4, 1);
        Thread coordinator = Thread.currentThread();
        ThreadLocal<Long> ownerContext = new ThreadLocal<>();
        var requests = new java.util.concurrent.LinkedBlockingQueue<Runnable>();
        AtomicInteger bridged = new AtomicInteger();
        AtomicInteger contextsClosed = new AtomicInteger();
        try (var workers = Executors.newFixedThreadPool(2)) {
            ticks.tickRegions(0, 4, (pos, type) -> {
                long expectedOwner = 10L + (pos.getX() >> 4) / 2;
                require(java.util.Objects.equals(ownerContext.get(), expectedOwner), "Dispatcher must bind the selected region context");
                var reply = new java.util.concurrent.CompletableFuture<Boolean>();
                requests.add(() -> {
                    bridged.incrementAndGet();
                    reply.complete(Thread.currentThread() == coordinator);
                });
                try {
                    require(reply.get(5, TimeUnit.SECONDS), "Main-thread bridge must execute on the coordinator");
                } catch (Exception exception) {
                    throw new AssertionError("Region work could not complete its main-thread request", exception);
                }
            }, chunk -> 10L + ChunkPos.getX(chunk) / 2, (region, work) -> {
                var completion = new java.util.concurrent.CompletableFuture<Void>();
                workers.execute(() -> {
                    Throwable failure = null;
                    ownerContext.set(region);
                    try {
                        work.run();
                    } catch (Throwable throwable) {
                        failure = throwable;
                    } finally {
                        ownerContext.remove();
                        contextsClosed.incrementAndGet();
                    }
                    if (failure == null) {
                        completion.complete(null);
                    } else {
                        completion.completeExceptionally(failure);
                    }
                });
                return completion;
            }, completion -> {
                require(Thread.currentThread() == coordinator, "Wait strategy must remain on the coordinating thread");
                while (!completion.isDone()) {
                    try {
                        Runnable request = requests.poll(10, TimeUnit.MILLISECONDS);
                        if (request != null) {
                            request.run();
                        }
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError(exception);
                    }
                }
            });
        }
        require(bridged.get() == 4 && requests.isEmpty(), "Every region callback must complete its main-thread bridge");
        require(contextsClosed.get() == 2 && ticks.count() == 0, "Completion must include ownership-context cleanup");
    }

    private static void verifyExecution() {
        LevelTicks<String> ticks = create(4, 2);
        CountDownLatch bothStarted = new CountDownLatch(2);
        AtomicInteger active = new AtomicInteger();
        AtomicInteger maximum = new AtomicInteger();
        AtomicInteger[] ownerActive = {new AtomicInteger(), new AtomicInteger()};
        List<List<Integer>> order = List.of(new ArrayList<>(), new ArrayList<>());
        try (var workers = Executors.newFixedThreadPool(2)) {
            ticks.tickRegions(0, 8, (pos, type) -> {
                int owner = (pos.getX() >> 4) / 2;
                require(ownerActive[owner].incrementAndGet() == 1, "One region must not run callbacks concurrently");
                maximum.accumulateAndGet(active.incrementAndGet(), Math::max);
                try {
                    List<Integer> observed = order.get(owner);
                    observed.add(pos.getX());
                    if (observed.size() == 1) {
                        bothStarted.countDown();
                        await(bothStarted);
                        try {
                            ticks.tick(0, 1, (nestedPos, nestedType) -> {});
                            throw new AssertionError("Nested tick must be rejected");
                        } catch (IllegalStateException expected) {
                        }
                        ticks.schedule(new ScheduledTick<>(type, new BlockPos(pos.getX(), 90, 1), 0, 100 + owner));
                    }
                } finally {
                    active.decrementAndGet();
                    ownerActive[owner].decrementAndGet();
                }
            }, chunk -> ChunkPos.getX(chunk) / 2, workers);
            require(maximum.get() == 2 && active.get() == 0, "Independent regions must overlap and finish before return");
            require(order.get(0).equals(List.of(1, 2, 17, 18)) && order.get(1).equals(List.of(33, 34, 49, 50)),
                "Region-local collection order must span its chunks");
            require(ticks.count() == 2, "Callback-created ticks must remain for the next collection");
            AtomicInteger next = new AtomicInteger();
            ticks.tick(1, 10, (pos, type) -> next.incrementAndGet());
            require(next.get() == 2, "Deferred callback work must run next tick");
            LevelTicks<String> capped = create(4, 2);
            AtomicInteger ran = new AtomicInteger();
            capped.tickRegions(0, 3, (pos, type) -> ran.incrementAndGet(), chunk -> chunk, workers);
            require(ran.get() == 3 && capped.count() == 5, "Execution cap must be shared across all regions");
        }
    }

    private static void verifyRejectedDispatch() throws Exception {
        LevelTicks<String> ticks = create(2, 1);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger submissions = new AtomicInteger();
        try (var worker = Executors.newSingleThreadExecutor(); var coordinator = Executors.newSingleThreadExecutor()) {
            var phase = coordinator.submit(() -> ticks.tickRegions(0, 2, (pos, type) -> {
                started.countDown();
                await(release);
            }, chunk -> chunk, task -> {
                if (submissions.getAndIncrement() == 0) {
                    worker.execute(task);
                } else {
                    throw new RejectedExecutionException("probe");
                }
            }));
            try {
                await(started);
                try {
                    phase.get(100, TimeUnit.MILLISECONDS);
                    throw new AssertionError("Rejected dispatch must still wait for started work");
                } catch (TimeoutException expected) {
                }
            } finally {
                release.countDown();
            }
            try {
                phase.get(10, TimeUnit.SECONDS);
                throw new AssertionError("Dispatch failure must propagate");
            } catch (ExecutionException expected) {
                require(expected.getCause() instanceof CompletionException
                    && expected.getCause().getCause() instanceof RejectedExecutionException, "Dispatch failure cause must survive the barrier");
            }
            require(ticks.count() == 1, "Unsubmitted work must be restored, not discarded");
            AtomicInteger recovered = new AtomicInteger();
            ticks.tick(1, 10, (pos, type) -> recovered.incrementAndGet());
            require(recovered.get() == 1, "Failed phase must release its guard and retain unexecuted work");
        }
    }

    private static LevelTicks<String> create(int chunks, int perChunk) {
        LevelTicks<String> ticks = new LevelTicks<>(chunk -> true, () -> InactiveProfiler.INSTANCE);
        for (int chunk = 0; chunk < chunks; chunk++) {
            ticks.addContainer(new ChunkPos(chunk, 0), new LevelChunkTicks<>());
            for (int id = 0; id < perChunk; id++) {
                ticks.schedule(new ScheduledTick<>("probe", new BlockPos(chunk * 16 + id + 1, 64, 1), 0, chunk * perChunk + id));
            }
        }
        return ticks;
    }

    private static void await(CountDownLatch latch) {
        try {
            require(latch.await(5, TimeUnit.SECONDS), "Timed out waiting for another region");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError(exception);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}

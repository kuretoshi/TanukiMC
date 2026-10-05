package net.minecraft.world.level.redstone;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;

public final class NeighborUpdaterProbe {
    private static final BlockPos POS = BlockPos.ZERO;
    private static final Method ADD_AND_RUN;

    static {
        try {
            ADD_AND_RUN = CollectingNeighborUpdater.class.getDeclaredMethod(
                "addAndRun", BlockPos.class, CollectingNeighborUpdater.NeighborUpdates.class);
            ADD_AND_RUN.setAccessible(true);
        } catch (ReflectiveOperationException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static void main(String[] args) throws Exception {
        CollectingNeighborUpdater updater = new CollectingNeighborUpdater(null, -1);
        List<String> order = new ArrayList<>();
        submit(updater, ignored -> {
            order.add("outer");
            submit(updater, nested -> {
                order.add("inner");
                return false;
            });
            return false;
        });
        require(order.equals(List.of("outer", "inner")), "Nested updates must retain their order");

        CountDownLatch firstEntered = new CountDownLatch(1);
        CountDownLatch secondRan = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        AtomicInteger completed = new AtomicInteger();
        try (var workers = Executors.newFixedThreadPool(2)) {
            var first = workers.submit(() -> submit(updater, ignored -> {
                firstEntered.countDown();
                try {
                    if (!releaseFirst.await(5, TimeUnit.SECONDS)) {
                        throw new AssertionError("Second updater did not run while the first was active");
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(ex);
                }
                completed.incrementAndGet();
                return false;
            }));
            require(firstEntered.await(5, TimeUnit.SECONDS), "First updater did not enter");
            var second = workers.submit(() -> submit(updater, ignored -> {
                completed.incrementAndGet();
                secondRan.countDown();
                return false;
            }));
            second.get(5, TimeUnit.SECONDS);
            require(secondRan.getCount() == 0L, "Other threads must not append to the active updater stack");
            releaseFirst.countDown();
            first.get(5, TimeUnit.SECONDS);
            require(completed.get() == 2, "Each thread must complete its own update");
        } finally {
            releaseFirst.countDown();
        }
        System.out.println("NEIGHBOR_UPDATER_PROBE PASS nested concurrent");
    }

    private static void submit(CollectingNeighborUpdater updater,
                               CollectingNeighborUpdater.NeighborUpdates update) {
        try {
            ADD_AND_RUN.invoke(updater, POS, update);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}

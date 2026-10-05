package test.youer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.SavedTick;
import net.minecraft.world.ticks.ScheduledTick;
import net.minecraft.world.ticks.TickPriority;

public final class ChunkTickProbe {
    public static void main(String[] args) throws Exception {
        Object type = new Object();
        LevelChunkTicks<Object> queue = new LevelChunkTicks<>();
        ScheduledTick<Object> first = new ScheduledTick<>(type, new BlockPos(1, 64, 1), 10, 0);
        ScheduledTick<Object> second = new ScheduledTick<>(type, new BlockPos(2, 64, 1), 11, 1);
        ScheduledTick<Object> addedWhileSaving = new ScheduledTick<>(type, new BlockPos(3, 64, 1), 12, 2);
        queue.schedule(first);
        var snapshot = queue.getAll();
        queue.schedule(second);
        require(snapshot.toList().equals(List.of(first)), "Queue stream must be a detached snapshot");
        var saved = queue.save(5, value -> {
            require(!Thread.holdsLock(queue), "Save encoder must run outside the queue monitor");
            queue.schedule(addedWhileSaving);
            return "test";
        });
        require(saved.size() == 2 && queue.count() == 3, "Save must use a coherent snapshot when encoder changes the queue");
        LevelChunkTicks<Object> loaded = LevelChunkTicks.load(saved, name -> Optional.of(type), new ChunkPos(0, 0));
        AtomicInteger unpackNotifications = new AtomicInteger();
        loaded.setOnTickAdded((container, tick) -> {
            require(!Thread.holdsLock(container), "Unpack notification must not hold the queue monitor");
            unpackNotifications.incrementAndGet();
        });
        loaded.unpack(100);
        loaded.unpack(100);
        require(loaded.count() == 2 && loaded.peek().triggerTick() == 105 && unpackNotifications.get() == 2,
            "Unpack must preserve delays and publish each loaded tick once");
        queue.removeIf(tick -> {
            require(!Thread.holdsLock(queue), "Removal predicate must run outside the queue monitor");
            return tick == second;
        });
        require(!queue.hasScheduledTick(second.pos(), type) && queue.count() == 2, "Removal must update deduplication state");
        List<SavedTick<Object>> mutable = new ArrayList<>(List.of(new SavedTick<>(type, first.pos(), 5, TickPriority.NORMAL)));
        LevelChunkTicks<Object> isolated = new LevelChunkTicks<>(mutable);
        mutable.clear();
        require(isolated.count() == 1, "Loaded pending list must not alias caller state");
        verifyConcurrentAccess(type);
        System.out.println("CHUNK_TICK_PROBE PASS snapshot save load unpack callbacks remove concurrent");
    }

    private static void verifyConcurrentAccess(Object type) throws Exception {
        LevelChunkTicks<Object> queue = new LevelChunkTicks<>();
        AtomicInteger notifications = new AtomicInteger();
        queue.setOnTickAdded((container, tick) -> {
            require(!Thread.holdsLock(container), "Schedule notification must run outside the queue monitor");
            notifications.incrementAndGet();
        });
        try (var workers = Executors.newFixedThreadPool(4)) {
            List<Future<?>> futures = new ArrayList<>();
            for (int worker = 0; worker < 4; worker++) {
                final int start = worker * 256;
                futures.add(workers.submit(() -> {
                    for (int id = start; id < start + 256; id++) {
                        ScheduledTick<Object> tick = new ScheduledTick<>(type, new BlockPos(id % 16, 64 + id / 16, 1), 0, id);
                        queue.schedule(tick);
                        queue.schedule(tick);
                    }
                }));
            }
            for (Future<?> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
            require(queue.count() == 1024 && notifications.get() == 1024, "Concurrent insertion must preserve deduplication and notifications");
            futures.clear();
            var consumed = ConcurrentHashMap.<Long>newKeySet();
            for (int worker = 0; worker < 4; worker++) {
                futures.add(workers.submit(() -> {
                    ScheduledTick<Object> tick;
                    while ((tick = queue.poll()) != null) {
                        require(consumed.add(tick.subTickOrder()), "Concurrent consumers must not receive the same tick twice");
                        require(!queue.hasScheduledTick(tick.pos(), type), "Consumed tick must leave membership index");
                    }
                }));
            }
            for (Future<?> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
            require(queue.count() == 0 && consumed.size() == 1024, "Concurrent consumers must account for every insertion");
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}

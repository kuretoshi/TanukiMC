package test.youer;

import com.mohistmc.youer.threading.PartitionedEventQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public final class EventQueueProbe {
    private record Event(long chunk, int id) {}

    public static void main(String[] args) throws Exception {
        PartitionedEventQueue<Event> queue = new PartitionedEventQueue<>(Event::chunk);
        Event a = new Event(-1, 1);
        Event b = new Event(2, 2);
        Event c = new Event(-1, 3);
        require(queue.add(a) && queue.add(b) && queue.add(c), "Insertions must succeed");
        require(!queue.add(a) && queue.size() == 3, "Duplicate insertion must not change order or size");
        require(queue.pollFirst(Set.of(2L)).equals(b), "Owned polling must skip foreign chunks");
        require(queue.pollFirst(Set.of(2L)) == null, "Empty owned queue must not consume foreign events");
        require(queue.pollFirst().equals(a) && queue.pollFirst().equals(c), "Global FIFO must survive owned polling");
        require(queue.pollFirst() == null, "Drained queue must be empty");
        queue.addAll(List.of(c, b, a));
        require(queue.pollFirst(Set.of(-1L, 2L)).equals(c), "Multi-chunk polling must preserve insertion order");
        require(queue.removeIf(event -> event.chunk() == -1), "Removal must find indexed event");
        require(queue.pollFirst(Set.of(-1L)) == null && queue.pollFirst().equals(b), "Removal must update both indexes");
        queue.addAll(List.of(a, b));
        require(queue.pollFirst().equals(a), "Processed event must leave deduplication set");
        queue.add(a);
        require(queue.pollFirst().equals(b) && queue.pollFirst().equals(a), "Rescheduled event must move to tail");

        try (var workers = Executors.newFixedThreadPool(4)) {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                final long chunk = i;
                futures.add(workers.submit(() -> {
                    for (int id = 0; id < 1000; id++) {
                        queue.add(new Event(chunk, id));
                    }
                }));
            }
            for (Future<?> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
            require(queue.size() == 4000, "Concurrent producers must not lose events");
            futures.clear();
            for (int i = 0; i < 4; i++) {
                final long chunk = i;
                futures.add(workers.submit(() -> {
                    for (int id = 0; id < 1000; id++) {
                        require(new Event(chunk, id).equals(queue.pollFirst(Set.of(chunk))), "Concurrent owners must retain local FIFO");
                    }
                }));
            }
            for (Future<?> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
        }
        require(queue.size() == 0 && queue.pollFirst() == null, "Concurrent owners must drain every event exactly once");
        System.out.println("EVENT_QUEUE_PROBE PASS dedup fifo partition reschedule removal concurrency");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}

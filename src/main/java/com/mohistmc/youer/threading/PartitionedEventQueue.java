package com.mohistmc.youer.threading;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;

/** Ordered, deduplicated events with immutable values and chunk keys. */
public final class PartitionedEventQueue<T> {
    private record Entry<T>(T value, long chunk, long sequence) {}

    private final ToLongFunction<T> chunkKey;
    private final LinkedHashMap<T, Entry<T>> ordered = new LinkedHashMap<>();
    private final Map<Long, LinkedHashMap<T, Entry<T>>> chunks = new HashMap<>();
    private long sequence;

    public PartitionedEventQueue(ToLongFunction<T> chunkKey) {
        this.chunkKey = java.util.Objects.requireNonNull(chunkKey);
    }

    public synchronized boolean add(T value) {
        java.util.Objects.requireNonNull(value);
        if (this.ordered.containsKey(value)) {
            return false;
        }
        long chunk = this.chunkKey.applyAsLong(value);
        Entry<T> entry = new Entry<>(value, chunk, this.sequence++);
        this.ordered.put(value, entry);
        this.chunks.computeIfAbsent(chunk, ignored -> new LinkedHashMap<>()).put(value, entry);
        return true;
    }

    public synchronized void addAll(Collection<? extends T> values) {
        values.forEach(this::add);
    }

    public synchronized T pollFirst() {
        return this.ordered.isEmpty() ? null : remove(this.ordered.firstEntry().getValue());
    }

    public synchronized T pollFirst(Set<Long> ownedChunks) {
        Entry<T> first = null;
        for (long chunk : ownedChunks) {
            LinkedHashMap<T, Entry<T>> events = this.chunks.get(chunk);
            if (events != null) {
                Entry<T> candidate = events.firstEntry().getValue();
                if (first == null || Long.compareUnsigned(candidate.sequence(), first.sequence()) < 0) {
                    first = candidate;
                }
            }
        }
        return first == null ? null : remove(first);
    }

    public synchronized boolean removeIf(Predicate<? super T> predicate) {
        boolean changed = false;
        var iterator = this.ordered.values().iterator();
        while (iterator.hasNext()) {
            Entry<T> entry = iterator.next();
            if (predicate.test(entry.value())) {
                iterator.remove();
                removeFromChunk(entry);
                changed = true;
            }
        }
        return changed;
    }

    public synchronized int size() {
        return this.ordered.size();
    }

    private T remove(Entry<T> entry) {
        this.ordered.remove(entry.value());
        removeFromChunk(entry);
        return entry.value();
    }

    private void removeFromChunk(Entry<T> entry) {
        LinkedHashMap<T, Entry<T>> events = this.chunks.get(entry.chunk());
        events.remove(entry.value());
        if (events.isEmpty()) {
            this.chunks.remove(entry.chunk());
        }
    }
}

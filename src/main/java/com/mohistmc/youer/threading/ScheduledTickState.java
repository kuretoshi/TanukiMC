package com.mohistmc.youer.threading;

import it.unimi.dsi.fastutil.objects.Object2IntOpenCustomHashMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.ticks.ScheduledTick;

/** One collection phase's execution state; callbacks run outside its monitor. */
public final class ScheduledTickState<T> {
    private static final class ChunkState<T> {
        final LinkedHashMap<Long, ScheduledTick<T>> pending = new LinkedHashMap<>();
        final Map<Long, ScheduledTick<T>> completed = new HashMap<>();
        final Object2IntOpenCustomHashMap<ScheduledTick<?>> membership =
            new Object2IntOpenCustomHashMap<>(ScheduledTick.UNIQUE_TICK_HASH);

        void removeMembership(ScheduledTick<T> tick) {
            if (this.membership.getInt(tick) == 1) {
                this.membership.removeInt(tick);
            } else {
                this.membership.addTo(tick, -1);
            }
        }
    }

    private final LinkedHashMap<Long, ScheduledTick<T>> pending = new LinkedHashMap<>();
    private final Map<Long, ChunkState<T>> chunks = new HashMap<>();
    private long sequence;

    public synchronized void add(ScheduledTick<T> tick) {
        long order = this.sequence++;
        ChunkState<T> chunk = this.chunks.computeIfAbsent(ChunkPos.asLong(tick.pos()), ignored -> new ChunkState<>());
        chunk.pending.put(order, tick);
        chunk.membership.addTo(tick, 1);
        this.pending.put(order, tick);
    }

    public synchronized ScheduledTick<T> poll() {
        return this.pending.isEmpty() ? null : take(this.pending.firstEntry().getKey());
    }

    public synchronized ScheduledTick<T> poll(Set<Long> ownedChunks) {
        Long first = null;
        for (long key : ownedChunks) {
            ChunkState<T> chunk = this.chunks.get(key);
            if (chunk != null && !chunk.pending.isEmpty()) {
                long candidate = chunk.pending.firstEntry().getKey();
                if (first == null || candidate < first) {
                    first = candidate;
                }
            }
        }
        return first == null ? null : take(first);
    }

    private ScheduledTick<T> take(long order) {
        ScheduledTick<T> tick = this.pending.remove(order);
        ChunkState<T> chunk = this.chunks.get(ChunkPos.asLong(tick.pos()));
        chunk.pending.remove(order);
        chunk.removeMembership(tick);
        chunk.completed.put(order, tick);
        return tick;
    }

    public synchronized int size() {
        return this.pending.size();
    }

    public synchronized Set<Long> pendingChunks() {
        Set<Long> result = new java.util.HashSet<>();
        this.chunks.forEach((key, chunk) -> {
            if (!chunk.pending.isEmpty()) {
                result.add(key);
            }
        });
        return Set.copyOf(result);
    }

    public synchronized List<ScheduledTick<T>> pendingSnapshot() {
        return List.copyOf(this.pending.values());
    }

    public synchronized boolean contains(BlockPos pos, T type) {
        ChunkState<T> chunk = this.chunks.get(ChunkPos.asLong(pos));
        return chunk != null && chunk.membership.containsKey(ScheduledTick.probe(type, pos));
    }

    public synchronized void removeIf(Predicate<ScheduledTick<T>> predicate) {
        var chunksIterator = this.chunks.values().iterator();
        while (chunksIterator.hasNext()) {
            ChunkState<T> chunk = chunksIterator.next();
            var iterator = chunk.pending.entrySet().iterator();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                if (predicate.test(entry.getValue())) {
                    this.pending.remove(entry.getKey());
                    chunk.removeMembership(entry.getValue());
                    iterator.remove();
                }
            }
            chunk.completed.values().removeIf(predicate);
            if (chunk.pending.isEmpty() && chunk.completed.isEmpty()) {
                chunksIterator.remove();
            }
        }
    }

    public synchronized List<ScheduledTick<T>> snapshot(Predicate<ScheduledTick<T>> predicate) {
        // Vanilla copies completed ticks before pending ticks, in collection order.
        Map<Long, ScheduledTick<T>> completed = new TreeMap<>();
        this.chunks.values().forEach(chunk -> completed.putAll(chunk.completed));
        List<ScheduledTick<T>> result = new ArrayList<>();
        completed.values().stream().filter(predicate).forEach(result::add);
        this.pending.values().stream().filter(predicate).forEach(result::add);
        return List.copyOf(result);
    }

    public synchronized void clear() {
        this.pending.clear();
        this.chunks.clear();
        this.sequence = 0;
    }
}

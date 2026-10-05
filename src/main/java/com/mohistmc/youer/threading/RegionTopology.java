package com.mohistmc.youer.threading;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Immutable ownership snapshot for a single tick phase. */
public final class RegionTopology {
    private final Map<RegionKey, Owner> owners;
    private final Map<RegionKey, Integer> workerSlots;
    private final int chunkShift;

    private RegionTopology(Map<RegionKey, Owner> owners, Map<RegionKey, Integer> workerSlots, int chunkShift) {
        this.owners = Map.copyOf(owners);
        this.workerSlots = Map.copyOf(workerSlots);
        this.chunkShift = chunkShift;
    }

    public static RegionTopology create(Collection<RegionKey> activeSections, int chunkShift) {
        if (chunkShift < 0 || chunkShift > 8) {
            throw new IllegalArgumentException("Invalid region chunk shift: " + chunkShift);
        }
        Set<RegionKey> remaining = new HashSet<>();
        // Reserve a halo and merge touching halos before any worker starts.
        for (RegionKey section : activeSections) {
            visitNeighbours(section, remaining::add);
        }
        Map<RegionKey, Owner> owners = new HashMap<>();
        Map<RegionKey, Integer> workerSlots = new HashMap<>();
        ArrayDeque<RegionKey> pending = new ArrayDeque<>();
        for (RegionKey first : new java.util.ArrayList<>(remaining)) {
            if (!remaining.remove(first)) {
                continue;
            }
            pending.add(first);
            Set<RegionKey> component = new HashSet<>();
            RegionKey identity = first;
            while (!pending.isEmpty()) {
                RegionKey current = pending.removeFirst();
                component.add(current);
                if (current.regionX() < identity.regionX()
                    || current.regionX() == identity.regionX() && current.regionZ() < identity.regionZ()) {
                    identity = current;
                }
                visitNeighbours(current, neighbour -> {
                    if (remaining.remove(neighbour)) {
                        pending.addLast(neighbour);
                    }
                });
            }
            Owner owner = new Owner(identity, Set.copyOf(component));
            workerSlots.put(identity, workerSlots.size());
            for (RegionKey section : component) {
                owners.put(section, owner);
            }
        }
        return new RegionTopology(owners, workerSlots, chunkShift);
    }

    private static void visitNeighbours(RegionKey section, java.util.function.Consumer<RegionKey> visitor) {
        for (long x = (long) section.regionX() - 1; x <= (long) section.regionX() + 1; x++) {
            for (long z = (long) section.regionZ() - 1; z <= (long) section.regionZ() + 1; z++) {
                if (x >= Integer.MIN_VALUE && x <= Integer.MAX_VALUE && z >= Integer.MIN_VALUE && z <= Integer.MAX_VALUE) {
                    visitor.accept(new RegionKey(section.worldId(), (int) x, (int) z));
                }
            }
        }
    }

    public Owner owner(RegionKey section) {
        Owner owner = this.owners.get(section);
        if (owner == null) {
            throw new IllegalStateException("Section was not reserved for this tick: " + section);
        }
        return owner;
    }

    public int workerIndex(Owner owner, int workerCount) {
        if (workerCount <= 0 || this.owner(owner.identity()) != owner) {
            throw new IllegalArgumentException("Owner and worker count must belong to this topology");
        }
        return this.workerSlots.get(owner.identity()) % workerCount;
    }

    public <T> java.util.List<Batch<T>> partition(Collection<T> entries, java.util.function.Function<T, RegionKey> position) {
        Map<RegionKey, java.util.List<T>> grouped = new java.util.LinkedHashMap<>();
        for (T entry : entries) {
            grouped.computeIfAbsent(owner(position.apply(entry)).identity(), ignored -> new java.util.ArrayList<>()).add(entry);
        }
        java.util.List<Batch<T>> batches = new java.util.ArrayList<>(grouped.size());
        grouped.forEach((identity, items) -> batches.add(new Batch<>(owner(identity), items)));
        return java.util.List.copyOf(batches);
    }

    public record Batch<T>(Owner owner, java.util.List<T> entries) {
        public Batch {
            entries = java.util.List.copyOf(entries);
        }
    }

    public boolean owns(Owner owner, UUID worldId, int chunkX, int chunkZ, int radius) {
        if (!owner.identity.worldId().equals(worldId)) {
            return false;
        }
        long distance = Math.max(0, radius);
        long minX = ((long) chunkX - distance) >> this.chunkShift;
        long maxX = ((long) chunkX + distance) >> this.chunkShift;
        long minZ = ((long) chunkZ - distance) >> this.chunkShift;
        long maxZ = ((long) chunkZ + distance) >> this.chunkShift;
        long width = maxX - minX + 1;
        long height = maxZ - minZ + 1;
        int available = owner.sections.size();
        if (width > available || height > available || width * height > available
            || minX < Integer.MIN_VALUE || maxX > Integer.MAX_VALUE
            || minZ < Integer.MIN_VALUE || maxZ > Integer.MAX_VALUE) {
            return false;
        }
        for (long x = minX; x <= maxX; x++) {
            for (long z = minZ; z <= maxZ; z++) {
                if (this.owners.get(new RegionKey(worldId, (int) x, (int) z)) != owner) {
                    return false;
                }
            }
        }
        return true;
    }

    public record Owner(RegionKey identity, Set<RegionKey> sections) {}
}

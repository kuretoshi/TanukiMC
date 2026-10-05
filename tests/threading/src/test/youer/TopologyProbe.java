package test.youer;

import com.mohistmc.youer.threading.RegionKey;
import com.mohistmc.youer.threading.RegionTopology;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class TopologyProbe {
    private static final UUID WORLD = new UUID(0L, 1L);

    public static void main(String[] args) throws Exception {
        EventQueueProbe.main(args);
        ScheduledTickProbe.main(args);
        ChunkTickProbe.main(args);
        RegionTickExecutionProbe.main(args);
        WorldRandomProbe.main(args);
        net.minecraft.world.level.redstone.NeighborUpdaterProbe.main(args);
        RegionKey left = key(0, 0);
        RegionKey right = key(6, 0);
        RegionTopology split = RegionTopology.create(List.of(left, right), 4);
        require(split.owner(left) != split.owner(right), "Distant sections must have separate owners");
        List<RegionKey> spread = new ArrayList<>();
        for (int x = 0; x < 8; x++) {
            spread.add(key(x * 6, 0));
        }
        RegionTopology distributed = RegionTopology.create(spread, 4);
        java.util.Set<Integer> workerSlots = new java.util.HashSet<>();
        for (RegionKey section : spread) {
            workerSlots.add(distributed.workerIndex(distributed.owner(section), 8));
        }
        require(workerSlots.size() == 8, "Independent owners must use separate available workers");
        require(distributed.workerIndex(distributed.owner(spread.getFirst()), 1) == 0,
            "One worker must still own every region");
        require(distributed.owner(key(1, 0)) == distributed.owner(spread.getFirst()),
            "A halo section must share its owner's worker");
        require(distributed.workerIndex(distributed.owner(spread.getFirst()), 8)
            == distributed.workerIndex(distributed.owner(key(1, 0)), 8),
            "An owner must retain its worker within a snapshot");
        RegionTopology merged = RegionTopology.create(List.of(left, key(3, 0), right), 4);
        require(merged.owner(left) == merged.owner(right), "A loaded bridge must merge ownership");
        require(split.owner(left) != split.owner(right), "Existing tick snapshots must stay immutable");
        RegionTopology splitAgain = RegionTopology.create(List.of(left, right), 4);
        require(splitAgain.owner(left) != splitAgain.owner(right), "Removing a bridge must split the next snapshot");
        List<RegionKey> work = new ArrayList<>(List.of(left, right, left));
        var batches = split.partition(work, section -> section);
        require(batches.size() == 2, "Split owners need independent work lists");
        require(batches.get(0).entries().equals(List.of(left, left)), "Owner-local order and repeated entries must survive");
        require(batches.get(1).entries().equals(List.of(right)), "Every entry must be assigned exactly once");
        require(merged.partition(work, section -> section).size() == 1, "Merged ownership must merge work lists");
        work.clear();
        require(batches.get(0).entries().size() == 2, "Clearing input must not mutate dispatched work");
        try {
            batches.get(0).entries().clear();
            throw new AssertionError("Batch entries must be immutable");
        } catch (UnsupportedOperationException expected) {
        }
        try {
            split.partition(List.of(key(100, 100)), section -> section);
            throw new AssertionError("Unreserved work must not be silently dropped");
        } catch (IllegalStateException expected) {
        }

        RegionTopology negative = RegionTopology.create(List.of(key(-1, -1), left), 4);
        require(negative.owner(left) == negative.owner(key(-1, -1)), "Diagonal and negative neighbours must merge");
        require(negative.owns(negative.owner(left), WORLD, 0, 0, 1), "Ownership must span a section boundary");
        require(!negative.owns(negative.owner(left), new UUID(0L, 2L), 0, 0, 0), "Worlds must stay independent");
        require(!negative.owns(negative.owner(left), WORLD, 0, 0, Integer.MAX_VALUE), "Oversized radii must fail promptly");

        List<RegionKey> ring = new ArrayList<>();
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                if (Math.abs(x) == 3 || Math.abs(z) == 3) {
                    ring.add(key(x, z));
                }
            }
        }
        RegionTopology hole = RegionTopology.create(ring, 0);
        RegionTopology.Owner ringOwner = hole.owner(key(-3, -3));
        require(hole.owns(ringOwner, WORLD, -3, -3, 0) && hole.owns(ringOwner, WORLD, 3, 3, 0), "Ring corners must be owned");
        require(!hole.owns(ringOwner, WORLD, 0, 0, 3), "Checking only corners must not hide an unowned interior");

        RegionKey edge = key(Integer.MAX_VALUE, Integer.MIN_VALUE);
        RegionTopology bounded = RegionTopology.create(List.of(edge), 0);
        require(bounded.owns(bounded.owner(edge), WORLD, Integer.MAX_VALUE, Integer.MIN_VALUE, 0), "Edge coordinates must not wrap");
        require(!bounded.owns(bounded.owner(edge), WORLD, Integer.MAX_VALUE, Integer.MIN_VALUE, 1), "Overflow must not claim ownership");
        RegionTaskQueueProbe.main(args);
        System.out.println("TOPOLOGY_PROBE PASS merge split immutable boundaries worlds holes overflow partition worker-distribution");
    }

    private static RegionKey key(int x, int z) {
        return new RegionKey(WORLD, x, z);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}

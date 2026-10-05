package test.youer;

import com.mohistmc.youer.threading.ScheduledTickState;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.LevelTicks;
import net.minecraft.world.ticks.ScheduledTick;
import net.minecraft.world.ticks.TickPriority;

public final class ScheduledTickProbe {
    public static void main(String[] args) throws Exception {
        LevelTicks<String> ticks = create();
        BlockPos a = new BlockPos(1, 64, 1);
        BlockPos b = new BlockPos(17, 64, 1);
        String first = new String("same");
        String second = new String("same");
        ticks.schedule(new ScheduledTick<>(first, a, 0, 1));
        ticks.schedule(new ScheduledTick<>(first, a, 0, 2));
        ticks.schedule(new ScheduledTick<>(second, a, 0, 3));
        require(ticks.count() == 2, "Deduplication must use type identity, not equals");
        List<String> types = new ArrayList<>();
        ticks.tick(0, 10, (pos, type) -> types.add(type));
        require(types.size() == 2 && types.get(0) == first && types.get(1) == second, "Distinct equal types must both run");

        ticks.schedule(new ScheduledTick<>(first, a, 1, TickPriority.NORMAL, 4));
        ticks.schedule(new ScheduledTick<>(first, b, 1, TickPriority.HIGH, 5));
        List<BlockPos> positions = new ArrayList<>();
        ticks.tick(1, 1, (pos, type) -> positions.add(pos));
        require(positions.equals(List.of(b)) && ticks.count() == 1, "Priority and execution cap must span containers");
        ticks.tick(2, 10, (pos, type) -> {
            positions.add(pos);
            ticks.schedule(new ScheduledTick<>(first, b, 2, 6));
        });
        require(positions.equals(List.of(b, a)) && ticks.count() == 1, "Ticks scheduled during execution must wait");
        ticks.tick(3, 10, (pos, type) -> positions.add(pos));
        require(positions.equals(List.of(b, a, b)), "Deferred tick must run on the next collection");

        ticks.schedule(new ScheduledTick<>(first, a, 4, 7));
        ticks.schedule(new ScheduledTick<>(first, b, 4, 8));
        positions.clear();
        ticks.tick(4, 10, (pos, type) -> {
            positions.add(pos);
            require(ticks.willTickThisTick(b, first), "Second tick must initially be pending");
            ticks.clearArea(new BoundingBox(17, 64, 1, 17, 64, 1));
            require(!ticks.willTickThisTick(b, first), "Cleared pending tick must disappear from membership cache");
        });
        require(positions.equals(List.of(a)), "Cleared pending tick must not execute");
        verifyPartitionedState(first, second, a, b);
        verifyCopy(first, a, b);
        verifyConcurrentOwners();
        verifyContainerLifecycle();
        verifyConcurrentRegistration();
        System.out.println("SCHEDULED_TICK_PROBE PASS identity priority cap deferral clear partition copy concurrent lifecycle registry");
    }

    private static void verifyPartitionedState(String first, String second, BlockPos a, BlockPos b) {
        ScheduledTickState<String> state = new ScheduledTickState<>();
        ScheduledTick<String> left = new ScheduledTick<>(first, a, 0, 0);
        ScheduledTick<String> right = new ScheduledTick<>(second, b, 0, 1);
        state.add(left);
        state.add(right);
        state.add(left);
        require(state.size() == 3, "Execution state must not deduplicate collected entries");
        require(state.poll(Set.of(ChunkPos.asLong(b))) == right, "Owned polling must not consume foreign chunks");
        require(state.poll(Set.of(ChunkPos.asLong(b))) == null, "Completed chunks must not expose foreign work");
        require(!state.contains(b, second) && state.contains(a, first), "Pending membership must follow selected polling");
        require(state.poll() == left && state.contains(a, first), "Duplicate identity must remain pending until its last occurrence");
        require(state.snapshot(tick -> true).equals(List.of(left, right, left)), "Copy snapshot must include completed and pending work");
        state.removeIf(tick -> tick.pos().equals(a));
        require(!state.contains(a, first) && state.size() == 0, "Removal must clear every index and duplicate");
        require(state.snapshot(tick -> true).equals(List.of(right)), "Removal must also affect completed history");
        state.clear();
        require(state.snapshot(tick -> true).isEmpty(), "Phase cleanup must clear history");
    }

    private static void verifyCopy(String type, BlockPos a, BlockPos b) {
        LevelTicks<String> source = create();
        LevelTicks<String> destination = create();
        BlockPos future = new BlockPos(18, 64, 1);
        source.schedule(new ScheduledTick<>(type, a, 0, 0));
        source.schedule(new ScheduledTick<>(type, b, 0, 1));
        source.schedule(new ScheduledTick<>(type, future, 10, 2));
        source.tick(0, 10, (pos, value) -> {
            if (pos.equals(a)) {
                destination.copyAreaFrom(source, new BoundingBox(0, 0, 0, 31, 100, 15), net.minecraft.core.Vec3i.ZERO);
                require(destination.count() == 3, "Copy must include completed, pending, and future ticks");
                require(source.willTickThisTick(b, type), "Copy must not consume source work");
            }
        });
        List<BlockPos> copied = new ArrayList<>();
        destination.tick(10, 10, (pos, value) -> copied.add(pos));
        require(copied.equals(List.of(a, b, future)), "Copied ticks must preserve their relative sub-tick order");
    }

    private static LevelTicks<String> create() {
        LevelTicks<String> ticks = new LevelTicks<>(chunk -> true, () -> InactiveProfiler.INSTANCE);
        ticks.addContainer(new ChunkPos(0, 0), new LevelChunkTicks<>());
        ticks.addContainer(new ChunkPos(1, 0), new LevelChunkTicks<>());
        return ticks;
    }

    private static final class CapturingContainer extends LevelChunkTicks<String> {
        java.util.function.BiConsumer<LevelChunkTicks<String>, ScheduledTick<String>> captured;
        ScheduledTick<String> duringAttach;

        @Override
        public void setOnTickAdded(java.util.function.BiConsumer<LevelChunkTicks<String>, ScheduledTick<String>> callback) {
            if (callback != null) {
                this.captured = callback;
                if (this.duringAttach != null) {
                    schedule(this.duringAttach);
                    this.duringAttach = null;
                }
            }
            super.setOnTickAdded(callback);
        }
    }

    private static void verifyContainerLifecycle() {
        String type = "lifecycle";
        BlockPos pos = new BlockPos(1, 64, 1);
        ChunkPos chunk = new ChunkPos(0, 0);
        for (boolean removeFirst : new boolean[] {true, false}) {
            LevelTicks<String> ticks = create();
            CapturingContainer old = new CapturingContainer();
            ScheduledTick<String> stale = new ScheduledTick<>(type, pos, 100, 0);
            old.schedule(stale);
            ticks.addContainer(chunk, old);
            if (removeFirst) {
                ticks.removeContainer(chunk);
            }
            LevelChunkTicks<String> replacement = new LevelChunkTicks<>();
            replacement.schedule(new ScheduledTick<>(type, pos, 0, 1));
            ticks.addContainer(chunk, replacement);
            old.captured.accept(old, stale);
            List<BlockPos> ran = new ArrayList<>();
            ticks.tick(0, 10, (position, value) -> ran.add(position));
            require(ran.equals(List.of(pos)), "Detached container notification must not postpone replacement ticks");
        }
        LevelTicks<String> ticks = create();
        CapturingContainer attaching = new CapturingContainer();
        attaching.duringAttach = new ScheduledTick<>(type, pos, 0, 0);
        ticks.addContainer(chunk, attaching);
        List<BlockPos> ran = new ArrayList<>();
        ticks.tick(0, 10, (position, value) -> ran.add(position));
        require(ran.equals(List.of(pos)), "Tick added during attachment must not be lost");
    }

    private static void verifyConcurrentOwners() throws Exception {
        ScheduledTickState<Object> state = new ScheduledTickState<>();
        Object type = new Object();
        for (int chunk = 0; chunk < 4; chunk++) {
            for (int id = 0; id < 256; id++) {
                state.add(new ScheduledTick<>(type, new BlockPos(chunk * 16 + id % 16, 64 + id / 16, 1), 0, id));
            }
        }
        try (var workers = java.util.concurrent.Executors.newFixedThreadPool(4)) {
            List<java.util.concurrent.Future<?>> futures = new ArrayList<>();
            for (int chunk = 0; chunk < 4; chunk++) {
                final int owner = chunk;
                futures.add(workers.submit(() -> {
                    Set<Long> owned = Set.of(ChunkPos.asLong(owner, 0));
                    for (int id = 0; id < 256; id++) {
                        ScheduledTick<Object> tick = state.poll(owned);
                        require(tick != null && tick.pos().getX() >> 4 == owner, "Owner must only receive its chunk");
                        require(tick.subTickOrder() == id && !state.contains(tick.pos(), type), "Owner-local ordering and membership must stay consistent");
                    }
                    require(state.poll(owned) == null, "An owner must not consume another owner's remaining ticks");
                }));
            }
            for (var future : futures) {
                future.get(10, java.util.concurrent.TimeUnit.SECONDS);
            }
        }
        require(state.size() == 0 && state.snapshot(tick -> true).size() == 1024, "Concurrent ownership must account for every tick once");
    }

    private static void verifyConcurrentRegistration() throws Exception {
        LevelTicks<String> ticks = new LevelTicks<>(chunk -> true, () -> InactiveProfiler.INSTANCE);
        String type = "registry";
        try (var workers = java.util.concurrent.Executors.newFixedThreadPool(4)) {
            List<java.util.concurrent.Future<?>> futures = new ArrayList<>();
            for (int worker = 0; worker < 4; worker++) {
                final int chunk = worker;
                futures.add(workers.submit(() -> {
                    ticks.addContainer(new ChunkPos(chunk, 0), new LevelChunkTicks<>());
                    for (int id = 0; id < 256; id++) {
                        ticks.schedule(new ScheduledTick<>(type, new BlockPos(chunk * 16 + id % 16, 64 + id / 16, 1), 0, chunk * 256L + id));
                        ticks.count();
                    }
                }));
            }
            for (var future : futures) {
                future.get(10, java.util.concurrent.TimeUnit.SECONDS);
            }
            require(ticks.count() == 1024, "Concurrent registration and scheduling must preserve every tick");
            java.util.Set<BlockPos> ran = new java.util.HashSet<>();
            ticks.tick(0, 1024, (pos, value) -> {
                require(ran.add(pos), "Registered tick must execute once");
                if (ran.size() == 1) {
                    try {
                        workers.submit(ticks::count).get(5, java.util.concurrent.TimeUnit.SECONDS);
                    } catch (Exception exception) {
                        throw new AssertionError("World tick callback must run outside the registry monitor", exception);
                    }
                }
            });
            require(ran.size() == 1024 && ticks.count() == 0, "Registry must drain all collected ticks");
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}

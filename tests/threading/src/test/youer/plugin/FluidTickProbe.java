package test.youer.plugin;

import com.mohistmc.youer.threading.ThreadedServer;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.plugin.java.JavaPlugin;

final class FluidTickProbe implements Listener {
    private static final Set<Integer> CHUNKS = Set.of(15, 128, 256, 384);
    private final World world;
    private final int sourceY;
    private final int reclaimed;
    private final Set<Integer> observed = ConcurrentHashMap.newKeySet();
    private final Set<Integer> formed = ConcurrentHashMap.newKeySet();
    private final Set<Integer> lavaFlows = ConcurrentHashMap.newKeySet();
    private final AtomicInteger active = new AtomicInteger();
    private final AtomicInteger maximum = new AtomicInteger();
    private final AtomicInteger lavaActive = new AtomicInteger();
    private final AtomicInteger lavaMaximum = new AtomicInteger();
    private final AtomicInteger failures = new AtomicInteger();

    FluidTickProbe(JavaPlugin plugin, World world) {
        this.world = world;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        this.reclaimed = clearPreviousProbes(world);
        this.sourceY = findUnusedHeight(world);
        var level = ((CraftWorld) world).getHandle();
        for (int chunkX : CHUNKS) {
            world.addPluginChunkTicket(chunkX, 0, plugin);
            BlockPos pos = new BlockPos(chunkX * 16 + 8, this.sourceY, 12);
            level.setBlock(pos.below(2), Blocks.STONE.defaultBlockState(), 3);
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                level.setBlock(pos.relative(direction), Blocks.STONE.defaultBlockState(), 3);
                level.setBlock(pos.below().relative(direction), Blocks.STONE.defaultBlockState(), 3);
            }
            level.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
            level.scheduleTick(pos, Fluids.WATER, 5);

            BlockPos lava = new BlockPos(chunkX * 16 + 8, this.sourceY, 20);
            level.setBlock(lava.below(2), Blocks.STONE.defaultBlockState(), 3);
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                level.setBlock(lava.relative(direction), Blocks.STONE.defaultBlockState(), 3);
                level.setBlock(lava.below().relative(direction), Blocks.STONE.defaultBlockState(), 3);
            }
            level.scheduleTick(lava, Fluids.LAVA, 5);
            level.setBlock(lava, Blocks.LAVA.defaultBlockState(), 3);
            level.setBlock(lava.below(), Blocks.WATER.defaultBlockState(), 3);
        }
    }

    private static int findUnusedHeight(World world) {
        var fluidTicks = ((CraftWorld) world).getHandle().getFluidTicks();
        for (int y = 80; y < 300; y += 4) {
            boolean unused = true;
            for (int chunkX : CHUNKS) {
                int x = chunkX * 16 + 8;
                BlockPos water = new BlockPos(x, y, 12);
                BlockPos lava = new BlockPos(x, y, 20);
                if (world.getBlockAt(x, y, 12).getType() != Material.AIR
                    || world.getBlockAt(x, y - 1, 12).getType() != Material.AIR
                    || world.getBlockAt(x, y, 20).getType() != Material.AIR
                    || world.getBlockAt(x, y - 1, 20).getType() != Material.AIR
                    || fluidTicks.hasScheduledTick(water, Fluids.WATER)
                    || fluidTicks.hasScheduledTick(lava, Fluids.LAVA)) {
                    unused = false;
                    break;
                }
            }
            if (unused) {
                return y;
            }
        }
        throw new IllegalStateException("No unused fluid probe height remains");
    }

    private static int clearPreviousProbes(World world) {
        int cleaned = 0;
        for (int y = 80; y < 300; y += 4) {
            for (int chunkX : CHUNKS) {
                int x = chunkX * 16 + 8;
                Material belowLava = world.getBlockAt(x, y - 1, 20).getType();
                if (world.getBlockAt(x, y, 12).getType() == Material.WATER
                    && world.getBlockAt(x, y, 20).getType() == Material.LAVA
                    && (belowLava == Material.STONE || belowLava == Material.WATER)
                    && world.getBlockAt(x + 1, y, 12).getType() == Material.STONE
                    && world.getBlockAt(x + 1, y, 20).getType() == Material.STONE) {
                    clearProbe(world, x, y);
                    cleaned++;
                }
            }
        }
        return cleaned;
    }

    private static void clearProbe(World world, int x, int y) {
        var level = ((CraftWorld) world).getHandle();
        for (int z : new int[] {12, 20}) {
            BlockPos source = new BlockPos(x, y, z);
            for (int depth = 0; depth <= 2; depth++) {
                level.setBlock(source.below(depth), Blocks.AIR.defaultBlockState(), 18);
            }
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                level.setBlock(source.relative(direction), Blocks.AIR.defaultBlockState(), 18);
                level.setBlock(source.below().relative(direction), Blocks.AIR.defaultBlockState(), 18);
            }
        }
    }

    int cleanup() {
        for (int chunkX : CHUNKS) {
            int x = chunkX * 16 + 8;
            clearProbe(this.world, x, this.sourceY);
            if (this.world.getBlockAt(x, this.sourceY, 12).getType() != Material.AIR
                || this.world.getBlockAt(x, this.sourceY - 1, 12).getType() != Material.AIR
                || this.world.getBlockAt(x, this.sourceY, 20).getType() != Material.AIR
                || this.world.getBlockAt(x, this.sourceY - 1, 20).getType() != Material.AIR) {
                return 0;
            }
        }
        return CHUNKS.size();
    }

    int reclaimed() {
        return this.reclaimed;
    }

    int sourceY() {
        return this.sourceY;
    }

    String queuedLava() {
        var level = ((CraftWorld) this.world).getHandle();
        var ticks = level.getFluidTicks();
        StringBuilder result = new StringBuilder();
        for (int chunkX : CHUNKS) {
            if (!result.isEmpty()) {
                result.append(',');
            }
            BlockPos lava = new BlockPos(chunkX * 16 + 8, this.sourceY, 20);
            var chunkTicks = (net.minecraft.world.ticks.LevelChunkTicks<net.minecraft.world.level.material.Fluid>)
                level.getChunk(chunkX, 1).getFluidTicks();
            String trigger = chunkTicks.getAll()
                .filter(tick -> tick.pos().equals(lava) && tick.type() == Fluids.LAVA)
                .map(tick -> Long.toString(tick.triggerTick())).findFirst().orElse("pending");
            result.append(chunkX).append(':').append(ticks.hasScheduledTick(lava, Fluids.LAVA))
                .append('/').append(trigger).append('/').append(chunkTicks.count());
        }
        return level.getGameTime() + "[" + result + "]";
    }

    @EventHandler
    public void onForm(BlockFormEvent event) {
        var block = event.getBlock();
        int chunkX = block.getX() >> 4;
        if (block.getWorld() != this.world || !CHUNKS.contains(chunkX)
            || block.getX() != chunkX * 16 + 8 || block.getY() != this.sourceY - 1 || block.getZ() != 20) {
            return;
        }
        int running = this.lavaActive.incrementAndGet();
        this.lavaMaximum.accumulateAndGet(running, Math::max);
        try {
            if (!ThreadedServer.isRegionThread() || !ThreadedServer.owns(this.world, chunkX, 0)
                || event.getNewState().getType() != Material.STONE) {
                this.failures.incrementAndGet();
            }
            this.formed.add(chunkX);
            try {
                Thread.sleep(50L);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                this.failures.incrementAndGet();
            }
        } finally {
            this.lavaActive.decrementAndGet();
        }
    }

    @EventHandler
    public void onFlow(BlockFromToEvent event) {
        var block = event.getBlock();
        int chunkX = block.getX() >> 4;
        if (block.getWorld() == this.world && CHUNKS.contains(chunkX)
            && block.getX() == chunkX * 16 + 8 && block.getY() == this.sourceY && block.getZ() == 20
            && block.getType() == Material.LAVA) {
            if (!ThreadedServer.isRegionThread() || !ThreadedServer.owns(this.world, chunkX, 0)) {
                this.failures.incrementAndGet();
            }
            this.lavaFlows.add(chunkX);
        }
        if (block.getWorld() != this.world || !CHUNKS.contains(chunkX)
            || block.getX() != chunkX * 16 + 8 || block.getY() != this.sourceY || block.getZ() != 12
            || block.getType() != Material.WATER) {
            return;
        }
        int running = this.active.incrementAndGet();
        this.maximum.accumulateAndGet(running, Math::max);
        try {
            if (!ThreadedServer.isRegionThread() || !ThreadedServer.owns(this.world, chunkX, 0)) {
                this.failures.incrementAndGet();
            }
            this.observed.add(chunkX);
            try {
                Thread.sleep(50L);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                this.failures.incrementAndGet();
            }
        } finally {
            this.active.decrementAndGet();
        }
    }

    boolean passed() {
        if (this.failures.get() != 0 || !this.observed.equals(CHUNKS) || this.maximum.get() <= 1
            || !this.formed.equals(CHUNKS) || !this.lavaFlows.equals(CHUNKS) || this.lavaMaximum.get() <= 1) {
            return false;
        }
        for (int chunkX : CHUNKS) {
            if (this.world.getBlockAt(chunkX * 16 + 8, this.sourceY - 1, 12).getType() != Material.WATER) {
                return false;
            }
            if (this.world.getBlockAt(chunkX * 16 + 8, this.sourceY - 1, 20).getType() != Material.STONE) {
                return false;
            }
        }
        return true;
    }

    int count() {
        return this.observed.size();
    }

    int maximum() {
        return this.maximum.get();
    }

    int formed() {
        return this.formed.size();
    }

    int lavaFlows() {
        return this.lavaFlows.size();
    }

    String lavaStates() {
        var level = ((CraftWorld) this.world).getHandle();
        StringBuilder result = new StringBuilder();
        for (int chunkX : CHUNKS) {
            BlockPos lava = new BlockPos(chunkX * 16 + 8, this.sourceY, 20);
            var sourceFluid = level.getFluidState(lava);
            var belowFluid = level.getFluidState(lava.below());
            result.append(chunkX).append(':')
                .append(this.world.getBlockAt(chunkX * 16 + 8, this.sourceY, 20).getType())
                .append('/').append(this.world.getBlockAt(chunkX * 16 + 8, this.sourceY - 1, 20).getType())
                .append('/').append(sourceFluid.getType() == Fluids.LAVA ? 'S' : sourceFluid.getType() == Fluids.FLOWING_LAVA ? 'F' : '-')
                .append('/').append(belowFluid.canBeReplacedWith(level, lava.below(), Fluids.LAVA, Direction.DOWN))
                .append(',');
        }
        return result.toString();
    }

    int lavaMaximum() {
        return this.lavaMaximum.get();
    }

    int failures() {
        return this.failures.get();
    }
}

package test.youer.plugin;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;

final class WorldEditProbe {
    private WorldEditProbe() {}

    static int verify(World world) throws ReflectiveOperationException {
        List<Block> blocks = new ArrayList<>();
        for (int chunkX : new int[] {15, 128, 256, 384}) {
            Block block = world.getBlockAt(chunkX * 16 + 10, 124, 10);
            blocks.add(block);
        }
        return verifyBlocks(world, blocks);
    }

    static int verifyRegion(World world, int chunkX) throws ReflectiveOperationException {
        return verifyBlocks(world, List.of(world.getBlockAt(chunkX * 16 + 10, 136, 10)));
    }

    private static int verifyBlocks(World world, List<Block> blocks) throws ReflectiveOperationException {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("WorldEdit");
        if (plugin == null || !plugin.isEnabled()) {
            throw new IllegalStateException("WorldEdit did not enable");
        }
        for (Block block : blocks) {
            if (block.getType() != Material.AIR) {
                throw new IllegalStateException("WorldEdit probe position is occupied: " + block.getLocation());
            }
        }

        ClassLoader loader = plugin.getClass().getClassLoader();
        Class<?> adapter = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter", true, loader);
        Class<?> worldType = Class.forName("com.sk89q.worldedit.world.World", true, loader);
        Class<?> worldEditType = Class.forName("com.sk89q.worldedit.WorldEdit", true, loader);
        Class<?> vectorType = Class.forName("com.sk89q.worldedit.math.BlockVector3", true, loader);
        Class<?> holderType = Class.forName("com.sk89q.worldedit.world.block.BlockStateHolder", true, loader);
        Class<?> blockTypes = Class.forName("com.sk89q.worldedit.world.block.BlockTypes", true, loader);
        Object editWorld = adapter.getMethod("adapt", World.class).invoke(null, world);
        Object editor = worldEditType.getMethod("getInstance").invoke(null);
        Object stone = blockTypes.getField("STONE").get(null);
        Object stoneState = stone.getClass().getMethod("getDefaultState").invoke(stone);

        try {
            AutoCloseable session = (AutoCloseable) worldEditType.getMethod("newEditSession", worldType).invoke(editor, editWorld);
            try (session) {
                var setBlock = session.getClass().getMethod("setBlock", vectorType, holderType);
                var at = vectorType.getMethod("at", int.class, int.class, int.class);
                for (Block block : blocks) {
                    Object pos = at.invoke(null, block.getX(), block.getY(), block.getZ());
                    if (!Boolean.TRUE.equals(setBlock.invoke(session, pos, stoneState))) {
                        throw new IllegalStateException("WorldEdit did not change " + block.getLocation());
                    }
                }
            }
            for (Block block : blocks) {
                if (block.getType() != Material.STONE) {
                    throw new IllegalStateException("WorldEdit edit was not visible at " + block.getLocation());
                }
            }
        } catch (ReflectiveOperationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("WorldEdit edit failed", ex);
        } finally {
            for (Block block : blocks) {
                block.setType(Material.AIR, false);
            }
        }
        if (blocks.stream().anyMatch(block -> block.getType() != Material.AIR)) {
            throw new IllegalStateException("WorldEdit probe blocks were not removed");
        }
        return blocks.size();
    }
}

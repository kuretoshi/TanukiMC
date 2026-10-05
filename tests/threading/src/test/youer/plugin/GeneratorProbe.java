package test.youer.plugin;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.HeightMap;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;

final class GeneratorProbe extends ChunkGenerator {
    private final boolean vanillaDecorations;
    private final ConcurrentHashMap<String, AtomicInteger> populations = new ConcurrentHashMap<>();

    private GeneratorProbe(boolean vanillaDecorations) {
        this.vanillaDecorations = vanillaDecorations;
    }

    static void verify(boolean vanillaDecorations) {
        GeneratorProbe generator = new GeneratorProbe(vanillaDecorations);
        World world = new WorldCreator("phase_generator_" + vanillaDecorations).seed(42L)
            .generateStructures(false).generator(generator).createWorld();
        if (world == null) {
            throw new IllegalStateException("Custom world was not created");
        }
        boolean generatedBefore = world.isChunkGenerated(0, 0);
        world.getChunkAt(0, 0);
        AtomicInteger count = generator.populations.get("0,0");
        if ((!generatedBefore && (count == null || count.get() != 1))
            || (count != null && count.get() != 1)
            || world.getBlockAt(8, 65, 8).getType() != Material.GOLD_BLOCK) {
            throw new IllegalStateException("Populator result must survive world reload, with vanilla decorations=" + vanillaDecorations);
        }
    }

    @Override
    public void generateSurface(WorldInfo world, Random random, int chunkX, int chunkZ, ChunkData data) {
        data.setRegion(0, 63, 0, 16, 64, 16, Material.GRASS_BLOCK);
    }

    @Override
    public int getBaseHeight(WorldInfo world, Random random, int x, int z, HeightMap heightMap) {
        return 64;
    }

    @Override
    public boolean shouldGenerateDecorations() {
        return this.vanillaDecorations;
    }

    @Override
    public List<BlockPopulator> getDefaultPopulators(World world) {
        return List.of(new BlockPopulator() {
            @Override
            public void populate(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, LimitedRegion region) {
                populations.computeIfAbsent(chunkX + "," + chunkZ, key -> new AtomicInteger()).incrementAndGet();
                region.setType((chunkX << 4) + 8, 65, (chunkZ << 4) + 8, Material.GOLD_BLOCK);
            }
        });
    }
}

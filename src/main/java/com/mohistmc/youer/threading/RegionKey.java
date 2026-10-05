package com.mohistmc.youer.threading;

import java.util.Objects;
import java.util.UUID;
import org.bukkit.World;

public record RegionKey(UUID worldId, int regionX, int regionZ) {

    public static RegionKey fromChunk(World world, int chunkX, int chunkZ) {
        int shift = ThreadedServer.regionChunkShift();
        return new RegionKey(world.getUID(), chunkX >> shift, chunkZ >> shift);
    }

    @Override
    public String toString() {
        return Objects.toString(this.worldId) + ":" + this.regionX + "," + this.regionZ;
    }
}

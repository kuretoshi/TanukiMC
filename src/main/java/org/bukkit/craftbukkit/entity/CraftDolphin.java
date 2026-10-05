package org.bukkit.craftbukkit.entity;

import com.mohistmc.youer.threading.ThreadedServer;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Dolphin;

public class CraftDolphin extends CraftWaterMob implements Dolphin {

    public CraftDolphin(CraftServer server, net.minecraft.world.entity.animal.Dolphin entity) {
        super(server, entity);
    }

    @Override
    public net.minecraft.world.entity.animal.Dolphin getHandle() {
        return (net.minecraft.world.entity.animal.Dolphin) super.getHandle();
    }

    @Override
    public String toString() {
        return "CraftDolphin";
    }

    // Paper start - Missing Dolphin API
    @Override
    public int getMoistness() {
        return this.getHandle().getMoistnessLevel();
    }

    @Override
    public void setMoistness(int moistness) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setMoisntessLevel(moistness);
            return null;
        });
    }

    @Override
    public void setHasFish(boolean hasFish) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setGotFish(hasFish);
            return null;
        });
    }

    @Override
    public boolean hasFish() {
        return this.getHandle().gotFish();
    }

    @Override
    public org.bukkit.Location getTreasureLocation() {
        return io.papermc.paper.util.MCUtil.toLocation(this.getHandle().level(), this.getHandle().getTreasurePos());
    }

    @Override
    public void setTreasureLocation(org.bukkit.Location location) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setTreasurePos(io.papermc.paper.util.MCUtil.toBlockPosition(location));
            return null;
        });
    }
    // Paper end - Missing Dolphin API
}

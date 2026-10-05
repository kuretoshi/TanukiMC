package org.bukkit.craftbukkit.entity;

import com.google.common.base.Preconditions;
import com.mohistmc.youer.threading.ThreadedServer;
import net.minecraft.world.entity.vehicle.MinecartTNT;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.minecart.ExplosiveMinecart;

public final class CraftMinecartTNT extends CraftMinecart implements ExplosiveMinecart {
    CraftMinecartTNT(CraftServer server, MinecartTNT entity) {
        super(server, entity);
    }

    @Override
    public void setFuseTicks(int ticks) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().fuse = ticks;
            return null;
        });
    }

    @Override
    public int getFuseTicks() {
        return this.getHandle().getFuse();
    }

    @Override
    public void ignite() {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().primeFuse();
            return null;
        });
    }

    @Override
    public boolean isIgnited() {
        return this.getHandle().isPrimed();
    }

    @Override
    public void explode() {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().explode(this.getHandle().getDeltaMovement().horizontalDistanceSqr());
            return null;
        });
    }

    @Override
    public void explode(double power) {
        Preconditions.checkArgument(0 <= power && power <= 5, "Power must be in range [0, 5] (got %s)", power);

        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().explode(power);
            return null;
        });
    }

    @Override
    public MinecartTNT getHandle() {
        return (MinecartTNT) super.getHandle();
    }

    @Override
    public String toString() {
        return "CraftMinecartTNT";
    }
}

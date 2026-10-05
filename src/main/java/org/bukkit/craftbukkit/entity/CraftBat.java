package org.bukkit.craftbukkit.entity;

import com.mohistmc.youer.threading.ThreadedServer;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Bat;

public class CraftBat extends CraftAmbient implements Bat {
    public CraftBat(CraftServer server, net.minecraft.world.entity.ambient.Bat entity) {
        super(server, entity);
    }

    @Override
    public net.minecraft.world.entity.ambient.Bat getHandle() {
        return (net.minecraft.world.entity.ambient.Bat) this.entity;
    }

    @Override
    public String toString() {
        return "CraftBat";
    }

    @Override
    public boolean isAwake() {
        return !this.getHandle().isResting();
    }

    @Override
    public void setAwake(boolean state) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setResting(!state);
            return null;
        });
    }
    // Paper start
    @Override
    public org.bukkit.Location getTargetLocation() {
        net.minecraft.core.BlockPos pos = this.getHandle().targetPosition;
        if (pos == null) {
            return null;
        }

        return io.papermc.paper.util.MCUtil.toLocation(this.getHandle().level(), pos);
    }

    @Override
    public void setTargetLocation(org.bukkit.Location location) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            net.minecraft.core.BlockPos pos = null;
            if (location != null) {
                pos = io.papermc.paper.util.MCUtil.toBlockPosition(location);
            }

            this.getHandle().targetPosition = pos;
            return null;
        });
    }
    // Paper end
}

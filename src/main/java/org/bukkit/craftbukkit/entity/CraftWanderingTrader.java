package org.bukkit.craftbukkit.entity;

import com.mohistmc.youer.threading.ThreadedServer;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.WanderingTrader;

public class CraftWanderingTrader extends CraftAbstractVillager implements WanderingTrader {

    public CraftWanderingTrader(CraftServer server, net.minecraft.world.entity.npc.WanderingTrader entity) {
        super(server, entity);
    }

    @Override
    public net.minecraft.world.entity.npc.WanderingTrader getHandle() {
        return (net.minecraft.world.entity.npc.WanderingTrader) this.entity;
    }

    @Override
    public String toString() {
        return "CraftWanderingTrader";
    }

    @Override
    public int getDespawnDelay() {
        return this.getHandle().getDespawnDelay();
    }

    @Override
    public void setDespawnDelay(int despawnDelay) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setDespawnDelay(despawnDelay);
            return null;
        });
    }

    // Paper start - Add more WanderingTrader API
    @Override
    public void setCanDrinkPotion(boolean bool) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            getHandle().canDrinkPotion = bool;
            return null;
        });
    }

    @Override
    public boolean canDrinkPotion() {
        return getHandle().canDrinkPotion;
    }

    @Override
    public void setCanDrinkMilk(boolean bool) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            getHandle().canDrinkMilk = bool;
            return null;
        });
    }

    @Override
    public boolean canDrinkMilk() {
        return getHandle().canDrinkMilk;
    }

    @Override
    public org.bukkit.Location getWanderingTowards() {
        net.minecraft.core.BlockPos pos = this.getHandle().getWanderTarget();
        if (pos == null) {
            return null;
        }

        return io.papermc.paper.util.MCUtil.toLocation(this.getHandle().level(), pos);
    }

    @Override
    public void setWanderingTowards(org.bukkit.Location location) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            net.minecraft.core.BlockPos pos = null;
            if (location != null) {
                pos = io.papermc.paper.util.MCUtil.toBlockPosition(location);
            }

            this.getHandle().setWanderTarget(pos);
            return null;
        });
    }
    // Paper end
}

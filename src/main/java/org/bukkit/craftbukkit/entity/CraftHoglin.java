package org.bukkit.craftbukkit.entity;

import com.google.common.base.Preconditions;
import com.mohistmc.youer.threading.ThreadedServer;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Hoglin;

public class CraftHoglin extends CraftAnimals implements Hoglin, CraftEnemy {

    public CraftHoglin(CraftServer server, net.minecraft.world.entity.monster.hoglin.Hoglin entity) {
        super(server, entity);
    }

    @Override
    public boolean isImmuneToZombification() {
        return this.getHandle().isImmuneToZombification();
    }

    @Override
    public void setImmuneToZombification(boolean flag) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setImmuneToZombification(flag);
            return null;
        });
    }

    @Override
    public boolean isAbleToBeHunted() {
        return this.getHandle().cannotBeHunted;
    }

    @Override
    public void setIsAbleToBeHunted(boolean flag) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().cannotBeHunted = flag;
            return null;
        });
    }

    @Override
    public int getConversionTime() {
        Preconditions.checkState(this.isConverting(), "Entity not converting");
        return this.getHandle().timeInOverworld;
    }

    @Override
    public void setConversionTime(int time) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            if (time < 0) {
                this.getHandle().timeInOverworld = -1;
                this.getHandle().setImmuneToZombification(false);
            } else {
                this.getHandle().timeInOverworld = time;
            }
            return null;
        });
    }

    @Override
    public boolean isConverting() {
        return this.getHandle().isConverting();
    }

    @Override
    public net.minecraft.world.entity.monster.hoglin.Hoglin getHandle() {
        return (net.minecraft.world.entity.monster.hoglin.Hoglin) this.entity;
    }

    @Override
    public String toString() {
        return "CraftHoglin";
    }
}

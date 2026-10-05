package org.bukkit.craftbukkit.entity;

import com.google.common.base.Preconditions;
import com.mohistmc.youer.threading.ThreadedServer;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.PiglinAbstract;

public class CraftPiglinAbstract extends CraftMonster implements PiglinAbstract {

    public CraftPiglinAbstract(CraftServer server, AbstractPiglin entity) {
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
    public boolean isBaby() {
        return this.getHandle().isBaby();
    }

    @Override
    public void setBaby(boolean flag) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setBaby(flag);
            return null;
        });
    }

    @Override
    public int getAge() {
        return this.getHandle().isBaby() ? -1 : 0;
    }

    @Override
    public void setAge(int i) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setBaby(i < 0);
            return null;
        });
    }

    @Override
    public void setAgeLock(boolean b) {
    }

    @Override
    public boolean getAgeLock() {
        return false;
    }

    @Override
    public void setBaby() {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setBaby(true);
            return null;
        });
    }

    @Override
    public void setAdult() {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setBaby(false);
            return null;
        });
    }

    @Override
    public boolean isAdult() {
        return !this.getHandle().isBaby();
    }

    @Override
    public boolean canBreed() {
        return false;
    }

    @Override
    public void setBreed(boolean b) {
    }

    @Override
    public AbstractPiglin getHandle() {
        return (AbstractPiglin) super.getHandle();
    }
}

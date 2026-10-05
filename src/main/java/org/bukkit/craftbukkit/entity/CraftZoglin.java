package org.bukkit.craftbukkit.entity;

import com.mohistmc.youer.threading.ThreadedServer;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Zoglin;

public class CraftZoglin extends CraftMonster implements Zoglin {

    public CraftZoglin(CraftServer server, net.minecraft.world.entity.monster.Zoglin entity) {
        super(server, entity);
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
    public net.minecraft.world.entity.monster.Zoglin getHandle() {
        return (net.minecraft.world.entity.monster.Zoglin) this.entity;
    }

    @Override
    public String toString() {
        return "CraftZoglin";
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
}

package org.bukkit.craftbukkit.entity;

import com.mohistmc.youer.threading.ThreadedServer;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Ghast;

public class CraftGhast extends CraftFlying implements Ghast, CraftEnemy {

    public CraftGhast(CraftServer server, net.minecraft.world.entity.monster.Ghast entity) {
        super(server, entity);
    }

    @Override
    public net.minecraft.world.entity.monster.Ghast getHandle() {
        return (net.minecraft.world.entity.monster.Ghast) this.entity;
    }

    @Override
    public String toString() {
        return "CraftGhast";
    }

    @Override
    public boolean isCharging() {
        return this.getHandle().isCharging();
    }

    @Override
    public void setCharging(boolean flag) {
        ThreadedServer.withWorldMutationLock(() -> {
            this.getHandle().setCharging(flag);
            return null;
        });
    }

    // Paper start
    @Override
    public int getExplosionPower() {
        return this.getHandle().getExplosionPower();
    }

    @Override
    public void setExplosionPower(int explosionPower) {
        com.google.common.base.Preconditions.checkArgument(explosionPower >= 0 && explosionPower <= 127, "The explosion power has to be between 0 and 127");
        ThreadedServer.withWorldMutationLock(() -> {
            this.getHandle().setExplosionPower(explosionPower);
            return null;
        });
    }
    // Paper end
}

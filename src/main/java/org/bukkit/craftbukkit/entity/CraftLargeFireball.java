package org.bukkit.craftbukkit.entity;

import com.mohistmc.youer.threading.ThreadedServer;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.LargeFireball;

public class CraftLargeFireball extends CraftSizedFireball implements LargeFireball {
    public CraftLargeFireball(CraftServer server, net.minecraft.world.entity.projectile.LargeFireball entity) {
        super(server, entity);
    }

    @Override
    public void setYield(float yield) {
        ThreadedServer.withWorldMutationLock(() -> {
            super.setYield(yield);
            this.getHandle().explosionPower = (int) yield;
            return null;
        });
    }

    @Override
    public net.minecraft.world.entity.projectile.LargeFireball getHandle() {
        return (net.minecraft.world.entity.projectile.LargeFireball) this.entity;
    }

    @Override
    public String toString() {
        return "CraftLargeFireball";
    }
}

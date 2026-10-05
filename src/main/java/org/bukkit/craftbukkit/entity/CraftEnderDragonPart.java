package org.bukkit.craftbukkit.entity;

import com.mohistmc.youer.threading.ThreadedServer;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EnderDragonPart;
import org.bukkit.entity.Entity;

public class CraftEnderDragonPart extends CraftComplexPart implements EnderDragonPart {
    public CraftEnderDragonPart(CraftServer server, net.minecraft.world.entity.boss.EnderDragonPart entity) {
        super(server, entity);
    }

    @Override
    public EnderDragon getParent() {
        return (EnderDragon) super.getParent();
    }

    @Override
    public net.minecraft.world.entity.boss.EnderDragonPart getHandle() {
        return (net.minecraft.world.entity.boss.EnderDragonPart) this.entity;
    }

    @Override
    public String toString() {
        return "CraftEnderDragonPart";
    }

    @Override
    public void damage(double amount, DamageSource damageSource) {
        ThreadedServer.withWorldMutationLock(() -> {
            this.getParent().damage(amount, damageSource);
            return null;
        });
    }

    @Override
    public void damage(double amount) {
        ThreadedServer.withWorldMutationLock(() -> {
            this.getParent().damage(amount);
            return null;
        });
    }

    @Override
    public void damage(double amount, Entity source) {
        ThreadedServer.withWorldMutationLock(() -> {
            this.getParent().damage(amount, source);
            return null;
        });
    }

    @Override
    public double getHealth() {
        return this.getParent().getHealth();
    }

    @Override
    public void setHealth(double health) {
        ThreadedServer.withWorldMutationLock(() -> {
            this.getParent().setHealth(health);
            return null;
        });
    }

    // Paper start - entity heal API
    @Override
    public void heal(final double amount, final org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason reason) {
        ThreadedServer.withWorldMutationLock(() -> {
            this.getParent().heal(amount, reason);
            return null;
        });
    }
    // Paper end - entity heal API

    @Override
    public double getAbsorptionAmount() {
        return this.getParent().getAbsorptionAmount();
    }

    @Override
    public void setAbsorptionAmount(double amount) {
        ThreadedServer.withWorldMutationLock(() -> {
            this.getParent().setAbsorptionAmount(amount);
            return null;
        });
    }

    @Override
    public double getMaxHealth() {
        return this.getParent().getMaxHealth();
    }

    @Override
    public void setMaxHealth(double health) {
        ThreadedServer.withWorldMutationLock(() -> {
            this.getParent().setMaxHealth(health);
            return null;
        });
    }

    @Override
    public void resetMaxHealth() {
        ThreadedServer.withWorldMutationLock(() -> {
            this.getParent().resetMaxHealth();
            return null;
        });
    }
}

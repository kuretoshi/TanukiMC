package org.bukkit.craftbukkit.entity;

import com.google.common.base.Preconditions;
import com.mohistmc.youer.threading.ThreadedServer;
import net.minecraft.core.BlockPos;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.util.CraftLocation;
import org.bukkit.entity.Vex;

public class CraftVex extends CraftMonster implements Vex {

    public CraftVex(CraftServer server, net.minecraft.world.entity.monster.Vex entity) {
        super(server, entity);
    }

    @Override
    public net.minecraft.world.entity.monster.Vex getHandle() {
        return (net.minecraft.world.entity.monster.Vex) super.getHandle();
    }

    // Paper start
    @Override
    public org.bukkit.entity.Mob getSummoner() {
        net.minecraft.world.entity.Mob owner = getHandle().getOwner();
        return owner != null ? (org.bukkit.entity.Mob) owner.getBukkitEntity() : null;
    }

    @Override
    public void setSummoner(org.bukkit.entity.Mob summoner) {
        ThreadedServer.withWorldMutationLock(() -> {
            getHandle().setOwner(summoner == null ? null : ((CraftMob) summoner).getHandle());
            return null;
        });
    }

    @Override
    public boolean hasLimitedLifetime() {
        return this.getHandle().hasLimitedLife;
    }

    @Override
    public void setLimitedLifetime(boolean hasLimitedLifetime) {
        ThreadedServer.withWorldMutationLock(() -> {
            this.getHandle().hasLimitedLife = hasLimitedLifetime;
            return null;
        });
    }

    @Override
    public int getLimitedLifetimeTicks() {
        return this.getHandle().limitedLifeTicks;
    }

    @Override
    public void setLimitedLifetimeTicks(int ticks) {
        ThreadedServer.withWorldMutationLock(() -> {
            this.getHandle().limitedLifeTicks = ticks;
            return null;
        });
    }
    // Paper end

    @Override
    public String toString() {
        return "CraftVex";
    }

    @Override
    public boolean isCharging() {
        return this.getHandle().isCharging();
    }

    @Override
    public void setCharging(boolean charging) {
        ThreadedServer.withWorldMutationLock(() -> {
            this.getHandle().setIsCharging(charging);
            return null;
        });
    }

    @Override
    public Location getBound() {
        BlockPos blockPosition = this.getHandle().getBoundOrigin();
        return (blockPosition == null) ? null : CraftLocation.toBukkit(blockPosition, this.getWorld());
    }

    @Override
    public void setBound(Location location) {
        ThreadedServer.withWorldMutationLock(() -> {
            if (location == null) {
                this.getHandle().setBoundOrigin(null);
            } else {
                Preconditions.checkArgument(this.getWorld().equals(location.getWorld()), "The bound world cannot be different to the entity's world.");
                this.getHandle().setBoundOrigin(CraftLocation.toBlockPosition(location));
            }
            return null;
        });
    }

    @Override
    public int getLifeTicks() {
        return this.getHandle().limitedLifeTicks;
    }

    @Override
    public void setLifeTicks(int lifeTicks) {
        ThreadedServer.withWorldMutationLock(() -> {
            this.getHandle().setLimitedLife(lifeTicks);
            if (lifeTicks < 0) {
                this.getHandle().hasLimitedLife = false;
            }
            return null;
        });
    }

    @Override
    public boolean hasLimitedLife() {
        return this.getHandle().hasLimitedLife;
    }
}

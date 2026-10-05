package org.bukkit.craftbukkit.entity;

import com.google.common.base.Preconditions;
import com.mohistmc.youer.threading.ThreadedServer;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.phys.Vec3;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Fireball;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class CraftFireball extends AbstractProjectile implements Fireball {
    public CraftFireball(CraftServer server, AbstractHurtingProjectile entity) {
        super(server, entity);
    }

    @Override
    public float getYield() {
        return this.getHandle().bukkitYield;
    }

    @Override
    public boolean isIncendiary() {
        return this.getHandle().isIncendiary;
    }

    @Override
    public void setIsIncendiary(boolean isIncendiary) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().isIncendiary = isIncendiary;
            return null;
        });
    }

    @Override
    public void setYield(float yield) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().bukkitYield = yield;
            return null;
        });
    }

    // Paper - moved to AbstractProjectile

    @Override
    public Vector getDirection() {
        return getAcceleration();
    }

    @Override
    public void setDirection(Vector direction) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            Preconditions.checkArgument(direction != null, "Vector direction cannot be null");
            if (direction.isZero()) {
                setVelocity(direction);
                setAcceleration(direction);
                return null;
            }
            Vector normalizedDirection = direction.clone().normalize();
            setVelocity(normalizedDirection.clone().multiply(getVelocity().length()));
            setAcceleration(normalizedDirection.multiply(getAcceleration().length()));
            return null;
        });
    }

    @Override
    public void setAcceleration(@NotNull Vector acceleration) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            Preconditions.checkArgument(acceleration != null, "Vector acceleration cannot be null");
            // SPIGOT-6993: EntityFireball#assignPower will normalize the given values
            // Note: Because of MC-80142 the fireball will stutter on the client when setting the power to something other than 0 or the normalized vector * 0.1
            getHandle().assignDirectionalMovement(new Vec3(acceleration.getX(), acceleration.getY(), acceleration.getZ()), acceleration.length());
            update(); // SPIGOT-6579
            return null;
        });
    }

    @NotNull
    @Override
    public Vector getAcceleration() {
        Vec3 delta = getHandle().getDeltaMovement();
        return new Vector(delta.x, delta.y, delta.z);
    }

    // Paper start - Expose power on fireball projectiles
    @Override
    public void setPower(final Vector power) {
        this.setAcceleration(power);
    }

    @Override
    public Vector getPower() {
        return this.getAcceleration();
    }
    // Paper end - Expose power on fireball projectiles

    @Override
    public AbstractHurtingProjectile getHandle() {
        return (AbstractHurtingProjectile) this.entity;
    }

    @Override
    public String toString() {
        return "CraftFireball";
    }
}

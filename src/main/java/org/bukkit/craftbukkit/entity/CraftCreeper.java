package org.bukkit.craftbukkit.entity;

import com.google.common.base.Preconditions;
import com.mohistmc.youer.threading.ThreadedServer;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.CreeperPowerEvent;

public class CraftCreeper extends CraftMonster implements Creeper {

    public CraftCreeper(CraftServer server, net.minecraft.world.entity.monster.Creeper entity) {
        super(server, entity);
    }

    @Override
    public boolean isPowered() {
        return this.getHandle().isPowered();
    }

    @Override
    public void setPowered(boolean powered) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            CreeperPowerEvent.PowerCause cause = powered ? CreeperPowerEvent.PowerCause.SET_ON : CreeperPowerEvent.PowerCause.SET_OFF;

            // only call event when we are not in world generation
            if (this.getHandle().generation || !this.callPowerEvent(cause)) {
                this.getHandle().setPowered(powered);
            }
            return null;
        });
    }

    private boolean callPowerEvent(CreeperPowerEvent.PowerCause cause) {
        CreeperPowerEvent event = new CreeperPowerEvent((Creeper) this.getHandle().getBukkitEntity(), cause);
        this.server.getPluginManager().callEvent(event);
        return event.isCancelled();
    }

    @Override
    public void setMaxFuseTicks(int ticks) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            Preconditions.checkArgument(ticks >= 0, "ticks < 0");

            this.getHandle().maxSwell = ticks;
            return null;
        });
    }

    @Override
    public int getMaxFuseTicks() {
        return this.getHandle().maxSwell;
    }

    @Override
    public void setFuseTicks(int ticks) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            Preconditions.checkArgument(ticks >= 0, "ticks < 0");
            Preconditions.checkArgument(ticks <= this.getMaxFuseTicks(), "ticks > maxFuseTicks");

            this.getHandle().swell = ticks;
            return null;
        });
    }

    @Override
    public int getFuseTicks() {
        return this.getHandle().swell;
    }

    @Override
    public void setExplosionRadius(int radius) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            Preconditions.checkArgument(radius >= 0, "radius < 0");

            this.getHandle().explosionRadius = radius;
            return null;
        });
    }

    @Override
    public int getExplosionRadius() {
        return this.getHandle().explosionRadius;
    }

    @Override
    public void explode() {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().explodeCreeper();
            return null;
        });
    }

    @Override
    public void ignite(Entity entity) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            Preconditions.checkNotNull(entity, "entity cannot be null");
            getHandle().entityIgniter = ((CraftEntity) entity).getHandle();
            getHandle().ignite();
            return null;
        });
    }

    @Override
    public void ignite() {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().ignite();
            return null;
        });
    }

    @Override
    public Entity getIgniter() {
        return (getHandle().entityIgniter != null) ? getHandle().entityIgniter.getBukkitEntity() : null;
    }

    @Override
    public net.minecraft.world.entity.monster.Creeper getHandle() {
        return (net.minecraft.world.entity.monster.Creeper) this.entity;
    }

    @Override
    public String toString() {
        return "CraftCreeper";
    }

    // Paper start
    @Override
    public void setIgnited(boolean ignited) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            getHandle().setIgnited(ignited);
            return null;
        });
    }

    @Override
    public boolean isIgnited() {
        return getHandle().isIgnited();
    }
    // Paper end
}

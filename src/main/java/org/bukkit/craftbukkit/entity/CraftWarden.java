package org.bukkit.craftbukkit.entity;

import com.google.common.base.Preconditions;
import com.mohistmc.youer.threading.ThreadedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.warden.WardenAi;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public class CraftWarden extends CraftMonster implements org.bukkit.entity.Warden {

    public CraftWarden(CraftServer server, Warden entity) {
        super(server, entity);
    }

    @Override
    public Warden getHandle() {
        return (Warden) this.entity;
    }

    @Override
    public String toString() {
        return "CraftWarden";
    }

    @Override
    public int getAnger() {
        return this.getHandle().getAngerManagement().getActiveAnger(this.getHandle().getTarget());
    }

    @Override
    public int getAnger(Entity entity) {
        Preconditions.checkArgument(entity != null, "Entity cannot be null");

        return this.getHandle().getAngerManagement().getActiveAnger(((CraftEntity) entity).getHandle());
    }

    // Paper start
    @Override
    public int getHighestAnger() {
        return this.getHandle().getAngerManagement().getActiveAnger(null);
    }
    // Paper end

    @Override
    public void increaseAnger(Entity entity, int increase) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            Preconditions.checkArgument(entity != null, "Entity cannot be null");

            this.getHandle().getAngerManagement().increaseAnger(((CraftEntity) entity).getHandle(), increase);
            return null;
        });
    }

    @Override
    public void setAnger(Entity entity, int anger) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            Preconditions.checkArgument(entity != null, "Entity cannot be null");

            this.getHandle().clearAnger(((CraftEntity) entity).getHandle());
            this.getHandle().getAngerManagement().increaseAnger(((CraftEntity) entity).getHandle(), anger);
            return null;
        });
    }

    @Override
    public void clearAnger(Entity entity) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            Preconditions.checkArgument(entity != null, "Entity cannot be null");

            this.getHandle().clearAnger(((CraftEntity) entity).getHandle());
            return null;
        });
    }

    @Override
    public LivingEntity getEntityAngryAt() {
        return (LivingEntity) this.getHandle().getEntityAngryAt().map(net.minecraft.world.entity.Entity::getBukkitEntity).orElse(null);
    }

    @Override
    public void setDisturbanceLocation(Location location) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            Preconditions.checkArgument(location != null, "Location cannot be null");

            WardenAi.setDisturbanceLocation(this.getHandle(), BlockPos.containing(location.getX(), location.getY(), location.getZ()));
            return null;
        });
    }

    @Override
    public AngerLevel getAngerLevel() {
        return switch (this.getHandle().getAngerLevel()) {
            case CALM -> AngerLevel.CALM;
            case AGITATED -> AngerLevel.AGITATED;
            case ANGRY -> AngerLevel.ANGRY;
        };
    }
}

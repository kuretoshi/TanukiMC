package org.bukkit.craftbukkit.entity;

import java.util.UUID;
import com.mohistmc.youer.threading.ThreadedServer;
import net.minecraft.world.entity.TamableAnimal;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Tameable;

public class CraftTameableAnimal extends CraftAnimals implements Tameable, Creature {
    public CraftTameableAnimal(CraftServer server, TamableAnimal entity) {
        super(server, entity);
    }

    @Override
    public TamableAnimal getHandle() {
        return (TamableAnimal) super.getHandle();
    }

    @Override
    public UUID getOwnerUniqueId() {
        return getOwnerUUID();
    }

    public UUID getOwnerUUID() {
        try {
            return this.getHandle().getOwnerUUID();
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public void setOwnerUUID(UUID uuid) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setOwnerUUID(uuid);
            return null;
        });
    }

    @Override
    public AnimalTamer getOwner() {
        if (this.getOwnerUUID() == null) {
            return null;
        }

        AnimalTamer owner = this.getServer().getPlayer(this.getOwnerUUID());
        if (owner == null) {
            owner = this.getServer().getOfflinePlayer(this.getOwnerUUID());
        }

        return owner;
    }

    @Override
    public boolean isTamed() {
        return this.getHandle().isTame();
    }

    @Override
    public void setOwner(AnimalTamer tamer) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            if (tamer != null) {
                this.setTamed(true);
                this.getHandle().setTargetReason(null, false);
                this.getHandle().setTarget(null);
                this.setOwnerUUID(tamer.getUniqueId());
            } else {
                this.setTamed(false);
                this.setOwnerUUID(null);
            }
            return null;
        });
    }

    @Override
    public void setTamed(boolean tame) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setTame(tame, true);
            if (!tame) {
                this.setOwnerUUID(null);
            }
            return null;
        });
    }

    public boolean isSitting() {
        return this.getHandle().isInSittingPose();
    }

    public void setSitting(boolean sitting) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setInSittingPose(sitting);
            this.getHandle().setOrderedToSit(sitting);
            return null;
        });
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{owner=" + this.getOwner() + ",tamed=" + this.isTamed() + "}";
    }
}

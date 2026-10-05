package org.bukkit.craftbukkit.entity;

import com.google.common.base.Preconditions;
import com.mohistmc.youer.threading.ThreadedServer;
import java.util.UUID;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.inventory.CraftSaddledInventory;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Horse;
import org.bukkit.inventory.AbstractHorseInventory;

public abstract class CraftAbstractHorse extends CraftAnimals implements AbstractHorse {

    public CraftAbstractHorse(CraftServer server, net.minecraft.world.entity.animal.horse.AbstractHorse entity) {
        super(server, entity);
    }

    @Override
    public net.minecraft.world.entity.animal.horse.AbstractHorse getHandle() {
        return (net.minecraft.world.entity.animal.horse.AbstractHorse) this.entity;
    }

    @Override
    public void setVariant(Horse.Variant variant) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public int getDomestication() {
        return this.getHandle().getTemper();
    }

    @Override
    public void setDomestication(int value) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            Preconditions.checkArgument(value >= 0 && value <= this.getMaxDomestication(), "Domestication level (%s) need to be between %s and %s (max domestication)", value, 0, this.getMaxDomestication());
            this.getHandle().setTemper(value);
            return null;
        });
    }

    @Override
    public int getMaxDomestication() {
        return this.getHandle().getMaxTemper();
    }

    @Override
    public void setMaxDomestication(int value) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            Preconditions.checkArgument(value > 0, "Max domestication (%s) cannot be zero or less", value);
            this.getHandle().maxDomestication = value;
            return null;
        });
    }

    @Override
    public double getJumpStrength() {
        return this.getHandle().getAttributeValue(Attributes.JUMP_STRENGTH);
    }

    @Override
    public void setJumpStrength(double strength) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            Preconditions.checkArgument(strength >= 0, "Jump strength (%s) cannot be less than zero", strength);
            this.getHandle().getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(strength);
            return null;
        });
    }

    @Override
    public boolean isTamed() {
        return this.getHandle().isTamed();
    }

    @Override
    public void setTamed(boolean tamed) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setTamed(tamed);
            return null;
        });
    }

    @Override
    public AnimalTamer getOwner() {
        if (this.getOwnerUUID() == null) return null;
        return this.getServer().getOfflinePlayer(this.getOwnerUUID());
    }

    @Override
    public void setOwner(AnimalTamer owner) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            if (owner != null) {
                this.setTamed(true);
                this.getHandle().setTargetReason(null, false);
                this.getHandle().setTarget(null);
                this.setOwnerUUID(owner.getUniqueId());
            } else {
                this.setTamed(false);
                this.setOwnerUUID(null);
            }
            return null;
        });
    }

    @Override
    public UUID getOwnerUniqueId() {
        return getOwnerUUID();
    }

    public UUID getOwnerUUID() {
        return this.getHandle().getOwnerUUID();
    }

    public void setOwnerUUID(UUID uuid) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setOwnerUUID(uuid);
            return null;
        });
    }

    @Override
    public boolean isEatingHaystack() {
        return this.getHandle().isEating();
    }

    @Override
    public void setEatingHaystack(boolean eatingHaystack) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setEating(eatingHaystack);
            return null;
        });
    }

    @Override
    public AbstractHorseInventory getInventory() {
        return new CraftSaddledInventory(this.getHandle().inventory, this.getHandle().getBodyArmorAccess()); // Paper - use both inventories
    }

    // Paper start - Horse API
    @Override
    public boolean isEatingGrass() {
        return this.getHandle().isEating();
    }

    @Override
    public void setEatingGrass(boolean eating) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setEating(eating);
            return null;
        });
    }

    @Override
    public boolean isRearing() {
        return this.getHandle().isStanding();
    }

    @Override
    public void setRearing(boolean rearing) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setForceStanding(rearing);
            return null;
        });
    }

    @Override
    public boolean isEating() {
        return this.getHandle().isMouthOpen();
    }

    @Override
    public void setEating(boolean eating) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setMouthOpen(eating);
            return null;
        });
    }
    // Paper end - Horse API
}

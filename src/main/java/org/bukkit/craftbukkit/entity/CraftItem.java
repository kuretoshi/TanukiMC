package org.bukkit.craftbukkit.entity;

import java.util.UUID;
import com.mohistmc.youer.threading.ThreadedServer;
import net.minecraft.world.entity.item.ItemEntity;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

public class CraftItem extends CraftEntity implements Item {

    // Paper start
    private final static int NO_AGE_TIME = (int) Short.MIN_VALUE;
    private final static int NO_PICKUP_TIME = (int) Short.MAX_VALUE;
    // Paper end

    public CraftItem(CraftServer server, ItemEntity entity) {
        super(server, entity);
    }

    @Override
    public ItemEntity getHandle() {
        return (ItemEntity) this.entity;
    }

    @Override
    public ItemStack getItemStack() {
        return CraftItemStack.asCraftMirror(this.getHandle().getItem());
    }

    @Override
    public void setItemStack(ItemStack stack) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setItem(CraftItemStack.asNMSCopy(stack));
            return null;
        });
    }

    @Override
    public int getPickupDelay() {
        return this.getHandle().pickupDelay;
    }

    @Override
    public void setPickupDelay(int delay) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().pickupDelay = Math.min(delay, Short.MAX_VALUE);
            return null;
        });
    }

    @Override
    public void setUnlimitedLifetime(boolean unlimited) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            if (unlimited) {
                // See EntityItem#INFINITE_LIFETIME
                this.getHandle().age = Short.MIN_VALUE;
            } else {
                this.getHandle().age = this.getTicksLived();
            }
            return null;
        });
    }

    @Override
    public boolean isUnlimitedLifetime() {
        return this.getHandle().age == Short.MIN_VALUE;
    }

    @Override
    public void setTicksLived(int value) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            super.setTicksLived(value);

            // Second field for EntityItem (don't set if lifetime is unlimited)
            if (!this.isUnlimitedLifetime()) {
                this.getHandle().age = value;
            }
            return null;
        });
    }

    // Paper start
    @Override
    public boolean canMobPickup() {
        return this.getHandle().canMobPickup;
    }

    @Override
    public void setCanMobPickup(boolean canMobPickup) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().canMobPickup = canMobPickup;
            return null;
        });
    }

    @Override
    public boolean canPlayerPickup() {
        return this.getHandle().pickupDelay != NO_PICKUP_TIME;
    }

    @Override
    public void setCanPlayerPickup(boolean canPlayerPickup) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().pickupDelay = canPlayerPickup ? 0 : NO_PICKUP_TIME;
            return null;
        });
    }

    @Override
    public boolean willAge() {
        return this.getHandle().age != NO_AGE_TIME;
    }

    @Override
    public void setWillAge(boolean willAge) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().age = willAge ? 0 : NO_AGE_TIME;
            return null;
        });
    }

    @org.jetbrains.annotations.NotNull
    @Override
    public net.kyori.adventure.util.TriState getFrictionState() {
        return this.getHandle().frictionState;
    }

    @Override
    public void setFrictionState(@org.jetbrains.annotations.NotNull net.kyori.adventure.util.TriState state) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            java.util.Objects.requireNonNull(state, "state may not be null");
            this.getHandle().frictionState = state;
            return null;
        });
    }

    @Override
    public int getHealth() {
        return this.getHandle().health;
    }

    @Override
    public void setHealth(int health) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            if (health <= 0) {
                this.getHandle().getItem().onDestroyed(this.getHandle());
                this.getHandle().discard(org.bukkit.event.entity.EntityRemoveEvent.Cause.PLUGIN);
            } else {
                this.getHandle().health = health;
            }
            return null;
        });
    }
    // Paper end

    @Override
    public void setOwner(UUID uuid) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setTarget(uuid);
            return null;
        });
    }

    @Override
    public UUID getOwner() {
        return this.getHandle().target;
    }

    @Override
    public void setThrower(UUID uuid) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().thrower = uuid;
            return null;
        });
    }

    @Override
    public UUID getThrower() {
        return this.getHandle().thrower;
    }

    @Override
    public String toString() {
        return "CraftItem";
    }

    // Purpur start
    @Override
    public void setImmuneToCactus(boolean immuneToCactus) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().immuneToCactus = immuneToCactus;
            return null;
        });
    }

    @Override
    public boolean isImmuneToCactus() {
        return this.getHandle().immuneToCactus;
    }

    @Override
    public void setImmuneToExplosion(boolean immuneToExplosion) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().immuneToExplosion = immuneToExplosion;
            return null;
        });
    }

    @Override
    public boolean isImmuneToExplosion() {
        return this.getHandle().immuneToExplosion;
    }

    @Override
    public void setImmuneToFire(@org.jetbrains.annotations.Nullable Boolean immuneToFire) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().immuneToFire = (immuneToFire != null && immuneToFire);
            return null;
        });
    }

    @Override
    public void setImmuneToFire(boolean immuneToFire) {
        this.setImmuneToFire((Boolean) immuneToFire);
    }

    @Override
    public boolean isImmuneToFire() {
        return this.getHandle().immuneToFire;
    }

    @Override
    public void setImmuneToLightning(boolean immuneToLightning) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().immuneToLightning = immuneToLightning;
            return null;
        });
    }

    @Override
    public boolean isImmuneToLightning() {
        return this.getHandle().immuneToLightning;
    }
    // Purpur end
}

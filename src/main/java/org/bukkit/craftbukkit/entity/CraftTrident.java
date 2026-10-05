package org.bukkit.craftbukkit.entity;

import com.mohistmc.youer.threading.ThreadedServer;
import net.minecraft.world.entity.projectile.ThrownTrident;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Trident;
import org.bukkit.inventory.ItemStack;

public class CraftTrident extends CraftAbstractArrow implements Trident {

    public CraftTrident(CraftServer server, ThrownTrident entity) {
        super(server, entity);
    }

    @Override
    public ThrownTrident getHandle() {
        return (ThrownTrident) super.getHandle();
    }

    @Override
    public ItemStack getItem() {
        return CraftItemStack.asBukkitCopy(this.getHandle().pickupItemStack);
    }

    @Override
    public void setItem(ItemStack itemStack) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().pickupItemStack = CraftItemStack.asNMSCopy(itemStack);
            return null;
        });
    }

    @Override
    public String toString() {
        return "CraftTrident";
    }

    // Paper start
    @Override
    public boolean hasGlint() {
        return this.getHandle().isFoil();
    }

    @Override
    public void setGlint(boolean glint) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setFoil(glint);
            return null;
        });
    }

    @Override
    public int getLoyaltyLevel() {
        return this.getHandle().getLoyalty();
    }

    @Override
    public void setLoyaltyLevel(int loyaltyLevel) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            com.google.common.base.Preconditions.checkArgument(loyaltyLevel >= 0 && loyaltyLevel <= 127, "The loyalty level has to be between 0 and 127");
            this.getHandle().setLoyalty((byte) loyaltyLevel);
            return null;
        });
    }

    @Override
    public boolean hasDealtDamage() {
        return this.getHandle().dealtDamage;
    }

    @Override
    public void setHasDealtDamage(boolean hasDealtDamage) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().dealtDamage = hasDealtDamage;
            return null;
        });
    }
    // Paper end
}

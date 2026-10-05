package org.bukkit.craftbukkit.entity;

import com.mohistmc.youer.threading.ThreadedServer;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.ThrowableProjectile;
import org.bukkit.inventory.ItemStack;

public abstract class CraftThrowableProjectile extends CraftProjectile implements ThrowableProjectile {

    public CraftThrowableProjectile(CraftServer server, ThrowableItemProjectile entity) {
        super(server, entity);
    }

    @Override
    public ItemStack getItem() {
        if (this.getHandle().getItem().isEmpty()) {
            return CraftItemStack.asBukkitCopy(new net.minecraft.world.item.ItemStack(this.getHandle().getDefaultItemPublic()));
        } else {
            return CraftItemStack.asBukkitCopy(this.getHandle().getItem());
        }
    }

    @Override
    public void setItem(ItemStack item) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setItem(CraftItemStack.asNMSCopy(item));
            return null;
        });
    }

    @Override
    public ThrowableItemProjectile getHandle() {
        return (ThrowableItemProjectile) this.entity;
    }
}

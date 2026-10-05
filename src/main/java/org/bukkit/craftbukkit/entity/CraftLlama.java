package org.bukkit.craftbukkit.entity;

import com.google.common.base.Preconditions;
import com.mohistmc.youer.threading.ThreadedServer;
import com.destroystokyo.paper.entity.CraftRangedEntity;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.inventory.CraftInventoryLlama;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Llama;
import org.bukkit.inventory.LlamaInventory;

public class CraftLlama extends CraftChestedHorse implements Llama, CraftRangedEntity<net.minecraft.world.entity.animal.horse.Llama> {

    public CraftLlama(CraftServer server, net.minecraft.world.entity.animal.horse.Llama entity) {
        super(server, entity);
    }

    @Override
    public net.minecraft.world.entity.animal.horse.Llama getHandle() {
        return (net.minecraft.world.entity.animal.horse.Llama) super.getHandle();
    }

    @Override
    public Color getColor() {
        return Color.values()[this.getHandle().getVariant().ordinal()];
    }

    @Override
    public void setColor(Color color) {
        Preconditions.checkArgument(color != null, "color");

        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setVariant(net.minecraft.world.entity.animal.horse.Llama.Variant.byId(color.ordinal()));
            return null;
        });
    }

    @Override
    public LlamaInventory getInventory() {
        return new CraftInventoryLlama(this.getHandle().inventory, getHandle().getBodyArmorAccess());
    }

    @Override
    public int getStrength() {
       return this.getHandle().getStrength();
    }

    @Override
    public void setStrength(int strength) {
        Preconditions.checkArgument(1 <= strength && strength <= 5, "strength must be [1,5]");
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            if (strength == this.getStrength()) return null;
            this.getHandle().setStrengthPublic(strength);
            this.getHandle().createInventory();
            return null;
        });
    }

    @Override
    public Horse.Variant getVariant() {
        return Horse.Variant.LLAMA;
    }

    @Override
    public String toString() {
        return "CraftLlama";
    }

    // Paper start
    @Override
    public boolean inCaravan() {
        return this.getHandle().inCaravan();
    }

    @Override
    public void joinCaravan(@org.jetbrains.annotations.NotNull Llama llama) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().joinCaravan(((CraftLlama) llama).getHandle());
            return null;
        });
    }

    @Override
    public void leaveCaravan() {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().leaveCaravan();
            return null;
        });
    }

    @Override
    public boolean hasCaravanTail() {
        return this.getHandle().hasCaravanTail();
    }

    @Override
    public Llama getCaravanHead() {
        return this.getHandle().getCaravanHead() == null ? null : (Llama) this.getHandle().getCaravanHead().getBukkitEntity();
    }

    @Override
    public Llama getCaravanTail() {
        return this.getHandle().caravanTail == null ? null : (Llama) this.getHandle().caravanTail.getBukkitEntity();
    }
    // Paper end

    // Purpur start
    @Override
    public boolean shouldJoinCaravan() {
        return getHandle().shouldJoinCaravan;
    }

    @Override
    public void setShouldJoinCaravan(boolean shouldJoinCaravan) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            getHandle().shouldJoinCaravan = shouldJoinCaravan;
            return null;
        });
    }
    // Purpur end
}

package org.bukkit.craftbukkit.entity;

import com.google.common.base.Preconditions;
import com.mohistmc.youer.threading.ThreadedServer;
import com.destroystokyo.paper.entity.CraftRangedEntity;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.world.item.Item;
import org.bukkit.Material;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.inventory.CraftInventory;
import org.bukkit.craftbukkit.inventory.CraftItemType;
import org.bukkit.entity.Piglin;
import org.bukkit.inventory.Inventory;

public class CraftPiglin extends CraftPiglinAbstract implements Piglin, CraftRangedEntity<net.minecraft.world.entity.monster.piglin.Piglin> {

    public CraftPiglin(CraftServer server, net.minecraft.world.entity.monster.piglin.Piglin entity) {
        super(server, entity);
    }

    @Override
    public boolean isAbleToHunt() {
        return this.getHandle().cannotHunt;
    }

    @Override
    public void setIsAbleToHunt(boolean flag) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().cannotHunt = flag;
            return null;
        });
    }

    @Override
    public boolean addBarterMaterial(Material material) {
        Preconditions.checkArgument(material != null, "material cannot be null");

        Item item = CraftItemType.bukkitToMinecraft(material);
        return ThreadedServer.onMainThreadIfRegionThread(() -> this.getHandle().allowedBarterItems.add(item));
    }

    @Override
    public boolean removeBarterMaterial(Material material) {
        Preconditions.checkArgument(material != null, "material cannot be null");

        Item item = CraftItemType.bukkitToMinecraft(material);
        return ThreadedServer.onMainThreadIfRegionThread(() -> this.getHandle().allowedBarterItems.remove(item));
    }

    @Override
    public boolean addMaterialOfInterest(Material material) {
        Preconditions.checkArgument(material != null, "material cannot be null");

        Item item = CraftItemType.bukkitToMinecraft(material);
        return ThreadedServer.onMainThreadIfRegionThread(() -> this.getHandle().interestItems.add(item));
    }

    @Override
    public boolean removeMaterialOfInterest(Material material) {
        Preconditions.checkArgument(material != null, "material cannot be null");

        Item item = CraftItemType.bukkitToMinecraft(material);
        return ThreadedServer.onMainThreadIfRegionThread(() -> this.getHandle().interestItems.remove(item));
    }

    @Override
    public Set<Material> getInterestList() {
        return Collections.unmodifiableSet(this.getHandle().interestItems.stream().map(CraftItemType::minecraftToBukkit).collect(Collectors.toSet()));
    }

    @Override
    public Set<Material> getBarterList() {
        return Collections.unmodifiableSet(this.getHandle().allowedBarterItems.stream().map(CraftItemType::minecraftToBukkit).collect(Collectors.toSet()));
    }

    @Override
    public Inventory getInventory() {
        return new CraftInventory(this.getHandle().inventory);
    }

    @Override
    public net.minecraft.world.entity.monster.piglin.Piglin getHandle() {
        return (net.minecraft.world.entity.monster.piglin.Piglin) super.getHandle();
    }

    @Override
    public String toString() {
        return "CraftPiglin";
    }
    // Paper start
    @Override
    public void setChargingCrossbow(boolean chargingCrossbow) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setChargingCrossbow(chargingCrossbow);
            return null;
        });
    }

    @Override
    public boolean isChargingCrossbow() {
        return this.getHandle().isChargingCrossbow();
    }

    @Override
    public void setDancing(boolean dancing) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            if (dancing) {
                this.getHandle().getBrain().setMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.DANCING, true);
                this.getHandle().getBrain().setMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.CELEBRATE_LOCATION, this.getHandle().getOnPos());
            } else {
                this.getHandle().getBrain().eraseMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.DANCING);
                this.getHandle().getBrain().eraseMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.CELEBRATE_LOCATION);
            }
            return null;
        });
    }

    @Override
    public void setDancing(long duration) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().getBrain().setMemoryWithExpiry(net.minecraft.world.entity.ai.memory.MemoryModuleType.DANCING, true, duration);
            this.getHandle().getBrain().setMemoryWithExpiry(net.minecraft.world.entity.ai.memory.MemoryModuleType.CELEBRATE_LOCATION, this.getHandle().getOnPos(), duration);
            return null;
        });
    }

    @Override
    public boolean isDancing() {
        return this.getHandle().isDancing();
    }
    // Paper end
}

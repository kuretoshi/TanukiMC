package org.bukkit.craftbukkit.entity;

import com.mohistmc.youer.threading.ThreadedServer;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Goat;

public class CraftGoat extends CraftAnimals implements Goat {

    public CraftGoat(CraftServer server, net.minecraft.world.entity.animal.goat.Goat entity) {
        super(server, entity);
    }

    @Override
    public net.minecraft.world.entity.animal.goat.Goat getHandle() {
        return (net.minecraft.world.entity.animal.goat.Goat) super.getHandle();
    }

    @Override
    public String toString() {
        return "CraftGoat";
    }

    @Override
    public boolean hasLeftHorn() {
        return this.getHandle().hasLeftHorn();
    }

    @Override
    public void setLeftHorn(boolean hasHorn) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().getEntityData().set(net.minecraft.world.entity.animal.goat.Goat.DATA_HAS_LEFT_HORN, hasHorn);
            return null;
        });
    }

    @Override
    public boolean hasRightHorn() {
        return this.getHandle().hasRightHorn();
    }

    @Override
    public void setRightHorn(boolean hasHorn) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().getEntityData().set(net.minecraft.world.entity.animal.goat.Goat.DATA_HAS_RIGHT_HORN, hasHorn);
            return null;
        });
    }

    @Override
    public boolean isScreaming() {
        return this.getHandle().isScreamingGoat();
    }

    @Override
    public void setScreaming(boolean screaming) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setScreamingGoat(screaming);
            return null;
        });
    }

    // Paper start - Goat ram API
    @Override
    public void ram(@org.jetbrains.annotations.NotNull org.bukkit.entity.LivingEntity entity) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().ram(((CraftLivingEntity) entity).getHandle());
            return null;
        });
    }
    // Paper end
}

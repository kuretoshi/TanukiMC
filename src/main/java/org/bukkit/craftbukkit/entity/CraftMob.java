package org.bukkit.craftbukkit.entity;

import com.google.common.base.Preconditions;
import com.mohistmc.youer.threading.ThreadedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.CraftLootTable;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftSound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.loot.LootTable;

public abstract class CraftMob extends CraftLivingEntity implements Mob, io.papermc.paper.entity.PaperLeashable { // Paper - Leashable API
    public CraftMob(CraftServer server, net.minecraft.world.entity.Mob entity) {
        super(server, entity);
    }

    @Override
    public void setTarget(LivingEntity target) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            Preconditions.checkState(!this.getHandle().generation, "Cannot set target during world generation");

            net.minecraft.world.entity.Mob entity = this.getHandle();
            if (target == null) {
                entity.setTargetReason(null, false);
                entity.setTarget(null);
            } else if (target instanceof CraftLivingEntity) {
                entity.setTargetReason(null, false);
                entity.setTarget(((CraftLivingEntity) target).getHandle());
            }
            return null;
        });
    }

    @Override
    public CraftLivingEntity getTarget() {
        if (this.getHandle().getTarget() == null) return null;

        return (CraftLivingEntity) this.getHandle().getTarget().getBukkitEntity();
    }

    @Override
    public void setAware(boolean aware) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().aware = aware;
            return null;
        });
    }

    @Override
    public boolean isAware() {
        return this.getHandle().aware;
    }

    @Override
    public Sound getAmbientSound() {
        SoundEvent sound = this.getHandle().getAmbientSound0();
        return (sound != null) ? CraftSound.minecraftToBukkit(sound) : null;
    }

    @Override
    public net.minecraft.world.entity.Mob getHandle() {
        return (net.minecraft.world.entity.Mob) this.entity;
    }

    @Override
    public String toString() {
        return "CraftMob";
    }

    @Override
    public void setLootTable(LootTable table) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().lootTable = CraftLootTable.bukkitToMinecraft(table);
            return null;
        });
    }

    @Override
    public LootTable getLootTable() {
        return CraftLootTable.minecraftToBukkit(this.getHandle().getLootTable());
    }

    @Override
    public void setSeed(long seed) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().lootTableSeed = seed;
            return null;
        });
    }

    @Override
    public long getSeed() {
        return this.getHandle().lootTableSeed;
    }

    // Paper start
    @Override
    public boolean isInDaylight() {
        return getHandle().isSunBurnTick();
    }

    @Override
    public void lookAt(@org.jetbrains.annotations.NotNull org.bukkit.Location location) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            com.google.common.base.Preconditions.checkNotNull(location, "location cannot be null");
            com.google.common.base.Preconditions.checkArgument(location.getWorld().equals(getWorld()), "location in a different world");
            getHandle().getLookControl().setLookAt(location.getX(), location.getY(), location.getZ());
            return null;
        });
    }

    @Override
    public void lookAt(@org.jetbrains.annotations.NotNull org.bukkit.Location location, float headRotationSpeed, float maxHeadPitch) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            com.google.common.base.Preconditions.checkNotNull(location, "location cannot be null");
            com.google.common.base.Preconditions.checkArgument(location.getWorld().equals(getWorld()), "location in a different world");
            getHandle().getLookControl().setLookAt(location.getX(), location.getY(), location.getZ(), headRotationSpeed, maxHeadPitch);
            return null;
        });
    }

    @Override
    public void lookAt(@org.jetbrains.annotations.NotNull org.bukkit.entity.Entity entity) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            com.google.common.base.Preconditions.checkNotNull(entity, "entity cannot be null");
            com.google.common.base.Preconditions.checkArgument(entity.getWorld().equals(getWorld()), "entity in a different world");
            getHandle().getLookControl().setLookAt(((CraftEntity) entity).getHandle());
            return null;
        });
    }

    @Override
    public void lookAt(@org.jetbrains.annotations.NotNull org.bukkit.entity.Entity entity, float headRotationSpeed, float maxHeadPitch) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            com.google.common.base.Preconditions.checkNotNull(entity, "entity cannot be null");
            com.google.common.base.Preconditions.checkArgument(entity.getWorld().equals(getWorld()), "entity in a different world");
            getHandle().getLookControl().setLookAt(((CraftEntity) entity).getHandle(), headRotationSpeed, maxHeadPitch);
            return null;
        });
    }

    @Override
    public void lookAt(double x, double y, double z) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            getHandle().getLookControl().setLookAt(x, y, z);
            return null;
        });
    }

    @Override
    public void lookAt(double x, double y, double z, float headRotationSpeed, float maxHeadPitch) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            getHandle().getLookControl().setLookAt(x, y, z, headRotationSpeed, maxHeadPitch);
            return null;
        });
    }

    @Override
    public int getHeadRotationSpeed() {
        return getHandle().getHeadRotSpeed();
    }

    @Override
    public int getMaxHeadPitch() {
        return getHandle().getMaxHeadXRot();
    }

    @Override
    public boolean isLeftHanded() {
        return getHandle().isLeftHanded();
    }

    @Override
    public void setLeftHanded(boolean leftHanded) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            getHandle().setLeftHanded(leftHanded);
            return null;
        });
    }
    // Paper end

    // Paper start
    @Override
    public boolean isAggressive() {
        return this.getHandle().isAggressive();
    }

    @Override
    public void setAggressive(boolean aggressive) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setAggressive(aggressive);
            return null;
        });
    }
    // Paper end

    // Paper start
    @Override
    public int getPossibleExperienceReward() {
        return getHandle().getExperienceReward((ServerLevel) this.getHandle().level(), null);
    }
    // Paper end

    // Paper start - Leashable API
    @Override
    public boolean isLeashed() {
        return io.papermc.paper.entity.PaperLeashable.super.isLeashed();
    }

    @Override
    public org.bukkit.entity.Entity getLeashHolder() throws IllegalStateException {
        return io.papermc.paper.entity.PaperLeashable.super.getLeashHolder();
    }

    @Override
    public boolean setLeashHolder(final org.bukkit.entity.Entity holder) {
        return ThreadedServer.onMainThreadIfRegionThread(() -> io.papermc.paper.entity.PaperLeashable.super.setLeashHolder(holder));
    }
    // Paper end - Leashable API
}

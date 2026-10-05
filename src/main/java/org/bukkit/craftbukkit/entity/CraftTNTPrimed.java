package org.bukkit.craftbukkit.entity;

import com.mohistmc.youer.threading.ThreadedServer;
import net.minecraft.world.entity.item.PrimedTnt;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.TNTPrimed;

public class CraftTNTPrimed extends CraftEntity implements TNTPrimed {

    public CraftTNTPrimed(CraftServer server, PrimedTnt entity) {
        super(server, entity);
    }

    @Override
    public float getYield() {
        return this.getHandle().yield;
    }

    @Override
    public boolean isIncendiary() {
        return this.getHandle().isIncendiary;
    }

    @Override
    public void setIsIncendiary(boolean isIncendiary) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().isIncendiary = isIncendiary;
            return null;
        });
    }

    @Override
    public void setYield(float yield) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().yield = yield;
            return null;
        });
    }

    @Override
    public int getFuseTicks() {
        return this.getHandle().getFuse();
    }

    @Override
    public void setFuseTicks(int fuseTicks) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setFuse(fuseTicks);
            return null;
        });
    }

    @Override
    public PrimedTnt getHandle() {
        return (PrimedTnt) this.entity;
    }

    @Override
    public String toString() {
        return "CraftTNTPrimed";
    }

    @Override
    public Entity getSource() {
        net.minecraft.world.entity.LivingEntity source = this.getHandle().getOwner();

        return (source != null) ? source.getBukkitEntity() : null;
    }

    @Override
    public void setSource(Entity source) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            if (source instanceof LivingEntity) {
                this.getHandle().owner = ((CraftLivingEntity) source).getHandle();
            } else {
                this.getHandle().owner = null;
            }
            return null;
        });
    }

    // Paper start
    @Override
    public void setBlockData(org.bukkit.block.data.BlockData data) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            com.google.common.base.Preconditions.checkArgument(data != null, "The visual block data of this tnt cannot be null. To reset it just set to the TNT default block data");
            this.getHandle().setBlockState(((org.bukkit.craftbukkit.block.data.CraftBlockData) data).getState());
            return null;
        });
    }

    @Override
    public org.bukkit.block.data.BlockData getBlockData() {
        return org.bukkit.craftbukkit.block.data.CraftBlockData.fromData(this.getHandle().getBlockState());
    }
    // Paper end
}

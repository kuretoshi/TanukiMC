package org.bukkit.craftbukkit.entity;

import com.mohistmc.youer.threading.ThreadedServer;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.bukkit.entity.Minecart;
import org.bukkit.material.MaterialData;
import org.bukkit.util.Vector;

public abstract class CraftMinecart extends CraftVehicle implements Minecart {
    public CraftMinecart(CraftServer server, AbstractMinecart entity) {
        super(server, entity);
    }

    @Override
    public void setDamage(double damage) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setDamage((float) damage);
            return null;
        });
    }

    @Override
    public double getDamage() {
        return this.getHandle().getDamage();
    }

    @Override
    public double getMaxSpeed() {
        return this.getHandle().maxSpeed;
    }

    @Override
    public void setMaxSpeed(double speed) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            if (speed >= 0D) {
                this.getHandle().maxSpeed = speed;
            }
            return null;
        });
    }

    @Override
    public boolean isSlowWhenEmpty() {
        return this.getHandle().slowWhenEmpty;
    }

    @Override
    public void setSlowWhenEmpty(boolean slow) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().slowWhenEmpty = slow;
            return null;
        });
    }

    @Override
    public Vector getFlyingVelocityMod() {
        return this.getHandle().getFlyingVelocityMod();
    }

    @Override
    public void setFlyingVelocityMod(Vector flying) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setFlyingVelocityMod(flying);
            return null;
        });
    }

    @Override
    public Vector getDerailedVelocityMod() {
        return this.getHandle().getDerailedVelocityMod();
    }

    @Override
    public void setDerailedVelocityMod(Vector derailed) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setDerailedVelocityMod(derailed);
            return null;
        });
    }

    // Paper start
    @Override
    public Material getMinecartMaterial() {
        net.minecraft.world.item.Item minecartItem = switch (getHandle().getMinecartType()) {
            case CHEST -> Items.CHEST_MINECART;
            case FURNACE ->  Items.FURNACE_MINECART;
            case TNT ->  Items.TNT_MINECART;
            case HOPPER ->  Items.HOPPER_MINECART;
            case COMMAND_BLOCK ->  Items.COMMAND_BLOCK_MINECART;
            case RIDEABLE, SPAWNER ->  Items.MINECART;
        };

        return CraftMagicNumbers.getMaterial(minecartItem);
    }
    // Paper end

    @Override
    public AbstractMinecart getHandle() {
        return (AbstractMinecart) this.entity;
    }

    @Override
    public void setDisplayBlock(MaterialData material) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            if (material != null) {
                BlockState block = CraftMagicNumbers.getBlock(material);
                this.getHandle().setDisplayBlockState(block);
            } else {
                // Set block to air (default) and set the flag to not have a display block.
                this.getHandle().setDisplayBlockState(Blocks.AIR.defaultBlockState());
                this.getHandle().setCustomDisplay(false);
            }
            return null;
        });
    }

    @Override
    public void setDisplayBlockData(BlockData blockData) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            if (blockData != null) {
                BlockState block = ((CraftBlockData) blockData).getState();
                this.getHandle().setDisplayBlockState(block);
            } else {
                // Set block to air (default) and set the flag to not have a display block.
                this.getHandle().setDisplayBlockState(Blocks.AIR.defaultBlockState());
                this.getHandle().setCustomDisplay(false);
            }
            return null;
        });
    }

    @Override
    public MaterialData getDisplayBlock() {
        BlockState blockData = this.getHandle().getDisplayBlockState();
        return CraftMagicNumbers.getMaterial(blockData);
    }

    @Override
    public BlockData getDisplayBlockData() {
        BlockState blockData = this.getHandle().getDisplayBlockState();
        return CraftBlockData.fromData(blockData);
    }

    @Override
    public void setDisplayBlockOffset(int offset) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setDisplayOffset(offset);
            return null;
        });
    }

    @Override
    public int getDisplayBlockOffset() {
        return this.getHandle().getDisplayOffset();
    }
}

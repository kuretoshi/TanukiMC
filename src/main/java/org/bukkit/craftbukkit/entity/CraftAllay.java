package org.bukkit.craftbukkit.entity;

import com.google.common.base.Preconditions;
import com.mohistmc.youer.threading.ThreadedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.allay.Allay;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.inventory.CraftInventory;
import org.bukkit.craftbukkit.util.CraftLocation;
import org.bukkit.inventory.Inventory;

public class CraftAllay extends CraftCreature implements org.bukkit.entity.Allay {

    public CraftAllay(CraftServer server, Allay entity) {
        super(server, entity);
    }

    @Override
    public Allay getHandle() {
        return (Allay) this.entity;
    }

    @Override
    public String toString() {
        return "CraftAllay";
    }

    @Override
    public Inventory getInventory() {
        return new CraftInventory(this.getHandle().getInventory());
    }

    @Override
    public boolean canDuplicate() {
        return this.getHandle().canDuplicate();
    }

    @Override
    public void setCanDuplicate(boolean canDuplicate) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setCanDuplicate(canDuplicate);
            return null;
        });
    }

    @Override
    public long getDuplicationCooldown() {
        return this.getHandle().duplicationCooldown;
    }

    @Override
    public void setDuplicationCooldown(long l) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().duplicationCooldown = l;
            return null;
        });
    }

    @Override
    public void resetDuplicationCooldown() {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().resetDuplicationCooldown();
            return null;
        });
    }

    @Override
    public boolean isDancing() {
        return this.getHandle().isDancing();
    }

    @Override
    public void startDancing(Location location) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            Preconditions.checkArgument(location != null, "Location cannot be null");
            Preconditions.checkArgument(location.getBlock().getType().equals(Material.JUKEBOX), "The Block in the Location need to be a JukeBox");
            this.getHandle().setJukeboxPlaying(CraftLocation.toBlockPosition(location), true);
            return null;
        });
    }

    @Override
    public void startDancing() {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().forceDancing = true;
            this.getHandle().setDancing(true);
            return null;
        });
    }

    @Override
    public void stopDancing() {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().forceDancing = false;
            this.getHandle().jukeboxPos = null;
            this.getHandle().setDancing(false); // Paper - Directly modify set dancing to avoid NPE
            return null;
        });
    }

    @Override
    public org.bukkit.entity.Allay duplicateAllay() {
        return ThreadedServer.onMainThreadIfRegionThread(() -> {
            Allay nmsAllay = this.getHandle().duplicateAllay0();
            return (nmsAllay != null) ? (org.bukkit.entity.Allay) nmsAllay.getBukkitEntity() : null;
        });
    }

    public Location getJukebox() {
        BlockPos nmsJukeboxPos = this.getHandle().jukeboxPos;
        return (nmsJukeboxPos != null) ? CraftLocation.toBukkit(nmsJukeboxPos, this.getWorld()) : null;
    }
}

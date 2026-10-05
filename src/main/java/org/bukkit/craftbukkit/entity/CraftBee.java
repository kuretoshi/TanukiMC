package org.bukkit.craftbukkit.entity;

import com.google.common.base.Preconditions;
import com.mohistmc.youer.threading.ThreadedServer;
import net.minecraft.core.BlockPos;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.util.CraftLocation;
import org.bukkit.entity.Bee;

public class CraftBee extends CraftAnimals implements Bee {

    public CraftBee(CraftServer server, net.minecraft.world.entity.animal.Bee entity) {
        super(server, entity);
    }

    @Override
    public net.minecraft.world.entity.animal.Bee getHandle() {
        return (net.minecraft.world.entity.animal.Bee) this.entity;
    }

    @Override
    public String toString() {
        return "CraftBee";
    }

    @Override
    public Location getHive() {
        BlockPos hive = this.getHandle().getHivePos();
        return (hive == null) ? null : CraftLocation.toBukkit(hive, this.getWorld());
    }

    @Override
    public void setHive(Location location) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            Preconditions.checkArgument(location == null || this.getWorld().equals(location.getWorld()), "Hive must be in same world");
            this.getHandle().hivePos = (location == null) ? null : CraftLocation.toBlockPosition(location);
            return null;
        });
    }

    @Override
    public Location getFlower() {
        BlockPos flower = this.getHandle().getSavedFlowerPos();
        return (flower == null) ? null : CraftLocation.toBukkit(flower, this.getWorld());
    }

    @Override
    public void setFlower(Location location) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            Preconditions.checkArgument(location == null || this.getWorld().equals(location.getWorld()), "Flower must be in same world");
            this.getHandle().setSavedFlowerPos(location == null ? null : CraftLocation.toBlockPosition(location));
            return null;
        });
    }

    @Override
    public boolean hasNectar() {
        return this.getHandle().hasNectar();
    }

    @Override
    public void setHasNectar(boolean nectar) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setHasNectar(nectar);
            return null;
        });
    }

    @Override
    public boolean hasStung() {
        return this.getHandle().hasStung();
    }

    @Override
    public void setHasStung(boolean stung) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setHasStung(stung);
            return null;
        });
    }

    @Override
    public int getAnger() {
        return this.getHandle().getRemainingPersistentAngerTime();
    }

    @Override
    public void setAnger(int anger) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setRemainingPersistentAngerTime(anger);
            return null;
        });
    }

    @Override
    public int getCannotEnterHiveTicks() {
        return this.getHandle().stayOutOfHiveCountdown;
    }

    @Override
    public void setCannotEnterHiveTicks(int ticks) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setStayOutOfHiveCountdown(ticks);
            return null;
        });
    }
    // Paper start
    @Override
    public void setRollingOverride(net.kyori.adventure.util.TriState rolling) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().rollingOverride = rolling;

            this.getHandle().setRolling(this.getHandle().isRolling()); // Refresh rolling state
            return null;
        });
    }

    @Override
    public boolean isRolling() {
        return this.getRollingOverride().toBooleanOrElse(this.getHandle().isRolling());
    }

    @Override
    public net.kyori.adventure.util.TriState getRollingOverride() {
        return this.getHandle().rollingOverride;
    }

    @Override
    public void setCropsGrownSincePollination(int crops) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().numCropsGrownSincePollination = crops;
            return null;
        });
    }

    @Override
    public int getCropsGrownSincePollination() {
        return this.getHandle().numCropsGrownSincePollination;
    }

    @Override
    public void setTicksSincePollination(int ticks) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().ticksWithoutNectarSinceExitingHive = ticks;
            return null;
        });
    }

    @Override
    public int getTicksSincePollination() {
        return this.getHandle().ticksWithoutNectarSinceExitingHive;
    }
    // Paper end
}

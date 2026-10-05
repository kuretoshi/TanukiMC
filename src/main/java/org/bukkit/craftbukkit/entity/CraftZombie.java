package org.bukkit.craftbukkit.entity;

import com.google.common.base.Preconditions;
import com.mohistmc.youer.threading.ThreadedServer;
import net.minecraft.world.entity.monster.ZombieVillager;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Zombie;

public class CraftZombie extends CraftMonster implements Zombie {

    public CraftZombie(CraftServer server, net.minecraft.world.entity.monster.Zombie entity) {
        super(server, entity);
    }

    @Override
    public net.minecraft.world.entity.monster.Zombie getHandle() {
        return (net.minecraft.world.entity.monster.Zombie) this.entity;
    }

    @Override
    public String toString() {
        return "CraftZombie";
    }

    @Override
    public boolean isBaby() {
        return this.getHandle().isBaby();
    }

    @Override
    public void setBaby(boolean flag) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setBaby(flag);
            return null;
        });
    }

    @Override
    public boolean isVillager() {
        return this.getHandle() instanceof ZombieVillager;
    }

    @Override
    public void setVillager(boolean flag) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public void setVillagerProfession(Villager.Profession profession) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public Villager.Profession getVillagerProfession() {
        return null;
    }

    @Override
    public boolean isConverting() {
        return this.getHandle().isUnderWaterConverting();
    }

    @Override
    public int getConversionTime() {
        Preconditions.checkState(this.isConverting(), "Entity not converting");

        return this.getHandle().conversionTime;
    }

    @Override
    public void setConversionTime(int time) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            if (time < 0) {
                this.getHandle().conversionTime = -1;
                this.getHandle().getEntityData().set(net.minecraft.world.entity.monster.Zombie.DATA_DROWNED_CONVERSION_ID, false);
            } else {
                this.getHandle().startUnderWaterConversion(time);
            }
            return null;
        });
    }

    @Override
    public int getAge() {
        return this.getHandle().isBaby() ? -1 : 0;
    }

    @Override
    public void setAge(int i) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setBaby(i < 0);
            return null;
        });
    }

    @Override
    public void setAgeLock(boolean b) {
    }

    // Paper start
    @Override
    public boolean isDrowning() {
        return getHandle().isUnderWaterConverting();
    }

    @Override
    public void startDrowning(int drownedConversionTime) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            getHandle().startUnderWaterConversion(drownedConversionTime);
            return null;
        });
    }

    @Override
    public void stopDrowning() {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            getHandle().stopDrowning();
            return null;
        });
    }

    @Override
    public boolean shouldBurnInDay() {
        return getHandle().isSunSensitive();
    }

    @Override
    public boolean isArmsRaised() {
        return getHandle().isAggressive();
    }

    @Override
    public void setArmsRaised(final boolean raised) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            getHandle().setAggressive(raised);
            return null;
        });
    }

    @Override
    public void setShouldBurnInDay(boolean shouldBurnInDay) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            getHandle().setShouldBurnInDay(shouldBurnInDay);
            return null;
        });
    }

    @Override
    public boolean supportsBreakingDoors() {
        return getHandle().supportsBreakDoorGoal();
    }
    // Paper end

    @Override
    public boolean getAgeLock() {
        return false;
    }

    @Override
    public void setBaby() {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setBaby(true);
            return null;
        });
    }

    @Override
    public void setAdult() {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setBaby(false);
            return null;
        });
    }

    @Override
    public boolean isAdult() {
        return !this.getHandle().isBaby();
    }

    @Override
    public boolean canBreed() {
        return false;
    }

    @Override
    public void setBreed(boolean b) {
    }

    @Override
    public boolean canBreakDoors() {
        return this.getHandle().canBreakDoors();
    }

    @Override
    public void setCanBreakDoors(boolean flag) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setCanBreakDoors(flag);
            return null;
        });
    }
}

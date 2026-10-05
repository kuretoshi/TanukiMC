package org.bukkit.craftbukkit.entity;

import com.mohistmc.youer.threading.ThreadedServer;
import net.minecraft.world.entity.AgeableMob;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Ageable;

public class CraftAgeable extends CraftCreature implements Ageable {
    public CraftAgeable(CraftServer server, AgeableMob entity) {
        super(server, entity);
    }

    @Override
    public int getAge() {
        return this.getHandle().getAge();
    }

    @Override
    public void setAge(int age) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setAge(age);
            return null;
        });
    }

    @Override
    public void setAgeLock(boolean lock) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().ageLocked = lock;
            return null;
        });
    }

    @Override
    public boolean getAgeLock() {
        return this.getHandle().ageLocked;
    }

    @Override
    public void setBaby() {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            if (this.isAdult()) {
                this.setAge(-24000);
            }
            return null;
        });
    }

    @Override
    public void setAdult() {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            if (!this.isAdult()) {
                this.setAge(0);
            }
            return null;
        });
    }

    @Override
    public boolean isAdult() {
        return this.getAge() >= 0;
    }


    @Override
    public boolean canBreed() {
        return this.getAge() == 0;
    }

    @Override
    public void setBreed(boolean breed) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            if (breed) {
                this.setAge(0);
            } else if (this.isAdult()) {
                this.setAge(6000);
            }
            return null;
        });
    }

    @Override
    public AgeableMob getHandle() {
        return (AgeableMob) this.entity;
    }

    @Override
    public String toString() {
        return "CraftAgeable";
    }
}

package org.bukkit.craftbukkit.entity;

import com.mohistmc.youer.threading.ThreadedServer;
import net.minecraft.world.entity.animal.frog.Tadpole;
import org.bukkit.craftbukkit.CraftServer;

public class CraftTadpole extends CraftFish implements org.bukkit.entity.Tadpole {

    public CraftTadpole(CraftServer server, Tadpole entity) {
        super(server, entity);
    }

    @Override
    public Tadpole getHandle() {
        return (Tadpole) this.entity;
    }

    @Override
    public String toString() {
        return "CraftTadpole";
    }

    @Override
    public int getAge() {
        return this.getHandle().age;
    }

    @Override
    public void setAge(int age) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().age = age;
            return null;
        });
    }
    // Paper start
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
    // Paper end
}

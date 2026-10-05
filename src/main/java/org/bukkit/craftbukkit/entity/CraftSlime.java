package org.bukkit.craftbukkit.entity;

import com.mohistmc.youer.threading.ThreadedServer;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Slime;

public class CraftSlime extends CraftMob implements Slime, CraftEnemy {

    public CraftSlime(CraftServer server, net.minecraft.world.entity.monster.Slime entity) {
        super(server, entity);
    }

    @Override
    public int getSize() {
        return this.getHandle().getSize();
    }

    @Override
    public void setSize(int size) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setSize(size, getHandle().isAlive()); // Paper - fix dead slime setSize invincibility
            return null;
        });
    }

    @Override
    public net.minecraft.world.entity.monster.Slime getHandle() {
        return (net.minecraft.world.entity.monster.Slime) this.entity;
    }

    @Override
    public String toString() {
        return "CraftSlime";
    }

    // Paper start
    @Override
    public boolean canWander() {
        return getHandle().canWander();
    }

    @Override
    public void setWander(boolean canWander) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            getHandle().setWander(canWander);
            return null;
        });
    }
    // Paper end
}

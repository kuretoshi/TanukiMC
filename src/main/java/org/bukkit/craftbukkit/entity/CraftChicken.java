package org.bukkit.craftbukkit.entity;

import com.mohistmc.youer.threading.ThreadedServer;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Chicken;

public class CraftChicken extends CraftAnimals implements Chicken {

    public CraftChicken(CraftServer server, net.minecraft.world.entity.animal.Chicken entity) {
        super(server, entity);
    }

    @Override
    public net.minecraft.world.entity.animal.Chicken getHandle() {
        return (net.minecraft.world.entity.animal.Chicken) this.entity;
    }

    @Override
    public String toString() {
        return "CraftChicken";
    }

    // Paper start
    @Override
    public boolean isChickenJockey() {
        return this.getHandle().isChickenJockey();
    }

    @Override
    public void setIsChickenJockey(boolean isChickenJockey) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setChickenJockey(isChickenJockey);
            return null;
        });
    }

    @Override
    public int getEggLayTime() {
        return this.getHandle().eggTime;
    }

    @Override
    public void setEggLayTime(int eggLayTime) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().eggTime = eggLayTime;
            return null;
        });
    }
    // Paper end
}

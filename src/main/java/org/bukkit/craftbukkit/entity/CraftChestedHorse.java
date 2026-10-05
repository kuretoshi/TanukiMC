package org.bukkit.craftbukkit.entity;

import com.mohistmc.youer.threading.ThreadedServer;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.ChestedHorse;

public abstract class CraftChestedHorse extends CraftAbstractHorse implements ChestedHorse {

    public CraftChestedHorse(CraftServer server, AbstractChestedHorse entity) {
        super(server, entity);
    }

    @Override
    public AbstractChestedHorse getHandle() {
        return (AbstractChestedHorse) super.getHandle();
    }

    @Override
    public boolean isCarryingChest() {
        return this.getHandle().hasChest();
    }

    @Override
    public void setCarryingChest(boolean chest) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            if (chest == this.isCarryingChest()) return null;
            this.getHandle().setChest(chest);
            this.getHandle().createInventory();
            return null;
        });
    }
}

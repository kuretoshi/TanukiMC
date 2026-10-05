package org.bukkit.craftbukkit.entity;

import com.mohistmc.youer.threading.ThreadedServer;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Ocelot;

public class CraftOcelot extends CraftAnimals implements Ocelot {
    public CraftOcelot(CraftServer server, net.minecraft.world.entity.animal.Ocelot ocelot) {
        super(server, ocelot);
    }

    @Override
    public net.minecraft.world.entity.animal.Ocelot getHandle() {
        return (net.minecraft.world.entity.animal.Ocelot) this.entity;
    }

    @Override
    public boolean isTrusting() {
        return this.getHandle().isTrusting();
    }

    @Override
    public void setTrusting(boolean trust) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setTrusting(trust);
            return null;
        });
    }

    @Override
    public Type getCatType() {
        return Type.WILD_OCELOT;
    }

    @Override
    public void setCatType(Type type) {
        throw new UnsupportedOperationException("Cats are now a different entity!");
    }

    @Override
    public String toString() {
        return "CraftOcelot";
    }
}

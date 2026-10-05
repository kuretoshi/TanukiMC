package org.bukkit.craftbukkit.entity;

import com.mohistmc.youer.threading.ThreadedServer;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Rabbit;

public class CraftRabbit extends CraftAnimals implements Rabbit {

    public CraftRabbit(CraftServer server, net.minecraft.world.entity.animal.Rabbit entity) {
        super(server, entity);
    }

    @Override
    public net.minecraft.world.entity.animal.Rabbit getHandle() {
        return (net.minecraft.world.entity.animal.Rabbit) this.entity;
    }

    @Override
    public String toString() {
        return "CraftRabbit{RabbitType=" + this.getRabbitType() + "}";
    }

    @Override
    public Type getRabbitType() {
        return Type.values()[this.getHandle().getVariant().ordinal()];
    }

    @Override
    public void setRabbitType(Type type) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setVariant(net.minecraft.world.entity.animal.Rabbit.Variant.values()[type.ordinal()]);
            return null;
        });
    }
    // Paper start
    @Override
    public void setMoreCarrotTicks(int ticks) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().moreCarrotTicks = ticks;
            return null;
        });
    }

    @Override
    public int getMoreCarrotTicks() {
        return this.getHandle().moreCarrotTicks;
    }
    // Paper end
}

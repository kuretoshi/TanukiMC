package org.bukkit.craftbukkit.entity;

import com.mohistmc.youer.threading.ThreadedServer;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Ravager;

public class CraftRavager extends CraftRaider implements Ravager {

    public CraftRavager(CraftServer server, net.minecraft.world.entity.monster.Ravager entity) {
        super(server, entity);
    }

    @Override
    public net.minecraft.world.entity.monster.Ravager getHandle() {
        return (net.minecraft.world.entity.monster.Ravager) super.getHandle();
    }

    @Override
    public String toString() {
        return "CraftRavager";
    }
    // Paper start - Missing Entity Behavior
    @Override
    public int getAttackTicks() {
        return this.getHandle().getAttackTick();
    }

    @Override
    public void setAttackTicks(int ticks) {
        ThreadedServer.withWorldMutationLock(() -> {
            this.getHandle().attackTick = ticks;
            return null;
        });
    }

    @Override
    public int getStunnedTicks() {
        return this.getHandle().getStunnedTick();
    }

    @Override
    public void setStunnedTicks(int ticks) {
        ThreadedServer.withWorldMutationLock(() -> {
            this.getHandle().stunnedTick = ticks;
            return null;
        });
    }

    @Override
    public int getRoarTicks() {
        return this.getHandle().getRoarTick();
    }

    @Override
    public void setRoarTicks(int ticks) {
        ThreadedServer.withWorldMutationLock(() -> {
            this.getHandle().roarTick = ticks;
            return null;
        });
    }
    // Paper end
}

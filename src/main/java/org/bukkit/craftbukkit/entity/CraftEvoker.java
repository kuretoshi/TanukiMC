package org.bukkit.craftbukkit.entity;

import com.mohistmc.youer.threading.ThreadedServer;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.monster.SpellcasterIllager;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Evoker;

public class CraftEvoker extends CraftSpellcaster implements Evoker {

    public CraftEvoker(CraftServer server, net.minecraft.world.entity.monster.Evoker entity) {
        super(server, entity);
    }

    @Override
    public net.minecraft.world.entity.monster.Evoker getHandle() {
        return (net.minecraft.world.entity.monster.Evoker) super.getHandle();
    }

    @Override
    public String toString() {
        return "CraftEvoker";
    }

    @Override
    public Evoker.Spell getCurrentSpell() {
        return Evoker.Spell.values()[this.getHandle().getCurrentSpell().ordinal()];
    }

    @Override
    public void setCurrentSpell(Evoker.Spell spell) {
        ThreadedServer.withWorldMutationLock(() -> {
            this.getHandle().setIsCastingSpell(spell == null ? SpellcasterIllager.IllagerSpell.NONE : SpellcasterIllager.IllagerSpell.byId(spell.ordinal()));
            return null;
        });
    }

    // Paper start - Add more Evoker API
    @Override
    public org.bukkit.entity.Sheep getWololoTarget() {
        Sheep sheep = getHandle().getWololoTarget();
        return sheep == null ? null : (org.bukkit.entity.Sheep) sheep.getBukkitEntity();
    }

    @Override
    public void setWololoTarget(org.bukkit.entity.Sheep sheep) {
        ThreadedServer.withWorldMutationLock(() -> {
            getHandle().setWololoTarget(sheep == null ? null : ((org.bukkit.craftbukkit.entity.CraftSheep) sheep).getHandle());
            return null;
        });
    }
    // Paper end - Add more Evoker API
}

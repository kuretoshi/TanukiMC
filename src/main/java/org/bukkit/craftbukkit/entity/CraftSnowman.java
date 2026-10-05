package org.bukkit.craftbukkit.entity;

import com.destroystokyo.paper.entity.CraftRangedEntity;
import com.mohistmc.youer.threading.ThreadedServer;
import net.minecraft.world.entity.animal.SnowGolem;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Snowman;

public class CraftSnowman extends CraftGolem implements Snowman, CraftRangedEntity<SnowGolem>, io.papermc.paper.entity.PaperShearable { // Paper
    public CraftSnowman(CraftServer server, SnowGolem entity) {
        super(server, entity);
    }

    @Override
    public boolean isDerp() {
        return !this.getHandle().hasPumpkin();
    }

    @Override
    public void setDerp(boolean derpMode) {
        ThreadedServer.withWorldMutationLock(() -> {
            this.getHandle().setPumpkin(!derpMode);
            return null;
        });
    }

    @Override
    public SnowGolem getHandle() {
        return (SnowGolem) this.entity;
    }

    @Override
    public String toString() {
        return "CraftSnowman";
    }

    // Purpur start
    @Override
    @org.jetbrains.annotations.Nullable
    public java.util.UUID getSummoner() {
        return getHandle().getSummoner();
    }

    @Override
    public void setSummoner(@org.jetbrains.annotations.Nullable java.util.UUID summoner) {
        ThreadedServer.withWorldMutationLock(() -> {
            getHandle().setSummoner(summoner);
            return null;
        });
    }
    // Purpur end
}

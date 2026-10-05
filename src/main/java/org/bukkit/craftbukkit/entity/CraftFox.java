package org.bukkit.craftbukkit.entity;

import com.google.common.base.Preconditions;
import com.mohistmc.youer.threading.ThreadedServer;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Fox;

public class CraftFox extends CraftAnimals implements Fox {

    public CraftFox(CraftServer server, net.minecraft.world.entity.animal.Fox entity) {
        super(server, entity);
    }

    @Override
    public net.minecraft.world.entity.animal.Fox getHandle() {
        return (net.minecraft.world.entity.animal.Fox) super.getHandle();
    }

    @Override
    public String toString() {
        return "CraftFox";
    }

    @Override
    public Type getFoxType() {
        return Type.values()[this.getHandle().getVariant().ordinal()];
    }

    @Override
    public void setFoxType(Type type) {
        Preconditions.checkArgument(type != null, "type");

        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setVariant(net.minecraft.world.entity.animal.Fox.Type.values()[type.ordinal()]);
            return null;
        });
    }

    @Override
    public boolean isCrouching() {
        return this.getHandle().isCrouching();
    }

    @Override
    public void setCrouching(boolean crouching) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setIsCrouching(crouching);
            return null;
        });
    }

    @Override
    public boolean isSitting() {
        return this.getHandle().isSitting();
    }

    @Override
    public void setSitting(boolean sitting) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setSitting(sitting);
            return null;
        });
    }

    @Override
    public void setSleeping(boolean sleeping) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setSleeping(sleeping);
            return null;
        });
    }

    @Override
    public AnimalTamer getFirstTrustedPlayer() {
        UUID uuid = this.getHandle().getEntityData().get(net.minecraft.world.entity.animal.Fox.DATA_TRUSTED_ID_0).orElse(null);
        if (uuid == null) {
            return null;
        }

        AnimalTamer player = this.getServer().getPlayer(uuid);
        if (player == null) {
            player = this.getServer().getOfflinePlayer(uuid);
        }

        return player;
    }

    @Override
    public void setFirstTrustedPlayer(AnimalTamer player) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            if (player == null) {
                Preconditions.checkState(this.getHandle().getEntityData().get(net.minecraft.world.entity.animal.Fox.DATA_TRUSTED_ID_1).isEmpty(), "Must remove second trusted player first");
            }

            this.getHandle().getEntityData().set(net.minecraft.world.entity.animal.Fox.DATA_TRUSTED_ID_0, player == null ? Optional.empty() : Optional.of(player.getUniqueId()));
            return null;
        });
    }

    @Override
    public AnimalTamer getSecondTrustedPlayer() {
        UUID uuid = this.getHandle().getEntityData().get(net.minecraft.world.entity.animal.Fox.DATA_TRUSTED_ID_1).orElse(null);
        if (uuid == null) {
            return null;
        }

        AnimalTamer player = this.getServer().getPlayer(uuid);
        if (player == null) {
            player = this.getServer().getOfflinePlayer(uuid);
        }

        return player;
    }

    @Override
    public void setSecondTrustedPlayer(AnimalTamer player) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            if (player != null) {
                Preconditions.checkState(this.getHandle().getEntityData().get(net.minecraft.world.entity.animal.Fox.DATA_TRUSTED_ID_0).isPresent(), "Must add first trusted player first");
            }

            this.getHandle().getEntityData().set(net.minecraft.world.entity.animal.Fox.DATA_TRUSTED_ID_1, player == null ? Optional.empty() : Optional.of(player.getUniqueId()));
            return null;
        });
    }

    @Override
    public boolean isFaceplanted() {
        return this.getHandle().isFaceplanted();
    }

    // Paper start - Add more fox behavior API
    @Override
    public void setInterested(boolean interested) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setIsInterested(interested);
            return null;
        });
    }

    @Override
    public boolean isInterested() {
        return this.getHandle().isInterested();
    }

    @Override
    public void setLeaping(boolean leaping) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setIsPouncing(leaping);
            return null;
        });
    }

    @Override
    public boolean isLeaping() {
        return this.getHandle().isPouncing();
    }

    @Override
    public void setDefending(boolean defending) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setDefending(defending);
            return null;
        });
    }

    @Override
    public boolean isDefending() {
        return this.getHandle().isDefending();
    }

    @Override
    public void setFaceplanted(boolean faceplanted) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setFaceplanted(faceplanted);
            return null;
        });
    }
    // Paper end - Add more fox behavior API
}

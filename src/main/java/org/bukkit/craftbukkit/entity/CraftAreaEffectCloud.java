package org.bukkit.craftbukkit.entity;

import com.google.common.collect.ImmutableList;
import com.mohistmc.youer.threading.ThreadedServer;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.PotionContents;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.craftbukkit.CraftParticle;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.potion.CraftPotionEffectType;
import org.bukkit.craftbukkit.potion.CraftPotionType;
import org.bukkit.craftbukkit.potion.CraftPotionUtil;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.projectiles.ProjectileSource;

public class CraftAreaEffectCloud extends CraftEntity implements AreaEffectCloud {

    public CraftAreaEffectCloud(CraftServer server, net.minecraft.world.entity.AreaEffectCloud entity) {
        super(server, entity);
    }

    @Override
    public net.minecraft.world.entity.AreaEffectCloud getHandle() {
        return (net.minecraft.world.entity.AreaEffectCloud) super.getHandle();
    }

    @Override
    public String toString() {
        return "CraftAreaEffectCloud";
    }

    @Override
    public int getDuration() {
        return this.getHandle().getDuration();
    }

    @Override
    public void setDuration(int duration) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setDuration(duration);
            return null;
        });
    }

    @Override
    public int getWaitTime() {
        return this.getHandle().waitTime;
    }

    @Override
    public void setWaitTime(int waitTime) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setWaitTime(waitTime);
            return null;
        });
    }

    @Override
    public int getReapplicationDelay() {
        return this.getHandle().reapplicationDelay;
    }

    @Override
    public void setReapplicationDelay(int delay) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().reapplicationDelay = delay;
            return null;
        });
    }

    @Override
    public int getDurationOnUse() {
        return this.getHandle().durationOnUse;
    }

    @Override
    public void setDurationOnUse(int duration) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().durationOnUse = duration;
            return null;
        });
    }

    @Override
    public float getRadius() {
        return this.getHandle().getRadius();
    }

    @Override
    public void setRadius(float radius) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setRadius(radius);
            return null;
        });
    }

    @Override
    public float getRadiusOnUse() {
        return this.getHandle().radiusOnUse;
    }

    @Override
    public void setRadiusOnUse(float radius) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setRadiusOnUse(radius);
            return null;
        });
    }

    @Override
    public float getRadiusPerTick() {
        return this.getHandle().radiusPerTick;
    }

    @Override
    public void setRadiusPerTick(float radius) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setRadiusPerTick(radius);
            return null;
        });
    }

    @Override
    public Particle getParticle() {
        return CraftParticle.minecraftToBukkit(this.getHandle().getParticle().getType());
    }

    @Override
    public void setParticle(Particle particle) {
        this.setParticle(particle, null);
    }

    @Override
    public <T> void setParticle(Particle particle, T data) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setParticle(CraftParticle.createParticleParam(particle, data));
            return null;
        });
    }

    @Override
    public Color getColor() {
        return Color.fromRGB(this.getHandle().potionContents.getColor() & 0x00FFFFFF); // Paper - skip alpha channel
    }

    @Override
    public void setColor(Color color) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            PotionContents old = this.getHandle().potionContents;
            this.getHandle().setPotionContents(new PotionContents(old.potion(), Optional.of(color.asRGB()), old.customEffects()));
            return null;
        });
    }

    @Override
    public boolean addCustomEffect(PotionEffect effect, boolean override) {
        return ThreadedServer.onMainThreadIfRegionThread(() -> {
            if (this.hasCustomEffect(effect.getType())) {
                if (!override) {
                    return false;
                }
                this.removeCustomEffect(effect.getType());
            }
            this.getHandle().addEffect(CraftPotionUtil.fromBukkit(effect));
            // this.getHandle().updateColor(); // Paper - already done above
            return true;
        });
    }

    @Override
    public void clearCustomEffects() {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            PotionContents old = this.getHandle().potionContents;
            this.getHandle().setPotionContents(new PotionContents(old.potion(), old.customColor(), List.of()));
            // this.getHandle().updateColor(); // Paper - already done above
            return null;
        });
    }

    @Override
    public List<PotionEffect> getCustomEffects() {
        ImmutableList.Builder<PotionEffect> builder = ImmutableList.builder();
        for (MobEffectInstance effect : this.getHandle().potionContents.customEffects()) {
            builder.add(CraftPotionUtil.toBukkit(effect));
        }
        return builder.build();
    }

    @Override
    public boolean hasCustomEffect(PotionEffectType type) {
        for (MobEffectInstance effect : this.getHandle().potionContents.customEffects()) {
            if (CraftPotionUtil.equals(effect.getEffect(), type)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasCustomEffects() {
        return !this.getHandle().potionContents.customEffects().isEmpty();
    }

    @Override
    public boolean removeCustomEffect(PotionEffectType effect) {
        return ThreadedServer.onMainThreadIfRegionThread(() -> {
            if (!this.hasCustomEffect(effect)) {
                return false;
            }
            Holder<MobEffect> minecraft = CraftPotionEffectType.bukkitToMinecraftHolder(effect);

            PotionContents old = this.getHandle().potionContents;
            this.getHandle().setPotionContents(new PotionContents(old.potion(), old.customColor(), old.customEffects().stream().filter((mobEffect) -> !mobEffect.getEffect().equals(minecraft)).toList()));
            return true;
        });
    }

    @Override
    public void setBasePotionData(PotionData data) {
        setBasePotionType(CraftPotionUtil.fromBukkit(data));
    }

    @Override
    public PotionData getBasePotionData() {
        return CraftPotionUtil.toBukkit(getBasePotionType());
    }

    @Override
    public void setBasePotionType(PotionType potionType) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            if (potionType != null) {
                this.getHandle().setPotionContents(this.getHandle().potionContents.withPotion(CraftPotionType.bukkitToMinecraftHolder(potionType)));
            } else {
                PotionContents old = this.getHandle().potionContents;
                this.getHandle().setPotionContents(new PotionContents(Optional.empty(), old.customColor(), old.customEffects()));
            }
            return null;
        });
    }

    @Override
    public PotionType getBasePotionType() {
        return this.getHandle().potionContents.potion().map(CraftPotionType::minecraftHolderToBukkit).orElse(null);
    }

    @Override
    public ProjectileSource getSource() {
        net.minecraft.world.entity.LivingEntity source = this.getHandle().getOwner();
        return (source == null) ? null : (LivingEntity) source.getBukkitEntity();
    }

    @Override
    public void setSource(ProjectileSource shooter) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            if (shooter instanceof CraftLivingEntity craftLivingEntity) {
                this.getHandle().setOwner(craftLivingEntity.getHandle());
            } else {
                this.getHandle().setOwner(null);
            }
            return null;
        });
    }

    // Paper start - owner API
    @Override
    public java.util.UUID getOwnerUniqueId() {
        return this.getHandle().ownerUUID;
    }

    @Override
    public void setOwnerUniqueId(final java.util.UUID ownerUuid) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setOwner(null);
            this.getHandle().ownerUUID = ownerUuid;
            return null;
        });
    }
    // Paper end
}

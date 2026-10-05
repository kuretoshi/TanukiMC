package org.bukkit.craftbukkit.entity;

import com.google.common.base.Preconditions;
import com.mohistmc.youer.threading.ThreadedServer;
import org.bukkit.Color;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Display;
import org.bukkit.util.Transformation;

public class CraftDisplay extends CraftEntity implements Display {

    public CraftDisplay(CraftServer server, net.minecraft.world.entity.Display entity) {
        super(server, entity);
    }

    @Override
    public net.minecraft.world.entity.Display getHandle() {
        return (net.minecraft.world.entity.Display) super.getHandle();
    }

    @Override
    public String toString() {
        return "CraftDisplay";
    }

    @Override
    public Transformation getTransformation() {
        com.mojang.math.Transformation nms = net.minecraft.world.entity.Display.createTransformation(this.getHandle().getEntityData());

        return new Transformation(nms.getTranslation(), nms.getLeftRotation(), nms.getScale(), nms.getRightRotation());
    }

    @Override
    public void setTransformation(Transformation transformation) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            Preconditions.checkArgument(transformation != null, "Transformation cannot be null");

            this.getHandle().setTransformation(new com.mojang.math.Transformation(transformation.getTranslation(), transformation.getLeftRotation(), transformation.getScale(), transformation.getRightRotation()));
            return null;
        });
    }

    @Override
    public void setTransformationMatrix(org.joml.Matrix4f transformationMatrix) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            Preconditions.checkArgument(transformationMatrix != null, "Transformation matrix cannot be null");

            this.getHandle().setTransformation(new com.mojang.math.Transformation(transformationMatrix));
            return null;
        });
    }

    @Override
    public int getInterpolationDuration() {
        return this.getHandle().getTransformationInterpolationDuration();
    }

    @Override
    public void setInterpolationDuration(int duration) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setTransformationInterpolationDuration(duration);
            return null;
        });
    }

    @Override
    public int getTeleportDuration() {
        return this.getHandle().getEntityData().get(net.minecraft.world.entity.Display.DATA_POS_ROT_INTERPOLATION_DURATION_ID);
    }

    @Override
    public void setTeleportDuration(int duration) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            Preconditions.checkArgument(duration >= 0 && duration <= 59, "duration (%s) cannot be lower than 0 or higher than 59", duration);
            this.getHandle().getEntityData().set(net.minecraft.world.entity.Display.DATA_POS_ROT_INTERPOLATION_DURATION_ID, duration);
            return null;
        });
    }

    @Override
    public float getViewRange() {
        return this.getHandle().getViewRange();
    }

    @Override
    public void setViewRange(float range) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setViewRange(range);
            return null;
        });
    }

    @Override
    public float getShadowRadius() {
        return this.getHandle().getShadowRadius();
    }

    @Override
    public void setShadowRadius(float radius) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setShadowRadius(radius);
            return null;
        });
    }

    @Override
    public float getShadowStrength() {
        return this.getHandle().getShadowStrength();
    }

    @Override
    public void setShadowStrength(float strength) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setShadowStrength(strength);
            return null;
        });
    }

    @Override
    public float getDisplayWidth() {
        return this.getHandle().getWidth();
    }

    @Override
    public void setDisplayWidth(float width) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setWidth(width);
            return null;
        });
    }

    @Override
    public float getDisplayHeight() {
        return this.getHandle().getHeight();
    }

    @Override
    public void setDisplayHeight(float height) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setHeight(height);
            return null;
        });
    }

    @Override
    public int getInterpolationDelay() {
        return this.getHandle().getTransformationInterpolationDelay();
    }

    @Override
    public void setInterpolationDelay(int ticks) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            this.getHandle().setTransformationInterpolationDelay(ticks);
            return null;
        });
    }

    @Override
    public Billboard getBillboard() {
        return Billboard.valueOf(this.getHandle().getBillboardConstraints().name());
    }

    @Override
    public void setBillboard(Billboard billboard) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            Preconditions.checkArgument(billboard != null, "Billboard cannot be null");

            this.getHandle().setBillboardConstraints(net.minecraft.world.entity.Display.BillboardConstraints.valueOf(billboard.name()));
            return null;
        });
    }

    @Override
    public Color getGlowColorOverride() {
        int color = this.getHandle().getGlowColorOverride();

        return (color == -1) ? null : Color.fromARGB(color);
    }

    @Override
    public void setGlowColorOverride(Color color) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            if (color == null) {
                this.getHandle().setGlowColorOverride(-1);
            } else {
                this.getHandle().setGlowColorOverride(color.asARGB());
            }
            return null;
        });
    }

    @Override
    public Brightness getBrightness() {
        net.minecraft.util.Brightness nms = this.getHandle().getBrightnessOverride();

        return (nms != null) ? new Brightness(nms.block(), nms.sky()) : null;
    }

    @Override
    public void setBrightness(Brightness brightness) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            if (brightness != null) {
                this.getHandle().setBrightnessOverride(new net.minecraft.util.Brightness(brightness.getBlockLight(), brightness.getSkyLight()));
            } else {
                this.getHandle().setBrightnessOverride(null);
            }
            return null;
        });
    }
}

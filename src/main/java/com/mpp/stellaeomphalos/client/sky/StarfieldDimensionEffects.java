package com.mpp.stellaeomphalos.client.sky;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;

/** Datapacks opt in with effects=stellaeomphalos:starfield. Vanilla effects are never replaced. */
public final class StarfieldDimensionEffects extends DimensionSpecialEffects {
    public StarfieldDimensionEffects() {
        super(192, true, SkyType.NORMAL, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 color, float sun) {
        return color.multiply(sun * .94 + .06, sun * .94 + .06, sun * .91 + .09);
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return false;
    }

    @Override
    public boolean renderSky(
            net.minecraft.client.multiplayer.ClientLevel level,
            int ticks,
            float partial,
            com.mojang.blaze3d.vertex.PoseStack pose,
            net.minecraft.client.Camera camera,
            org.joml.Matrix4f projection,
            boolean foggy,
            Runnable setupFog) {
        // REPLACE suppresses the vanilla sky only for this explicitly bound dimension; AFTER_SKY
        // draws the starfield.
        if (com.mpp.stellaeomphalos.OmphalosConfig.CLIENT.snapshot().get("sky.overlay")
                != com.mpp.stellaeomphalos.OmphalosConfig.SkyMode.REPLACE) return false;
        setupFog.run();
        return true;
    }
}

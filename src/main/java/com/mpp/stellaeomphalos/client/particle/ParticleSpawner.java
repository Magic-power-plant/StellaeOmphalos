package com.mpp.stellaeomphalos.client.particle;

import com.mpp.stellaeomphalos.OmphalosConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ParticleOptions;

/** Budget applies inside providers too, so server particle packets cannot bypass it. */
public final class ParticleSpawner {
    private static int alive, generation;

    private ParticleSpawner() {}

    public static int acquire(double x, double y, double z, boolean heavy) {
        var quality =
                (OmphalosConfig.ParticleQuality)
                        OmphalosConfig.CLIENT.snapshot().get("particles.quality");
        var mc = Minecraft.getInstance();
        int distance = OmphalosConfig.CLIENT.integer("particles.effectDistance");
        if (quality == OmphalosConfig.ParticleQuality.OFF
                || mc.level == null
                || alive >= OmphalosConfig.CLIENT.integer("particles.budget")
                || mc.gameRenderer.getMainCamera().getPosition().distanceToSqr(x, y, z)
                        > (double) distance * distance) return -1;
        if (heavy
                && quality == OmphalosConfig.ParticleQuality.LOW
                && mc.level.random.nextInt(3) != 0) return -1;
        alive++;
        return generation;
    }

    public static void release(int ticket) {
        if (ticket == generation && alive > 0) alive--;
    }

    public static Particle spawn(
            ParticleOptions options,
            double x,
            double y,
            double z,
            double vx,
            double vy,
            double vz) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return null;
        return mc.particleEngine.createParticle(options, x, y, z, vx, vy, vz);
    }

    public static void clear() {
        alive = 0;
        generation++;
    }

    public static int alive() {
        return alive;
    }
}

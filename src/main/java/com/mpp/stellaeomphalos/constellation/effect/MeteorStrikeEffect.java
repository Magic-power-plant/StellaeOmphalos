package com.mpp.stellaeomphalos.constellation.effect;

import com.mpp.stellaeomphalos.OmphalosConfig;
import com.mpp.stellaeomphalos.constellation.domain.DomainParticles;
import com.mpp.stellaeomphalos.constellation.sign.SignSkyService;
import com.mpp.stellaeomphalos.core.util.combat.DamageKit;
import com.mpp.stellaeomphalos.core.util.math.SkyDensityField;
import com.mpp.stellaeomphalos.network.toClient.PktDomainParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

/**
 * Celestial strike: a flattened ellipsoid AOE (Y radius halved). Base damage floats with the time
 * of day and sky noise: {@code 10 + dayFactor*40 + skyNoise*60}; per-target falloff is
 * {@code 1 - clamp(d/r)}; hits below 0.5 damage are skipped. Position and radius are broadcast to
 * players within 96 blocks.
 */
public final class MeteorStrikeEffect {
    private MeteorStrikeEffect() {}

    /** Pure damage formula, exposed for unit tests. Returns 0 when below the 0.5 cutoff. */
    public static float damageAt(double distance, double radius, double dayFactor, double skyNoise) {
        if (radius <= 0) return 0.0F;
        double falloff = 1.0 - Math.min(Math.max(distance / radius, 0.0), 1.0);
        float damage = (float) ((10.0 + dayFactor * 40.0 + skyNoise * 60.0) * falloff);
        return damage < 0.5F ? 0.0F : damage;
    }

    public static void strike(ServerLevel level, BlockPos center, double radius) {
        var noise = new SkyDensityField(level.getSeed(), OmphalosConfig.COMMON.integer("performance.skyDensityGridSize"))
                .sample(center.getX(), center.getZ());
        double dayFactor = SignSkyService.dayDistributionFactor(level);
        var box = new AABB(center).inflate(radius, radius * 0.5, radius);   // flattened ellipsoid scan box
        for (var entity : level.getEntitiesOfClass(LivingEntity.class, box)) {
            double dx = entity.getX() - (center.getX() + 0.5);
            double dy = (entity.getY() - center.getY()) * 2.0;              // Y radius halved
            double dz = entity.getZ() - (center.getZ() + 0.5);
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            float damage = damageAt(distance, radius, dayFactor, noise);
            if (damage <= 0.0F) continue;
            DamageKit.apply(entity, level.damageSources().magic(), damage);
        }
        DomainParticles.broadcast(level, center, 96.0, PktDomainParticle.Types.METEOR_STRIKE,
                null, level.random.nextLong());
    }
}

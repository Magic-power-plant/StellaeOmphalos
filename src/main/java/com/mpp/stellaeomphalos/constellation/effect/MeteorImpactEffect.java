package com.mpp.stellaeomphalos.constellation.effect;

import com.mpp.stellaeomphalos.constellation.domain.DomainParticles;
import com.mpp.stellaeomphalos.network.toClient.PktDomainParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

/**
 * Shooting-star impact: when the star is not extinguished this is a real explosion (power 4.5, no
 * fire, block damage governed by {@code mobGriefing}); an extinguished star only plays the effect.
 * The effect seed rides the broadcast packet so every client renders the identical burst.
 */
public final class MeteorImpactEffect {
    public static final float EXPLOSION_POWER = 4.5F;

    private MeteorImpactEffect() {}

    public static void impact(ServerLevel level, BlockPos center, boolean extinguished, long seed) {
        if (!extinguished) {
            boolean griefing = level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
            level.explode(null, center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5,
                    EXPLOSION_POWER, false,
                    griefing ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE);
        }
        // Both routes broadcast; the seed travels with the packet for deterministic client visuals.
        DomainParticles.broadcast(level, center, 96.0, PktDomainParticle.Types.METEOR_IMPACT, null, seed);
    }
}

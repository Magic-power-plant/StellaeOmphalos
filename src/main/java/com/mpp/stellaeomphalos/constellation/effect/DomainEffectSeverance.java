package com.mpp.stellaeomphalos.constellation.effect;

import com.mpp.stellaeomphalos.constellation.domain.DomainContext;
import com.mpp.stellaeomphalos.constellation.domain.DomainEntityScan;
import com.mpp.stellaeomphalos.constellation.domain.DomainParticles;
import com.mpp.stellaeomphalos.constellation.domain.DomainProperties;
import com.mpp.stellaeomphalos.constellation.sign.MajorSign;
import com.mpp.stellaeomphalos.core.util.combat.DamageKit;
import com.mpp.stellaeomphalos.network.toClient.PktDomainParticle;
import javax.annotation.Nullable;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Monster;

/**
 * discidia — severance. Low-frequency AOE (every 20 ticks): damages hostile monsters
 * (1.20.1: {@code Monster} replaces the old Enemy probe). {@code hurtResistantTime} is zeroed for
 * the hit and restored in {@code finally}. Scan cap 24. Corrupted: heals monsters, grants
 * resistance, and backlashes the pedestal owner.
 */
public final class DomainEffectSeverance extends DomainEntityScan<Monster> {
    private static final int MAX_TARGETS = 24;
    private static final int INTERVAL_TICKS = 20;
    private static final float BASE_DAMAGE = 6.0F;
    private static final float BACKLASH_DAMAGE = 4.0F;

    public DomainEffectSeverance(@Nullable MajorSign owner) {
        super(owner, Monster.class, MAX_TARGETS);
    }

    @Override
    public DomainProperties provideProperties(int mirrorCount) {
        return new DomainProperties(6.0, 1.0, 1.0, false, 0.0, 1.0);
    }

    @Override
    public boolean play(DomainContext ctx, float strength, DomainProperties props) {
        var level = ctx.level();
        if (level.getGameTime() % INTERVAL_TICKS != 0) return false;
        if (!strengthGate(strength, level.random)) return false;
        if (props.corrupted()) return playCorrupted(ctx, props);
        boolean acted = false;
        for (var monster : collect(level, ctx.origin(), props.size())) {
            int invulnerable = monster.invulnerableTime;
            monster.invulnerableTime = 0;
            try {
                acted |= DamageKit.apply(monster, level.damageSources().magic(),
                        (float) (BASE_DAMAGE * props.potency() * Math.max(props.effectAmplifier(), 1.0)));
            } finally {
                monster.invulnerableTime = invulnerable;
            }
        }
        if (acted && DomainParticles.mergedThisTick(level, ctx.origin(), PktDomainParticle.Types.SEVERANCE))
            DomainParticles.broadcast(level, ctx.origin(), 96.0, PktDomainParticle.Types.SEVERANCE,
                    null, level.random.nextLong());
        return acted;
    }

    private boolean playCorrupted(DomainContext ctx, DomainProperties props) {
        var level = ctx.level();
        boolean acted = false;
        for (var monster : collect(level, ctx.origin(), props.size())) {
            monster.heal((float) (4.0 * props.potency()));
            monster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, INTERVAL_TICKS * 2, 1));
            acted = true;
        }
        var owner = owningPlayer(ctx);
        if (owner != null) {
            var player = level.getServer().getPlayerList().getPlayer(owner);
            if (player != null && player.level() == level
                    && player.blockPosition().distSqr(ctx.origin()) <= props.size() * props.size() * 16)
                DamageKit.apply(player, level.damageSources().magic(), BACKLASH_DAMAGE);
        }
        return acted;
    }
}

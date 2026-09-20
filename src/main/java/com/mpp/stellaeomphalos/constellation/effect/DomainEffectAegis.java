package com.mpp.stellaeomphalos.constellation.effect;

import com.mpp.stellaeomphalos.constellation.domain.DomainContext;
import com.mpp.stellaeomphalos.constellation.domain.DomainEntityScan;
import com.mpp.stellaeomphalos.constellation.domain.DomainParticles;
import com.mpp.stellaeomphalos.constellation.domain.DomainProperties;
import com.mpp.stellaeomphalos.constellation.domain.DomainSpawnDeny;
import com.mpp.stellaeomphalos.constellation.sign.MajorSign;
import com.mpp.stellaeomphalos.network.toClient.PktDomainParticle;
import javax.annotation.Nullable;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;

/**
 * armara — protection. Every tick: renews the spawn-deny token (+1..3 ticks, capped at 400),
 * reflects projectiles and knocks hostiles back. Scan cap 32. Corrupted: instead grants monsters
 * seven buffs (speed / strength / resistance / regeneration / fire resistance / absorption /
 * jump boost).
 */
public final class DomainEffectAegis extends DomainEntityScan<Entity> {
    private static final int MAX_TARGETS = 32;

    public DomainEffectAegis(@Nullable MajorSign owner) {
        super(owner, Entity.class, MAX_TARGETS);
    }

    @Override
    public DomainProperties provideProperties(int mirrorCount) {
        return new DomainProperties(6.0 + mirrorCount * 2.0, 1.0, 1.0, false, 0.0, 1.0);
    }

    @Override
    public boolean play(DomainContext ctx, float strength, DomainProperties props) {
        var level = ctx.level();
        if (!strengthGate(strength, level.random)) return false;
        if (props.corrupted()) return playCorrupted(ctx, props);
        DomainSpawnDeny.renew(level, ctx.origin(), props.size(), 1 + level.random.nextInt(3));
        boolean acted = false;
        for (var entity : collect(level, ctx.origin(), props.size())) {
            if (entity instanceof Projectile projectile && !entity.onGround()) {
                var motion = projectile.getDeltaMovement();
                projectile.setDeltaMovement(motion.scale(-0.6));
                projectile.setOwner(null);
                acted = true;
                if (DomainParticles.mergedThisTick(level, projectile.blockPosition(), PktDomainParticle.Types.AEGIS))
                    DomainParticles.broadcast(level, projectile.blockPosition(), 96.0, PktDomainParticle.Types.AEGIS,
                            ctx.origin(), level.random.nextLong());
            } else if (entity instanceof Mob mob && isHostile(mob)) {
                var away = mob.position().subtract(ctx.origin().getCenter()).normalize().scale(0.6);
                mob.push(away.x, 0.25, away.z);
                acted = true;
            } else if (entity instanceof net.minecraft.world.entity.player.Player player) {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 0));
                acted = true;
            }
        }
        return acted;
    }

    private boolean playCorrupted(DomainContext ctx, DomainProperties props) {
        boolean acted = false;
        for (var entity : collect(ctx.level(), ctx.origin(), props.size())) {
            if (!(entity instanceof Mob mob) || !isHostile(mob)) continue;
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1));
            mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1));
            mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 1));
            mob.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0));
            mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200, 0));
            mob.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 1));
            mob.addEffect(new MobEffectInstance(MobEffects.JUMP, 200, 1));
            acted = true;
        }
        return acted;
    }

    private static boolean isHostile(Mob mob) {
        return mob instanceof net.minecraft.world.entity.monster.Enemy;
    }
}

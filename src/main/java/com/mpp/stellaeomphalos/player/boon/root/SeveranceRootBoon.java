package com.mpp.stellaeomphalos.player.boon.root;

import javax.annotation.Nullable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.entity.living.LivingDamageEvent;

/**
 * Discidia root behavior: dealing damage grants experience at 0.09 per point. The attacker is
 * resolved with a two-level fallback — the damage source's true entity first, then the direct
 * entity's owner — so thrown and shot projectiles credit the shooting player.
 */
public final class SeveranceRootBoon {

    public static final String SIGN = "discidia";
    public static final double FACTOR = 0.09;

    private SeveranceRootBoon() {}

    public static void onDamage(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide || event.getAmount() <= 0) return;
        var attacker = attackerOf(event.getSource());
        if (attacker == null || attacker == event.getEntity()) return;
        if (!RootExpSupport.active(attacker, SIGN)) return;
        RootExpSupport.grant(attacker, SIGN, event.getAmount() * FACTOR);
    }

    /** Two-level source fallback: true entity, else the direct entity's owner (projectiles). */
    @Nullable
    public static ServerPlayer attackerOf(DamageSource source) {
        if (source.getEntity() instanceof ServerPlayer player) return player;
        if (source.getDirectEntity() instanceof Projectile projectile
                && projectile.getOwner() instanceof ServerPlayer owner) return owner;
        return null;
    }
}

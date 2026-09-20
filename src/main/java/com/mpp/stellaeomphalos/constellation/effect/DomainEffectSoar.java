package com.mpp.stellaeomphalos.constellation.effect;

import com.mpp.stellaeomphalos.constellation.domain.DomainContext;
import com.mpp.stellaeomphalos.constellation.domain.DomainEffect;
import com.mpp.stellaeomphalos.constellation.domain.DomainEffectRunner;
import com.mpp.stellaeomphalos.constellation.domain.DomainEffectStatus;
import com.mpp.stellaeomphalos.constellation.domain.DomainParticles;
import com.mpp.stellaeomphalos.constellation.domain.DomainProperties;
import com.mpp.stellaeomphalos.constellation.sign.MajorSign;
import com.mpp.stellaeomphalos.core.util.collections.TickExpiryMap;
import com.mpp.stellaeomphalos.network.toClient.PktDomainParticle;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * vicio — soar. Status-type effect: grants creative-style flight to players inside the effect range
 * (8 + mirrors x 2) through a per-player 40-tick token renewed every tick; vanilla ability packets
 * go out only when the permission actually changes. Corrupted: strips flight from survival /
 * adventure players and applies slowness + mining fatigue.
 */
public final class DomainEffectSoar extends DomainEffect implements DomainEffectStatus {
    private static final int TOKEN_TICKS = 40;

    private static final Map<ResourceKey<Level>, TickExpiryMap<UUID, Boolean>> TOKENS = new HashMap<>();

    public DomainEffectSoar(@Nullable MajorSign owner) { super(owner); }

    private static TickExpiryMap<UUID, Boolean> tokens(ServerLevel level) {
        return TOKENS.computeIfAbsent(level.dimension(), key ->
                new TickExpiryMap<>((playerId, ignored) -> revoke(level.getServer().getPlayerList().getPlayer(playerId))));
    }

    private static void syncAbilities(ServerPlayer player) {
        // Skip players without a live channel (login race, embedded/mock players): abilities stay
        // server-side correct and sync on the next vanilla abilities update.
        var listener = player.connection;
        if (listener != null && listener.connection.isConnected()) player.onUpdateAbilities();
    }

    private static void revoke(@Nullable ServerPlayer player) {
        if (player == null || player.isCreative() || player.isSpectator()) return;
        var abilities = player.getAbilities();
        if (abilities.mayfly) {
            abilities.mayfly = false;
            abilities.flying = false;
            syncAbilities(player);
        }
    }

    /** Grant seam (also exercised by the GameTest): false when skipped or already granted. */
    public static boolean grantFlight(ServerPlayer player) {
        if (player.isCreative() || player.isSpectator()) return false;
        var abilities = player.getAbilities();
        if (abilities.mayfly) return false;
        abilities.mayfly = true;
        syncAbilities(player);
        return true;
    }

    /** Revoke seam: strips granted flight from a survival/adventure player. */
    public static void revokeFlight(@Nullable ServerPlayer player) { revoke(player); }

    /** Drops all flight tokens and revokes granted flight (server stop / tests). */
    public static void revokeAll(net.minecraft.server.MinecraftServer server) {
        TOKENS.forEach((dimension, map) -> {
            map.clear();
            for (var player : server.getPlayerList().getPlayers()) revoke(player);
        });
        TOKENS.clear();
    }

    @Override
    public DomainProperties provideProperties(int mirrorCount) {
        return new DomainProperties(8.0 + mirrorCount * 2.0, 1.0, 1.0, false, 0.0, 1.0);
    }

    @Override
    public boolean isActive(DomainContext ctx) {
        return DomainEffectRunner.enabled();
    }

    @Override
    public boolean play(DomainContext ctx, float strength, DomainProperties props) {
        var level = ctx.level();
        var tokens = tokens(level);
        tokens.advance();
        var box = new AABB(ctx.origin()).inflate(props.size());
        boolean acted = false;
        for (var player : level.getEntitiesOfClass(ServerPlayer.class, box)) {
            if (player.isCreative() || player.isSpectator()) continue;
            if (props.corrupted()) {
                acted |= stripFlight(player);
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, TOKEN_TICKS, 1));
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, TOKEN_TICKS, 1));
            } else {
                if (grantFlight(player))   // packet only on permission change
                    DomainParticles.broadcast(level, player.blockPosition(), 96.0, PktDomainParticle.Types.SOAR,
                            ctx.origin(), level.random.nextLong());
                tokens.put(player.getUUID(), Boolean.TRUE, TOKEN_TICKS);
                acted = true;
            }
        }
        return acted;
    }

    private static boolean stripFlight(ServerPlayer player) {
        var abilities = player.getAbilities();
        if (!abilities.mayfly && !abilities.flying) return false;
        abilities.mayfly = false;
        abilities.flying = false;
        syncAbilities(player);
        return true;
    }
}

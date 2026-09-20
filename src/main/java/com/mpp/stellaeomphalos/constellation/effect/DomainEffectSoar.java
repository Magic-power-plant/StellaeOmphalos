package com.mpp.stellaeomphalos.constellation.effect;

import com.mpp.stellaeomphalos.constellation.domain.DomainContext;
import com.mpp.stellaeomphalos.constellation.domain.DomainEffect;
import com.mpp.stellaeomphalos.constellation.domain.DomainEffectRunner;
import com.mpp.stellaeomphalos.constellation.domain.DomainEffectStatus;
import com.mpp.stellaeomphalos.constellation.domain.DomainParticles;
import com.mpp.stellaeomphalos.constellation.domain.DomainProperties;
import com.mpp.stellaeomphalos.constellation.sign.MajorSign;
import com.mpp.stellaeomphalos.network.toClient.PktDomainParticle;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

/**
 * vicio — soar. Status-type effect: grants creative-style flight to players inside the effect range
 * (8 + mirrors x 2) through a per-player 40-tick token renewed every tick; vanilla ability packets
 * go out only when the permission actually changes. Corrupted: strips flight from survival /
 * adventure players and applies slowness + mining fatigue.
 */
public final class DomainEffectSoar extends DomainEffect implements DomainEffectStatus {
    private static final int TOKEN_TICKS = 40;

    private record Lease(
            ResourceKey<Level> dimension, net.minecraft.core.BlockPos origin, UUID player) {}

    private static final Map<Lease, Long> LEASES = new HashMap<>();
    private static final java.util.Set<UUID> OWNED_FLIGHT = new java.util.HashSet<>();

    public DomainEffectSoar(@Nullable MajorSign owner) {
        super(owner);
    }

    public static void tickLeases(ServerLevel level) {
        LEASES.entrySet()
                .removeIf(
                        e ->
                                e.getKey().dimension().equals(level.dimension())
                                        && e.getValue() <= level.getGameTime());
        for (var id : java.util.Set.copyOf(OWNED_FLIGHT))
            if (LEASES.keySet().stream().noneMatch(l -> l.player().equals(id))) {
                revoke(level.getServer().getPlayerList().getPlayer(id));
                OWNED_FLIGHT.remove(id);
            }
    }

    public static void logout(ServerPlayer player) {
        LEASES.keySet().removeIf(key -> key.player().equals(player.getUUID()));
        if (OWNED_FLIGHT.remove(player.getUUID())) revoke(player);
    }

    @Override
    public void suspend(DomainContext ctx) {
        LEASES.keySet()
                .removeIf(
                        k ->
                                k.dimension().equals(ctx.level().dimension())
                                        && k.origin().equals(ctx.origin()));
        tickLeases(ctx.level());
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
    public static void revokeFlight(@Nullable ServerPlayer player) {
        revoke(player);
    }

    /** Drops all flight tokens and revokes granted flight (server stop / tests). */
    public static void revokeAll(net.minecraft.server.MinecraftServer server) {
        for (var id : java.util.Set.copyOf(OWNED_FLIGHT))
            revoke(server.getPlayerList().getPlayer(id));
        LEASES.clear();
        OWNED_FLIGHT.clear();
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
        tickLeases(level);
        var box = new AABB(ctx.origin()).inflate(props.size());
        boolean acted = false;
        for (var player :
                com.mpp.stellaeomphalos.constellation.domain.DomainWorkBudget.entities(
                        level, ServerPlayer.class, box)) {
            if (player.isCreative() || player.isSpectator()) continue;
            if (props.corrupted()) {
                acted |= stripFlight(player);
                player.addEffect(
                        new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, TOKEN_TICKS, 1));
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, TOKEN_TICKS, 1));
            } else {
                if (grantFlight(player)) {
                    OWNED_FLIGHT.add(player.getUUID());
                }
                if (OWNED_FLIGHT.contains(player.getUUID()))
                    DomainParticles.broadcast(
                            level,
                            player.blockPosition(),
                            96.0,
                            PktDomainParticle.Types.SOAR,
                            ctx.origin(),
                            level.random.nextLong());
                if (OWNED_FLIGHT.contains(player.getUUID()))
                    LEASES.put(
                            new Lease(
                                    level.dimension(), ctx.origin().immutable(), player.getUUID()),
                            level.getGameTime() + TOKEN_TICKS);
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

package com.mpp.stellaeomphalos.lumen.transport;

import com.mojang.logging.LogUtils;
import com.mpp.stellaeomphalos.network.OmphalosChannel;
import com.mpp.stellaeomphalos.network.OmphalosPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

/**
 * Lumen S2C send helper. Payloads stay unregistered until the integrator appends the frozen
 * IDs to NetworkBootstrap; sends degrade to a one-time warning instead of crashing.
 */
public final class LumenBroadcast {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean warned;
    private LumenBroadcast() {}

    public static void sendToPlayer(ServerPlayer player, OmphalosPayload payload) {
        try {
            OmphalosChannel.send(player, payload);
        } catch (IllegalArgumentException exception) {
            if (!warned) {
                warned = true;
                LOGGER.warn("Lumen payload not registered yet (integrator wiring pending): {}", exception.getMessage());
            }
        }
    }

    public static void sendToNearby(ServerLevel level, BlockPos pos, OmphalosPayload payload, double radiusSquared) {
        var center = Vec3.atCenterOf(pos);
        for (var player : level.players())
            if (player.distanceToSqr(center) <= radiusSquared) sendToPlayer(player, payload);
    }
}

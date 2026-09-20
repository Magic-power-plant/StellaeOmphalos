package com.mpp.stellaeomphalos.network;

import net.minecraft.server.level.ServerPlayer;

/**
 * Send helper that tolerates payloads whose registration line has not been wired into
 * NetworkBootstrap by the integrator yet. Any other send failure is rethrown.
 */
public final class SafeDispatch {
    private SafeDispatch() {}
    public static boolean send(ServerPlayer player, OmphalosPayload payload) {
        if (player.connection == null) return false;
        try {
            OmphalosChannel.send(player, payload);
            return true;
        } catch (IllegalArgumentException exception) {
            if ("Unregistered payload".equals(exception.getMessage())) return false;
            throw exception;
        }
    }
}

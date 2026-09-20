package com.mpp.stellaeomphalos.constellation.sign;

import com.mpp.stellaeomphalos.core.bootstrap.RuntimeServices;
import com.mpp.stellaeomphalos.network.OmphalosChannel;
import com.mpp.stellaeomphalos.network.toClient.PktSkySeed;
import com.mpp.stellaeomphalos.network.toServer.PktSkySeedRequest;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerAboutToStartEvent;

/** Server-side payload handlers for the sign module; the integrator calls {@link #attachServer()}. */
public final class SignServerHooks {
    private SignServerHooks() {}

    /** Subscribes the session hook that installs C2S handlers into each new server session. */
    public static void attachServer() {
        MinecraftForge.EVENT_BUS.addListener(SignServerHooks::serverStarting);
    }

    private static void serverStarting(ServerAboutToStartEvent event) {
        RuntimeServices.current().handlers().register(PktSkySeedRequest.class, SignServerHooks::onSeedRequest);
    }

    private static void onSeedRequest(ServerPlayer player, PktSkySeedRequest request) {
        var sessions = SignSkyService.sessions();
        int session = sessions.current(player.getUUID());
        if (request.sessionId() >= 0 && request.sessionId() != session) return;   // stale session: drop
        var level = player.server.getLevel(request.dim());
        if (level == null) return;
        long tick = RuntimeServices.current().scheduler().currentTick();
        if (!sessions.tryAcquireRequest(player.getUUID(), request.dim(), tick)) return;
        OmphalosChannel.send(player, new PktSkySeed(session, request.dim(), level.getSeed()));
    }
}

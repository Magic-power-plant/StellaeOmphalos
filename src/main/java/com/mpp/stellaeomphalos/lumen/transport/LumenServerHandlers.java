package com.mpp.stellaeomphalos.lumen.transport;

import com.mpp.stellaeomphalos.Omphalos;
import com.mpp.stellaeomphalos.core.util.CooldownGuard;
import com.mpp.stellaeomphalos.core.util.world.ChunkSafeAccess;
import com.mpp.stellaeomphalos.lumen.capability.LumenSink;
import com.mpp.stellaeomphalos.network.toClient.PktLumenDelta;
import com.mpp.stellaeomphalos.network.toServer.PktLumenSinkQuery;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/** C2S lumen handlers; every entry re-validates distance, chunk state and cooldown. */
final class LumenServerHandlers {
    private static final ResourceLocation SINK_QUERY = new ResourceLocation(Omphalos.MODID, "lumen_sink_query");
    private static final double QUERY_RANGE_SQ = 64 * 64;
    private static final long QUERY_COOLDOWN_TICKS = 10;
    private static final CooldownGuard COOLDOWN = new CooldownGuard();
    private LumenServerHandlers() {}

    static void sinkQuery(ServerPlayer player, PktLumenSinkQuery packet) {
        var level = player.serverLevel();
        if (player.distanceToSqr(Vec3.atCenterOf(packet.pos())) > QUERY_RANGE_SQ || !level.hasChunkAt(packet.pos())) return;
        if (!COOLDOWN.acquire(player.getUUID(), SINK_QUERY, level.getGameTime(), QUERY_COOLDOWN_TICKS)) return;
        ChunkSafeAccess.entity(level, packet.pos(), BlockEntity.class)
                .filter(LumenSink.class::isInstance).map(LumenSink.class::cast)
                .ifPresent(sink -> LumenBroadcast.sendToPlayer(player,
                        new PktLumenDelta(LumenSession.current(), packet.pos(), sink.lumenStored(), sink.lumenCapacity())));
    }

    static void clear() { COOLDOWN.clear(); }
}

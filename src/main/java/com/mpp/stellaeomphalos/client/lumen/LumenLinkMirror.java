package com.mpp.stellaeomphalos.client.lumen;

import com.mpp.stellaeomphalos.client.OmphalosClient;
import com.mpp.stellaeomphalos.client.event.ClientSessionCleaner;
import com.mpp.stellaeomphalos.lumen.capability.LumenIO;
import com.mpp.stellaeomphalos.network.toClient.PktLumenDelta;
import com.mpp.stellaeomphalos.network.toClient.PktLumenNode;
import com.mpp.stellaeomphalos.network.toServer.PktLumenSinkQuery;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;

/**
 * Read-only client mirror of the lumen link topology (LumenLinkView semantics for 《客户端渲染界面与音效》 beam
 * rendering) plus sink stored/capacity snapshots for GUIs. Session-guarded: payloads carrying a
 * stale session id are dropped; a newer session id wipes the cache and takes over.
 * Cleanup action registered by attachClient(): ClientSessionCleaner "lumen_link_mirror" -> clear().
 */
public final class LumenLinkMirror {
    /** Read-only link snapshot of one node: its position, side and resolved link targets. */
    public record LumenLinkView(BlockPos pos, LumenIO io, List<BlockPos> connections) {
        public LumenLinkView { connections = List.copyOf(connections); }
    }
    public record SinkState(long stored, long capacity) {}

    public static final double DISTANCE_CAP_SQ = 64 * 64;
    private static final Map<BlockPos, LumenLinkView> LINKS = new HashMap<>();
    private static final Map<BlockPos, SinkState> SINK_STATES = new HashMap<>();
    private static int session;
    private LumenLinkMirror() {}

    /** Integrator hook: call inside OmphalosClient.setup enqueueWork. */
    public static void attachClient() {
        OmphalosClient.handlers().register(PktLumenNode.class, (minecraft, packet) -> accept(packet));
        OmphalosClient.handlers().register(PktLumenDelta.class, (minecraft, packet) -> accept(packet));
        ClientSessionCleaner.register("lumen_link_mirror", LumenLinkMirror::clear);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(
                (net.minecraftforge.client.event.ClientPlayerNetworkEvent.Clone event) -> clear());
    }

    private static boolean acceptSession(int sessionId) {
        if (sessionId > session) {
            session = sessionId;
            LINKS.clear();
            SINK_STATES.clear();
        }
        return sessionId == session;
    }

    private static void accept(PktLumenNode packet) {
        if (!acceptSession(packet.sessionId())) return;
        var values = LumenIO.values();
        if (packet.io() < 0 || packet.io() >= values.length) return;
        LINKS.put(packet.pos(), new LumenLinkView(packet.pos(), values[packet.io()], packet.connections()));
    }

    private static void accept(PktLumenDelta packet) {
        if (!acceptSession(packet.sessionId())) return;
        SINK_STATES.put(packet.pos(), new SinkState(packet.stored(), packet.capacity()));
    }

    /** GUI hook: ask the server for a sink snapshot (server re-validates distance and chunk). */
    public static void sendQuery(BlockPos pos) {
        OmphalosClient.sendDependent(new PktLumenSinkQuery(session, pos));
    }

    public static List<LumenLinkView> links() { return List.copyOf(LINKS.values()); }
    public static Optional<SinkState> sinkState(BlockPos pos) { return Optional.ofNullable(SINK_STATES.get(pos)); }
    public static int session() { return session; }

    public static void clear() {
        LINKS.clear();
        SINK_STATES.clear();
        session = 0;
    }
}

package com.mpp.stellaeomphalos.client.stasis;

import com.mpp.stellaeomphalos.client.OmphalosClient;
import com.mpp.stellaeomphalos.client.event.ClientSessionCleaner;
import com.mpp.stellaeomphalos.lumen.transport.stasis.StasisFilter;
import com.mpp.stellaeomphalos.network.toClient.PktStasisZone;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;

/**
 * Client-side read-only mirror of stasis zones of the current dimension (rendering data for
 * Part-7). Cleanup action ("stasis_mirror"): drop all zones and reset the session so late
 * packets from a previous session are discarded (session_id mismatch).
 */
public final class StasisMirror {
    public record Zone(BlockPos center, float radius, StasisFilter.Mode filterMode, Optional<UUID> owner, int particleTier) {}
    private record Key(long center, float radius) {}

    private static final int NO_SESSION = Integer.MIN_VALUE;
    private static final AtomicBoolean ATTACHED = new AtomicBoolean();
    private static final Map<Key, Zone> ZONES = new LinkedHashMap<>();
    private static int session = NO_SESSION;

    private StasisMirror() {}

    public static Collection<Zone> zones() { return List.copyOf(ZONES.values()); }

    public static void handle(PktStasisZone packet) {
        if (session != NO_SESSION && packet.sessionId() != session) return;
        session = packet.sessionId();
        for (var entry : packet.zones()) {
            var key = new Key(entry.center(), entry.radius());
            if (entry.op() == PktStasisZone.Op.REMOVE) ZONES.remove(key);
            else ZONES.put(key, new Zone(BlockPos.of(entry.center()), entry.radius(),
                    entry.filterMode() == PktStasisZone.FilterMode.ALL_EXCEPT ? StasisFilter.Mode.ALL_EXCEPT : StasisFilter.Mode.NO_PLAYERS,
                    entry.owner(), entry.particleTier()));
        }
    }

    public static void reset() { ZONES.clear(); session = NO_SESSION; }

    public static void attach() {
        if (!ATTACHED.compareAndSet(false, true)) return;
        ClientSessionCleaner.register("stasis_mirror", StasisMirror::reset);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(
                (net.minecraftforge.client.event.ClientPlayerNetworkEvent.Clone event) -> reset());
        OmphalosClient.handlers().register(PktStasisZone.class, (minecraft, packet) -> handle(packet));
    }
}

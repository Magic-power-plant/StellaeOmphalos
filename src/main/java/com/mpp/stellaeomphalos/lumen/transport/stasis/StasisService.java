package com.mpp.stellaeomphalos.lumen.transport.stasis;

import com.mpp.stellaeomphalos.OmphalosConfig;
import com.mpp.stellaeomphalos.network.SafeDispatch;
import com.mpp.stellaeomphalos.network.toClient.PktStasisZone;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Per-dimension stasis zone tables. Lifecycle mirrors StarlightChargeService: lazy per server,
 * dropped on ServerStoppingEvent via StasisBootstrap.
 *
 * Zones persist through StasisData after every mutation; on first touch of a dimension the
 * stored zones are re-created (WARMUP, re-parked) with their freeze strategy and remaining
 * ticks intact (AC-2.8). When the zone cap is exceeded the oldest zone is force-released.
 */
public final class StasisService {
    private static final AtomicInteger SESSION_COUNTER = new AtomicInteger();
    private static StasisService current;
    private final MinecraftServer server;
    private final int sessionId = SESSION_COUNTER.incrementAndGet();
    private final Map<ResourceKey<Level>, List<StasisZone>> zones = new HashMap<>();
    private final Map<ResourceKey<Level>, List<PktStasisZone.ZoneEntry>> pendingSync = new HashMap<>();

    private StasisService(MinecraftServer server) { this.server = server; }

    public static StasisService get(MinecraftServer server) {
        if (current == null || current.server != server) current = new StasisService(server);
        return current;
    }

    static void shutdown() { current = null; }

    public boolean activate(ServerLevel level, BlockPos center, double radius, StasisFilter filter, long durationTicks, @Nullable UUID owner) {
        if (!enabled() || durationTicks <= 0) return false;
        double clampedRadius = Math.max(1, Math.min(radius, maxRadius()));
        var list = zoneList(level);
        while (list.size() >= maxZones()) {
            var oldest = list.remove(0);
            oldest.release();
            queueSync(level, oldest, PktStasisZone.Op.REMOVE);
        }
        var zone = new StasisZone(center, clampedRadius, filter, durationTicks, owner);
        zone.park(level, maxFrozenBlockEntities());
        zone.setParticleTier(zone.parkedCount() > maxFrozenBlockEntities() / 2 ? 1 : 0);
        list.add(zone);
        queueSync(level, zone, PktStasisZone.Op.UPSERT);
        persist(level);
        return true;
    }

    public boolean isFrozen(Entity entity) {
        if (!(entity.level() instanceof ServerLevel level)) return false;
        var list = zones.get(level.dimension());
        if (list == null || list.isEmpty()) return false;
        for (var zone : list) if (zone.freezesNow(entity)) return true;
        return false;
    }

    public boolean isBlockEntityFrozen(ServerLevel level, BlockPos pos) {
        var list = zones.get(level.dimension());
        if (list == null) return false;
        for (var zone : list) if (zone.freezesBlockEntityNow(pos)) return true;
        return false;
    }

    public List<StasisZoneView> zones(ServerLevel level) {
        return zoneList(level).stream().map(StasisZone::view).toList();
    }

    /** Drops runtime zone state for a dimension and re-creates it from storage (used after reload). */
    public void reload(ServerLevel level) {
        zones.remove(level.dimension());
        zoneList(level);
    }

    /** Removes every zone of a dimension immediately (admin/debug/testing). */
    public int clear(ServerLevel level) {
        var list = zoneList(level);
        int count = list.size();
        for (var zone : list) {
            zone.release();
            queueSync(level, zone, PktStasisZone.Op.REMOVE);
        }
        list.clear();
        persist(level);
        return count;
    }

    void tick(ServerLevel level) {
        var list = zoneList(level);
        if (!list.isEmpty()) {
            boolean changed = false;
            for (var iterator = list.iterator(); iterator.hasNext(); ) {
                var zone = iterator.next();
                if (zone.tick(level)) {
                    iterator.remove();
                    queueSync(level, zone, PktStasisZone.Op.REMOVE);
                    changed = true;
                }
            }
            if (changed) persist(level);
        }
        flushSync(level);
    }

    private List<StasisZone> zoneList(ServerLevel level) {
        return zones.computeIfAbsent(level.dimension(), key -> {
            var records = new ArrayList<>(StasisData.get(level).state().zones());
            records.removeIf(record -> record.remaining() <= 0);
            records.sort(java.util.Comparator.comparingLong(StasisData.ZoneRecord::remaining));
            while (records.size() > maxZones()) records.remove(0);
            var list = new ArrayList<StasisZone>();
            for (var record : records) {
                var zone = StasisZone.restore(record, maxRadius());
                zone.park(level, maxFrozenBlockEntities());
                list.add(zone);
            }
            return list;
        });
    }

    private void persist(ServerLevel level) {
        StasisData.get(level).store(new StasisData.State(zoneList(level).stream().map(StasisZone::toRecord).toList()));
    }

    private void queueSync(ServerLevel level, StasisZone zone, PktStasisZone.Op op) {
        var entry = new PktStasisZone.ZoneEntry(op, zone.center().asLong(), (float) zone.radius(),
                zone.filter().mode() == StasisFilter.Mode.ALL_EXCEPT ? PktStasisZone.FilterMode.ALL_EXCEPT : PktStasisZone.FilterMode.NO_PLAYERS,
                java.util.Optional.ofNullable(zone.filter().owner()), zone.particleTier());
        pendingSync.computeIfAbsent(level.dimension(), key -> new ArrayList<>()).add(entry);
    }

    private void flushSync(ServerLevel level) {
        var entries = pendingSync.remove(level.dimension());
        if (entries == null || entries.isEmpty()) return;
        var packet = new PktStasisZone(sessionId, entries);
        for (var player : level.players()) SafeDispatch.send(player, packet);
    }

    private static boolean enabled() { return OmphalosConfig.SERVER.flag("gameplay.stasisEnabled"); }
    private static int maxZones() { return OmphalosConfig.SERVER.integer("gameplay.stasisMaxZones"); }
    private static int maxRadius() { return OmphalosConfig.SERVER.integer("gameplay.stasisMaxRadius"); }
    private static int maxFrozenBlockEntities() { return OmphalosConfig.SERVER.integer("gameplay.stasisMaxFrozenBlockEntities"); }
}

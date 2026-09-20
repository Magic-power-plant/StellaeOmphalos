package com.mpp.stellaeomphalos.lumen.transport;

import com.mojang.logging.LogUtils;
import com.mpp.stellaeomphalos.OmphalosConfig;
import com.mpp.stellaeomphalos.core.util.world.ChunkSafeAccess;
import com.mpp.stellaeomphalos.lumen.capability.LumenDelivery;
import com.mpp.stellaeomphalos.lumen.capability.LumenIO;
import com.mpp.stellaeomphalos.lumen.capability.LumenNode;
import com.mpp.stellaeomphalos.lumen.capability.LumenSink;
import com.mpp.stellaeomphalos.lumen.capability.LumenSource;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;

/**
 * One dimension's lumen graph. Three-layer shard index (chunk to sectionY to pos); edges are
 * implicit: RELAY/PRISM nodes link to every in-range node, sources/sinks only link to transit
 * nodes. Resolutions are cached per topology epoch and recomputed by bounded incremental BFS.
 * Nodes in unloaded chunks are PENDING and truncate the BFS; the network never destroys blocks,
 * it only drops orphaned data with a warning.
 */
public final class LumenNetwork {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String NODE_PROVIDER = "stellaeomphalos:lumen_node";

    /** One slot of the shard index; a null node marks a save-restored placeholder (PENDING until its block entity joins). */
    static final class Entry {
        final BlockPos pos;
        LumenIO io;
        LumenNode node;
        boolean suspended;
        long stored;
        long capacity;
        Entry(BlockPos pos, LumenIO io) { this.pos = pos; this.io = io; }
        boolean pending() { return node == null || suspended; }
    }

    private record Cursor(BlockPos pos, double factor, int hops) {}
    private record Route(BlockPos sink, int hops, double factor) {}

    private final ServerLevel level;
    private final LumenNetworkData data;
    private final Map<Long, Map<Integer, Map<BlockPos, Entry>>> buckets = new HashMap<>();
    private final Map<BlockPos, LumenSourceData> sourceData = new LinkedHashMap<>();
    private final Map<BlockPos, EpochCache<List<Route>>> routes = new HashMap<>();
    private final Set<BlockPos> awaitingChunk = new HashSet<>();
    private final Set<BlockPos> announceQueue = new LinkedHashSet<>();
    private final LumenRoutingService routing;
    private long epoch = 1;
    private boolean persistRequested;
    private int nodeCount;

    public LumenNetwork(ServerLevel level, LumenNetworkData data) {
        this.level = level;
        this.data = data;
        routing = new LumenRoutingService(this);
        restore(data.state());
    }

    public ServerLevel level() { return level; }
    LumenRoutingService routing() { return routing; }
    public long topologyEpoch() { return epoch; }
    public int nodeCount() { return nodeCount; }
    /** Sources whose last resolution hit a PENDING node in an unloaded chunk; re-queued on chunk load. */
    public boolean awaitingChunk(BlockPos source) { return awaitingChunk.contains(source); }

    private void bumpEpoch() { epoch++; }

    /** A node whose block entity was removed is dead immediately, even before the leave queue drains. */
    private static boolean alive(Entry entry) {
        return !entry.pending() && !(entry.node instanceof BlockEntity entity && entity.isRemoved());
    }

    private Entry entryAt(BlockPos pos) {
        var bucket = buckets.get(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4));
        if (bucket == null) return null;
        var section = bucket.get(LumenMath.sectionY(pos.getY(), level.getMinBuildHeight()));
        return section == null ? null : section.get(pos);
    }

    // ---- topology mutation ----

    public boolean join(LumenNode node) {
        var pos = node.pos();
        long chunkKey = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
        var bucket = buckets.computeIfAbsent(chunkKey, key -> new HashMap<>());
        var section = bucket.computeIfAbsent(node.sectionY(), key -> new LinkedHashMap<>());
        var existing = section.get(pos);
        if (existing != null && existing.node == node && !existing.suspended) return false;
        int maxPerSection = OmphalosConfig.COMMON.integer("performance.lumenMaxNodesPerSection");
        if (existing == null && section.size() >= maxPerSection) {
            LOGGER.warn("Lumen section at {} is full ({} nodes); refusing node at {}", chunkKey, maxPerSection, pos);
            return false;
        }
        var entry = existing != null ? existing : new Entry(pos, node.io());
        entry.io = node.io();
        entry.node = node;
        entry.suspended = false;
        if (existing == null) {
            section.put(pos, entry);
            nodeCount++;
        }
        if (node instanceof LumenSource source) {
            sourceData.put(pos, source.data());
            routing.markProximityDirtyAround(pos);
        }
        bumpEpoch();
        node.markClean();
        announceQueue.add(pos);
        requestPersist();
        return true;
    }

    public boolean leave(BlockPos pos) {
        var entry = entryAt(pos);
        if (entry == null) return false;
        removeEntry(pos, entry);
        bumpEpoch();
        requestPersist();
        return true;
    }

    /** Synchronous kill switch run when a leave is enqueued: the node stops routing at once (epoch bump invalidates cached routes); the queued op removes the entry itself. */
    void markDead(BlockPos pos) {
        var entry = entryAt(pos);
        if (entry == null || entry.node == null) return;
        entry.node = null;
        bumpEpoch();
        requestPersist();
    }

    private void removeEntry(BlockPos pos, Entry entry) {
        long chunkKey = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
        var bucket = buckets.get(chunkKey);
        if (bucket == null) return;
        int sectionY = LumenMath.sectionY(pos.getY(), level.getMinBuildHeight());
        var section = bucket.get(sectionY);
        if (section != null && section.remove(pos) != null) {
            nodeCount--;
            if (section.isEmpty()) bucket.remove(sectionY);
        }
        if (bucket.isEmpty()) buckets.remove(chunkKey);
        if (entry.io == LumenIO.SOURCE) {
            sourceData.remove(pos);
            routing.markProximityDirtyAround(pos);
        }
        routes.remove(pos);
        awaitingChunk.remove(pos);
        announceQueue.remove(pos);
    }

    /** Revalidates a position: joins a live block entity, drops orphaned placeholder data with a warning (blocks are never touched). */
    public void rescan(BlockPos pos) {
        var entity = ChunkSafeAccess.entity(level, pos, BlockEntity.class);
        if (entity.isEmpty()) return;
        if (entity.get() instanceof LumenNode node) {
            var entry = entryAt(pos);
            if (entry == null || entry.node != node) join(node);
            else {
                bumpEpoch();
                node.markClean();
            }
            return;
        }
        var entry = entryAt(pos);
        if (entry != null) {
            removeEntry(pos, entry);
            bumpEpoch();
            requestPersist();
            LOGGER.warn("Discarding orphaned lumen node data at {} (block entity missing)", pos);
        }
    }

    /** Source parameters changed (sky visibility, enhancement, ...): invalidate cached routes. */
    public void markSourceDirty(BlockPos pos) {
        var entry = entryAt(pos);
        if (entry != null && entry.node instanceof LumenSource live) sourceData.put(pos, live.data());
        bumpEpoch();
    }

    /** Chunk unload: nodes in the chunk become PENDING and are skipped by routing. */
    public void suspendChunk(long chunkKey) {
        var bucket = buckets.get(chunkKey);
        if (bucket == null) return;
        boolean changed = false;
        for (var section : bucket.values())
            for (var entry : section.values())
                if (!entry.suspended) { entry.suspended = true; changed = true; }
        if (changed) bumpEpoch();
    }

    /** Chunk load: suspended nodes resume; live block entities re-join through their own onLoad. */
    public void resumeChunk(long chunkKey) {
        var bucket = buckets.get(chunkKey);
        if (bucket == null) return;
        boolean changed = false;
        for (var section : bucket.values())
            for (var entry : section.values())
                if (entry.suspended) { entry.suspended = false; changed = true; }
        if (changed) bumpEpoch();
    }

    // ---- routing ----

    /**
     * Subscription query: sinks reachable from a source, with effective LU after per-hop loss.
     * Results are cached per topology epoch; the edge budget bounds one BFS. When the budget is
     * exhausted the search aborts (nothing is cached) and the source is re-queued for next tick.
     */
    public List<LumenDelivery> resolve(BlockPos from, long budget) {
        var entry = entryAt(from);
        if (entry == null || !alive(entry) || !(entry.node instanceof LumenSource source)) return List.of();
        var cache = routes.computeIfAbsent(from, key -> new EpochCache<>());
        var found = cache.get(epoch, () -> computeRoutes(from, budget));
        if (found == null) {
            routing.enqueueResolve(from);
            return List.of();
        }
        long provided = source.provideLumen(level, level.getGameTime());
        if (provided <= 0) return List.of();
        var deliveries = new ArrayList<LumenDelivery>(found.size());
        for (var route : found) {
            long amount = LumenMath.scale(provided, route.factor());
            if (amount > 0) deliveries.add(new LumenDelivery(route.sink(), amount, route.hops()));
        }
        return List.copyOf(deliveries);
    }

    /** Total loss fraction (0..1) along the best known path; 1 when unreachable. */
    public double lossBetween(BlockPos from, BlockPos to) {
        var found = computeRoutes(from, OmphalosConfig.COMMON.integer("performance.lumenRoutingStepsPerTick"));
        if (found == null) return 1;
        for (var route : found)
            if (route.sink().equals(to)) return 1 - route.factor();
        return 1;
    }

    /** Delivers a resolved amount to its sink; @return accepted LU (0 when the sink vanished). */
    public long deliver(LumenDelivery delivery) {
        var entry = entryAt(delivery.sink());
        if (entry == null || !alive(entry) || !(entry.node instanceof LumenSink sink)) return 0;
        return sink.acceptLumen(level, delivery.amount(), false);
    }

    /** Recomputes and caches routes for a dirty source outside the hot path; re-queues when the budget runs out. */
    void warmResolve(BlockPos from, long budget) {
        var entry = entryAt(from);
        if (entry == null || !alive(entry)) return;
        var cache = routes.computeIfAbsent(from, key -> new EpochCache<>());
        if (cache.get(epoch, () -> computeRoutes(from, budget)) == null) routing.enqueueResolve(from);
    }

    private List<Route> computeRoutes(BlockPos from, long edgeBudget) {
        var origin = entryAt(from);
        if (origin == null || !alive(origin)) return List.of();
        int maxHops = OmphalosConfig.COMMON.integer("performance.lumenMaxHops");
        double baseLoss = OmphalosConfig.SERVER.decimal("gameplay.lumenLossPerHop");
        var best = new HashMap<BlockPos, Cursor>();
        var queue = new ArrayDeque<Cursor>();
        var start = new Cursor(from, 1, 0);
        best.put(from, start);
        queue.add(start);
        var result = new ArrayList<Route>();
        long edges = 0;
        boolean truncated = false;
        while (!queue.isEmpty()) {
            var current = queue.poll();
            var known = best.get(current.pos());
            if (known != current) continue;
            if (current.hops() >= maxHops) continue;
            var currentEntry = entryAt(current.pos());
            if (currentEntry == null) continue;
            for (var neighbor : neighbors(current.pos())) {
                if (!edgeAllowed(currentEntry.io, neighbor.io)) continue;
                if (neighbor.pending()) {
                    truncated = true;
                    continue;
                }
                if (!alive(neighbor)) continue;
                if (++edges > edgeBudget) {
                    if (truncated) awaitingChunk.add(from);
                    return null;
                }
                int heightDelta = Math.abs(neighbor.pos.getY() - current.pos().getY());
                double factor = current.factor() * (1 - LumenMath.hopLoss(baseLoss, heightDelta));
                int hops = current.hops() + 1;
                var previous = best.get(neighbor.pos);
                if (previous != null && previous.factor() >= factor) continue;
                var next = new Cursor(neighbor.pos, factor, hops);
                best.put(neighbor.pos, next);
                queue.add(next);
                if (neighbor.io == LumenIO.SINK) result.add(new Route(neighbor.pos, hops, factor));
            }
        }
        if (truncated) awaitingChunk.add(from);
        else awaitingChunk.remove(from);
        result.sort(Comparator.comparingInt(Route::hops).thenComparingLong(route -> route.sink().asLong()));
        return List.copyOf(result);
    }

    private static boolean edgeAllowed(LumenIO a, LumenIO b) {
        return a.isTransit() || b.isTransit();
    }

    /** Live link targets of one node (for the client link snapshot). */
    public List<BlockPos> connectionsOf(BlockPos pos) {
        var entry = entryAt(pos);
        if (entry == null || !alive(entry)) return List.of();
        var result = new ArrayList<BlockPos>();
        for (var neighbor : neighbors(pos))
            if (alive(neighbor) && edgeAllowed(entry.io, neighbor.io)) result.add(neighbor.pos);
        result.sort(Comparator.comparingLong(BlockPos::asLong));
        return List.copyOf(result);
    }

    private List<Entry> neighbors(BlockPos pos) {
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        int sectionY = LumenMath.sectionY(pos.getY(), level.getMinBuildHeight());
        long rangeSquared = (long) LumenMath.LINK_RANGE * LumenMath.LINK_RANGE;
        var result = new ArrayList<Entry>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                var bucket = buckets.get(ChunkPos.asLong(chunkX + dx, chunkZ + dz));
                if (bucket == null) continue;
                for (int dy = -1; dy <= 1; dy++) {
                    var section = bucket.get(sectionY + dy);
                    if (section == null) continue;
                    for (var entry : section.values()) {
                        if (entry.pos.equals(pos)) continue;
                        long ox = entry.pos.getX() - pos.getX();
                        long oy = entry.pos.getY() - pos.getY();
                        long oz = entry.pos.getZ() - pos.getZ();
                        if (ox * ox + oy * oy + oz * oz <= rangeSquared) result.add(entry);
                    }
                }
            }
        }
        return result;
    }

    /** Network entries within link range of a changed block; used to invalidate topology on block changes. */
    public List<BlockPos> entriesNear(BlockPos pos) {
        var result = new ArrayList<BlockPos>();
        for (var entry : neighbors(pos)) result.add(entry.pos);
        return result;
    }

    /** Positions of all entries (including placeholders) inside one chunk; used to revalidate after chunk load. */
    public List<BlockPos> entriesInChunk(long chunkKey) {
        var bucket = buckets.get(chunkKey);
        if (bucket == null) return List.of();
        var result = new ArrayList<BlockPos>();
        for (var section : bucket.values()) result.addAll(section.keySet());
        return List.copyOf(result);
    }

    // ---- source data / proximity ----

    public double proximityFactorAt(BlockPos pos) {
        var source = sourceData.get(pos);
        return source != null ? source.proximityFactor() : 1;
    }

    Set<BlockPos> sourcePositions() { return sourceData.keySet(); }

    void updateSourceProximity(BlockPos pos, double factor) {
        var source = sourceData.get(pos);
        if (source == null || source.proximityFactor() == factor) return;
        sourceData.put(pos, source.withProximity(factor));
        requestPersist();
    }

    // ---- persistence ----

    void requestPersist() { persistRequested = true; }

    public void persistIfNeeded() {
        if (!persistRequested) return;
        persistRequested = false;
        data.store(snapshot());
    }

    public LumenNetworkData.LumenNetworkState snapshot() {
        var sources = new ArrayList<LumenNetworkData.SavedSource>();
        sourceData.forEach((pos, source) -> sources.add(new LumenNetworkData.SavedSource(
                pos.asLong(), source.providerId().toString(), source.baseOutput(), source.sign().map(ResourceLocation::toString),
                source.autoLink(), source.seesSky(), source.enhanced(), source.proximityFactor(), source.noiseFactor())));
        var nodes = new ArrayList<LumenNetworkData.SavedNode>();
        float loss = (float) OmphalosConfig.SERVER.decimal("gameplay.lumenLossPerHop");
        for (var bucket : buckets.values())
            for (var section : bucket.values())
                for (var entry : section.values()) {
                    long stored = entry.stored;
                    long capacity = entry.capacity;
                    if (entry.node instanceof LumenSink sink) {
                        stored = sink.lumenStored();
                        capacity = sink.lumenCapacity();
                        entry.stored = stored;
                        entry.capacity = capacity;
                    }
                    var source = sourceData.get(entry.pos);
                    nodes.add(new LumenNetworkData.SavedNode(entry.pos.asLong(), entry.io.name(),
                            source != null ? source.providerId().toString() : NODE_PROVIDER, stored, capacity, loss));
                }
        return new LumenNetworkData.LumenNetworkState(sources, nodes);
    }

    private void restore(LumenNetworkData.LumenNetworkState state) {
        for (var saved : state.sources()) {
            var provider = ResourceLocation.tryParse(saved.provider());
            if (provider == null) {
                LOGGER.warn("Skipping lumen source with invalid provider {} at {}", saved.provider(), saved.pos());
                continue;
            }
            var pos = BlockPos.of(saved.pos());
            sourceData.put(pos, new LumenSourceData(provider, saved.output(),
                    saved.sign().map(ResourceLocation::tryParse), saved.autoLink(), saved.seesSky(),
                    saved.enhanced(), saved.proximity(), saved.noise()));
        }
        for (var saved : state.nodes()) {
            LumenIO io;
            try {
                io = LumenIO.valueOf(saved.io());
            } catch (IllegalArgumentException exception) {
                LOGGER.warn("Skipping lumen node with unknown side {} at {}", saved.io(), saved.pos());
                continue;
            }
            if (ResourceLocation.tryParse(saved.provider()) == null) {
                LOGGER.warn("Skipping lumen node with invalid provider {} at {}", saved.provider(), saved.pos());
                continue;
            }
            var pos = BlockPos.of(saved.pos());
            long chunkKey = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
            var bucket = buckets.computeIfAbsent(chunkKey, key -> new HashMap<>());
            var section = bucket.computeIfAbsent(LumenMath.sectionY(pos.getY(), level.getMinBuildHeight()), key -> new LinkedHashMap<>());
            if (section.containsKey(pos)) continue;
            var entry = new Entry(pos, io);
            entry.stored = Math.max(0, Math.min(saved.stored(), saved.capacity()));
            entry.capacity = saved.capacity();
            section.put(pos, entry);
            nodeCount++;
        }
    }

    // ---- client announcements ----

    List<BlockPos> drainAnnouncements() {
        if (announceQueue.isEmpty()) return List.of();
        var drained = List.copyOf(announceQueue);
        announceQueue.clear();
        return drained;
    }

    public Optional<LumenIO> ioAt(BlockPos pos) {
        var entry = entryAt(pos);
        return entry == null ? Optional.empty() : Optional.of(entry.io);
    }
}

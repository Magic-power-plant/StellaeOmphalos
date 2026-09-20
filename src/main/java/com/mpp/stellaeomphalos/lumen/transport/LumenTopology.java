package com.mpp.stellaeomphalos.lumen.transport;

import com.mpp.stellaeomphalos.Omphalos;
import com.mpp.stellaeomphalos.core.util.world.BlockChangeNotice;
import com.mpp.stellaeomphalos.core.util.world.ChunkSafeAccess;
import com.mpp.stellaeomphalos.lumen.capability.LumenNode;
import com.mpp.stellaeomphalos.network.toClient.PktLumenNode;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Topology coordinator: owns the per-dimension networks and the join/leave/rescan queues,
 * subscribes block-change notices and chunk events. Queue processing itself runs in
 * {@link LumenScheduler} under per-tick budgets. All queues are position-keyed and deduplicated.
 */
@Mod.EventBusSubscriber(modid = Omphalos.MODID)
public final class LumenTopology {
    private enum Op { JOIN, LEAVE, RESCAN, SOURCE_DIRTY }

    private static final class DimensionState {
        final ServerLevel level;
        final LumenNetwork network;
        final EnumMap<Op, LinkedHashSet<BlockPos>> queues = new EnumMap<>(Op.class);
        DimensionState(ServerLevel level) {
            this.level = level;
            network = new LumenNetwork(level, LumenNetworkData.get(level));
            for (var op : Op.values()) queues.put(op, new LinkedHashSet<>());
        }
        int enqueue(Op op, BlockPos pos) {
            queues.get(op).add(pos.immutable());
            return queues.get(op).size();
        }
    }

    private static final Map<ResourceKey<Level>, DimensionState> DIMENSIONS = new LinkedHashMap<>();
    private static final double ANNOUNCE_RANGE_SQ = 64 * 64;
    private static final long ANNOUNCE_INTERVAL = 40;
    private static final long PERSIST_INTERVAL = 100;
    private LumenTopology() {}

    /** The network of one dimension; created lazily (loading its SavedData) on first use. */
    public static LumenNetwork network(ServerLevel level) {
        return DIMENSIONS.computeIfAbsent(level.dimension(), key -> new DimensionState(level)).network;
    }

    public static void enqueueJoin(ServerLevel level, BlockPos pos) { enqueue(level, Op.JOIN, pos); }
    public static void enqueueLeave(ServerLevel level, BlockPos pos) {
        var existing = DIMENSIONS.get(level.dimension());
        if (existing != null) existing.network.markDead(pos);
        enqueue(level, Op.LEAVE, pos);
    }
    public static void enqueueRescan(ServerLevel level, BlockPos pos) { enqueue(level, Op.RESCAN, pos); }
    public static void markSourceDirty(ServerLevel level, BlockPos pos) { enqueue(level, Op.SOURCE_DIRTY, pos); }

    private static void enqueue(ServerLevel level, Op op, BlockPos pos) {
        DIMENSIONS.computeIfAbsent(level.dimension(), key -> new DimensionState(level)).enqueue(op, pos);
    }

    /** One server tick of budgeted work; called by {@link LumenScheduler} at ServerTickEvent END. */
    static void tick(MinecraftServer server, int topologyOps, int proximityOps, long routingSteps, long deadlineNanos) {
        int remainingOps = topologyOps;
        var iterator = DIMENSIONS.values().iterator();
        while (iterator.hasNext()) {
            var state = iterator.next();
            if (server.getLevel(state.level.dimension()) != state.level) {
                iterator.remove();
                continue;
            }
            if (System.nanoTime() >= deadlineNanos) return;
            remainingOps -= processQueues(state, remainingOps, deadlineNanos);
            state.network.routing().tick(proximityOps, routingSteps);
        }
        long tick = server.getTickCount();
        if (tick % ANNOUNCE_INTERVAL == 0) DIMENSIONS.values().forEach(LumenTopology::flushAnnouncements);
        if (tick % PERSIST_INTERVAL == 0) DIMENSIONS.values().forEach(state -> state.network.persistIfNeeded());
    }

    private static int processQueues(DimensionState state, int budget, long deadlineNanos) {
        int processed = 0;
        for (var op : Op.values()) {
            var queue = state.queues.get(op);
            var iterator = queue.iterator();
            while (iterator.hasNext() && processed < budget && System.nanoTime() < deadlineNanos) {
                var pos = iterator.next();
                iterator.remove();
                process(state, op, pos);
                processed++;
            }
            if (processed >= budget) return processed;
        }
        return processed;
    }

    private static void process(DimensionState state, Op op, BlockPos pos) {
        var network = state.network;
        switch (op) {
            case JOIN -> ChunkSafeAccess.entity(state.level, pos, BlockEntity.class)
                    .filter(LumenNode.class::isInstance).map(LumenNode.class::cast)
                    .ifPresent(state.network::join);
            case LEAVE -> network.leave(pos);
            case RESCAN -> network.rescan(pos);
            case SOURCE_DIRTY -> network.markSourceDirty(pos);
        }
    }

    private static void flushAnnouncements(DimensionState state) {
        var drained = state.network.drainAnnouncements();
        if (drained.isEmpty()) return;
        for (var pos : drained) {
            var io = state.network.ioAt(pos);
            if (io.isEmpty()) continue;
            var connections = state.network.connectionsOf(pos);
            var packet = new PktLumenNode(LumenSession.current(), pos, (byte) io.get().ordinal(), connections);
            LumenBroadcast.sendToNearby(state.level, pos, packet, ANNOUNCE_RANGE_SQ);
        }
    }

    // ---- event subscriptions ----

    @SubscribeEvent
    public static void blockChanged(BlockChangeNotice notice) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        var level = server.getLevel(notice.position().dimension());
        if (level == null) return;
        var state = DIMENSIONS.get(notice.position().dimension());
        if (state == null) return;
        var changed = notice.position().position();
        if (!level.hasChunkAt(changed)) return;
        state.enqueue(Op.RESCAN, changed);
        for (var pos : state.network.entriesNear(changed)) state.enqueue(Op.RESCAN, pos);
    }

    @SubscribeEvent
    public static void chunkLoaded(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        var state = DIMENSIONS.get(level.dimension());
        if (state == null) return;
        long chunkKey = event.getChunk().getPos().toLong();
        state.network.resumeChunk(chunkKey);
        for (var pos : state.network.entriesInChunk(chunkKey)) state.enqueue(Op.RESCAN, pos);
    }

    @SubscribeEvent
    public static void chunkUnloaded(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        var state = DIMENSIONS.get(level.dimension());
        if (state == null) return;
        state.network.suspendChunk(event.getChunk().getPos().toLong());
    }

    @SubscribeEvent
    public static void serverStopping(ServerStoppingEvent event) {
        DIMENSIONS.values().forEach(state -> state.network.persistIfNeeded());
        DIMENSIONS.clear();
    }
}

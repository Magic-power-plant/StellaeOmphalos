package com.mpp.stellaeomphalos.content.world;

import com.mpp.stellaeomphalos.content.world.capability.WorldCapabilities;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.concurrent.*;

/** Generation workers only enqueue coordinates. Runtime access is confined to drain(). */
public final class PendingGeodeRegistration {
    private record Entry(ResourceKey<Level> dimension, long position) {}

    private static final ConcurrentMap<Entry, Boolean> QUEUE = new ConcurrentHashMap<>();

    private PendingGeodeRegistration() {}

    public static void enqueue(ResourceKey<Level> dimension, BlockPos p) {
        if (QUEUE.size() < 65536) QUEUE.put(new Entry(dimension, p.asLong()), true);
    }

    public static int drain(ServerLevel level, int budget) {
        int processed = 0;
        for (var entry : QUEUE.keySet()) {
            if (processed >= budget) break;
            if (!entry.dimension().equals(level.dimension())) continue;
            var pos = BlockPos.of(entry.position());
            var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
            if (chunk == null) continue;
            QUEUE.remove(entry);
            if (level.getBlockState(pos).is(WorldContent.GEODE_ORE.get()))
                chunk.getCapability(WorldCapabilities.GEODES).ifPresent(i -> i.add(pos));
            processed++;
        }
        return processed;
    }

    public static void clear() {
        QUEUE.clear();
    }
}

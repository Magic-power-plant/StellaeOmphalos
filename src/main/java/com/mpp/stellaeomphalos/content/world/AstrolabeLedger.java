package com.mpp.stellaeomphalos.content.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/** Spatial buckets retain records, including unloaded structures. Nearest is globally exact. */
public final class AstrolabeLedger extends SavedData {
    private final Map<ResourceLocation, Map<Long, Set<Long>>> targets = new HashMap<>();

    public static AstrolabeLedger get(ServerLevel level) {
        return level.getDataStorage()
                .computeIfAbsent(
                        AstrolabeLedger::load, AstrolabeLedger::new, "stellaeomphalos_astrolabe");
    }

    private static long grid(BlockPos p) {
        return ChunkPos.asLong(p.getX() >> 8, p.getZ() >> 8);
    }

    public void mark(ResourceLocation target, BlockPos origin) {
        if (targets.computeIfAbsent(target, k -> new HashMap<>())
                .computeIfAbsent(grid(origin), k -> new HashSet<>())
                .add(origin.asLong())) setDirty();
    }

    public Optional<BlockPos> nearest(ResourceLocation target, Vec3 from, int radiusChunks) {
        if (radiusChunks < 0 || radiusChunks > 4096)
            throw new IllegalArgumentException("Search radius");
        var buckets = targets.getOrDefault(target, Map.of());
        double best = (double) radiusChunks * 16 * radiusChunks * 16;
        BlockPos found = null;
        var ordered =
                buckets.entrySet().stream()
                        .sorted(Comparator.comparingDouble(e -> lowerBound(e.getKey(), from)))
                        .toList();
        for (var bucket : ordered) {
            if (lowerBound(bucket.getKey(), from) > best) break;
            for (long encoded : bucket.getValue()) {
                var p = BlockPos.of(encoded);
                double dx = p.getX() - from.x, dz = p.getZ() - from.z, dist = dx * dx + dz * dz;
                if (dist < best || dist == best && (found == null || p.asLong() < found.asLong())) {
                    best = dist;
                    found = p;
                }
            }
        }
        return Optional.ofNullable(found);
    }

    private static double lowerBound(long grid, Vec3 p) {
        int x = ChunkPos.getX(grid) * 256, z = ChunkPos.getZ(grid) * 256;
        double dx = Math.max(0, Math.max(x - p.x, p.x - (x + 255))),
                dz = Math.max(0, Math.max(z - p.z, p.z - (z + 255)));
        return dx * dx + dz * dz;
    }

    public int size() {
        return targets.values().stream()
                .flatMap(m -> m.values().stream())
                .mapToInt(Set::size)
                .sum();
    }

    public CompoundTag save(CompoundTag n) {
        n.putInt("Version", 1);
        var list = new ListTag();
        targets.forEach(
                (id, buckets) -> {
                    var entry = new CompoundTag();
                    entry.putString("Target", id.toString());
                    entry.putLongArray(
                            "Positions",
                            buckets.values().stream()
                                    .flatMap(Set::stream)
                                    .mapToLong(Long::longValue)
                                    .toArray());
                    list.add(entry);
                });
        n.put("Targets", list);
        return n;
    }

    public static AstrolabeLedger load(CompoundTag n) {
        var ledger = new AstrolabeLedger();
        for (var value : n.getList("Targets", Tag.TAG_COMPOUND)) {
            var entry = (CompoundTag) value;
            var id = ResourceLocation.tryParse(entry.getString("Target"));
            if (id != null)
                for (long p : entry.getLongArray("Positions")) ledger.mark(id, BlockPos.of(p));
        }
        ledger.setDirty(false);
        return ledger;
    }
}

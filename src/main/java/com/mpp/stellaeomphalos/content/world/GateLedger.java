package com.mpp.stellaeomphalos.content.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

/** Dimension-local gate identities survive chunk unload. Broken loaded entries self-heal. */
public final class GateLedger extends SavedData {
    private final Set<Long> positions = new HashSet<>();

    public static GateLedger get(ServerLevel level) {
        return level.getDataStorage()
                .computeIfAbsent(GateLedger::load, GateLedger::new, "stellaeomphalos_gates");
    }

    public void add(BlockPos pos) {
        if (positions.add(pos.asLong())) setDirty();
    }

    public void remove(BlockPos pos) {
        if (positions.remove(pos.asLong())) setDirty();
    }

    public boolean contains(BlockPos pos) {
        return positions.contains(pos.asLong());
    }

    public List<BlockPos> destinations(ServerLevel level, BlockPos origin) {
        boolean removed =
                positions.removeIf(
                        encoded -> {
                            var p = BlockPos.of(encoded);
                            return level.hasChunkAt(p)
                                    && !level.getBlockState(p)
                                            .is(WorldContent.BLOCKS.get("gate_core").get());
                        });
        if (removed) setDirty();
        return positions.stream()
                .map(BlockPos::of)
                .filter(p -> !p.equals(origin))
                .sorted(Comparator.comparingDouble(origin::distSqr))
                .limit(64)
                .toList();
    }

    public CompoundTag save(CompoundTag tag) {
        tag.putInt("Version", 1);
        tag.putLongArray("Gates", positions.stream().sorted().mapToLong(Long::longValue).toArray());
        return tag;
    }

    private static GateLedger load(CompoundTag tag) {
        var ledger = new GateLedger();
        for (long pos : tag.getLongArray("Gates")) ledger.positions.add(pos);
        return ledger;
    }
}

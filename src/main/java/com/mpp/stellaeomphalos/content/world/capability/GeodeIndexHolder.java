package com.mpp.stellaeomphalos.content.world.capability;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.*;

public final class GeodeIndexHolder implements INBTSerializable<CompoundTag> {
    private final Set<Long> positions = new HashSet<>();
    private boolean dirty = true;
    private Runnable changed = () -> {};

    public void onChanged(Runnable callback) {
        changed = callback;
    }

    public void add(BlockPos p) {
        if (positions.add(p.asLong())) changed.run();
    }

    public void remove(BlockPos p) {
        if (positions.remove(p.asLong())) changed.run();
    }

    public List<BlockPos> positions() {
        return positions.stream().map(BlockPos::of).toList();
    }

    public boolean dirty() {
        return dirty;
    }

    public void clean() {
        dirty = false;
        changed.run();
    }

    public CompoundTag serializeNBT() {
        var n = new CompoundTag();
        n.putInt("Version", 1);
        n.putLongArray("Positions", positions.stream().mapToLong(Long::longValue).toArray());
        n.putBoolean("Dirty", dirty);
        return n;
    }

    public void deserializeNBT(CompoundTag n) {
        positions.clear();
        for (long p : n.getLongArray("Positions")) if (positions.size() < 65536) positions.add(p);
        dirty = !n.contains("Dirty") || n.getBoolean("Dirty");
    }
}

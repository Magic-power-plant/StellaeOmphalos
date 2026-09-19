package com.mpp.stellaeomphalos.core.util.world;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/** Dimension-owned progress with an inactivity timeout. Caller owns block authorization and crack packets. */
public final class ProgressBreakTracker {
    private record Progress(double amount, long touched) {}
    private final Map<BlockPos, Progress> progress = new HashMap<>();
    public boolean advance(BlockPos position, double amount, long tick) {
        if (!Double.isFinite(amount) || amount < 0) throw new IllegalArgumentException("Invalid break progress");
        var old = progress.getOrDefault(position, new Progress(0, tick));
        double next = Math.min(1, old.amount + amount);
        if (next >= 1) { progress.remove(position); return true; }
        progress.put(position.immutable(), new Progress(next, tick)); return false;
    }
    public void expire(long tick, long timeout) {
        if (timeout < 1) throw new IllegalArgumentException("Invalid break timeout");
        progress.values().removeIf(value -> tick - value.touched >= timeout);
    }
    public CompoundTag save() {
        var tag = new CompoundTag(); var entries = new ListTag();
        progress.forEach((pos, value) -> {
            var entry = new CompoundTag(); entry.putLong("Position", pos.asLong()); entry.putDouble("Progress", value.amount); entries.add(entry);
        });
        tag.put("Entries", entries); return tag;
    }
    public void restore(CompoundTag tag, long tick) {
        var candidate = new HashMap<BlockPos, Progress>();
        for (Tag saved : tag.getList("Entries", Tag.TAG_COMPOUND)) {
            var entry = (CompoundTag) saved; double value = entry.getDouble("Progress");
            if (!entry.contains("Position", Tag.TAG_LONG) || !Double.isFinite(value) || value < 0 || value >= 1) throw new IllegalArgumentException("Invalid saved break progress");
            candidate.put(BlockPos.of(entry.getLong("Position")), new Progress(value, tick));
        }
        progress.clear(); progress.putAll(candidate);
    }
    public void clear() { progress.clear(); }
}

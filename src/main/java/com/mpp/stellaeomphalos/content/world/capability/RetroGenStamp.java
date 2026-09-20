package com.mpp.stellaeomphalos.content.world.capability;

import com.mpp.stellaeomphalos.content.world.WorldGenPhase;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

public final class RetroGenStamp implements INBTSerializable<CompoundTag> {
    private int stamp, version = 1;
    private long seedHash;
    private boolean terrainPopulated;
    private Runnable changed = () -> {};

    public void onChanged(Runnable callback) {
        changed = callback;
    }

    public int stamp() {
        return stamp;
    }

    public boolean missing(WorldGenPhase phase) {
        return (stamp & phase.bit()) == 0;
    }

    public void complete(WorldGenPhase phase) {
        stamp |= phase.bit();
        changed.run();
    }

    public void reset() {
        stamp = 0;
        changed.run();
    }

    public void ready(long seed) {
        long mixed = SpringVeinHolder.mix64(seed);
        if (seedHash != 0 && seedHash != mixed) stamp = 0;
        seedHash = mixed;
        terrainPopulated = true;
        changed.run();
    }

    public boolean terrainPopulated() {
        return terrainPopulated;
    }

    public CompoundTag serializeNBT() {
        var n = new CompoundTag();
        n.putInt("Version", 1);
        n.putInt("Stamp", stamp);
        n.putInt("Generator", version);
        n.putLong("SeedHash", seedHash);
        n.putBoolean("TerrainPopulated", terrainPopulated);
        return n;
    }

    public void deserializeNBT(CompoundTag n) {
        stamp = n.getInt("Stamp");
        version = n.getInt("Generator");
        seedHash = n.getLong("SeedHash");
        terrainPopulated = n.getBoolean("TerrainPopulated");
    }
}

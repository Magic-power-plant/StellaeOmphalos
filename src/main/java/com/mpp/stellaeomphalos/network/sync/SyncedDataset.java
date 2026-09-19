package com.mpp.stellaeomphalos.network.sync;

import com.mpp.stellaeomphalos.network.toClient.PktSyncDataset;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Owns version and dirty lifecycle; capabilities and authorization remain domain-specific. */
public abstract class SyncedDataset {
    private final ResourceLocation id;
    private long version;
    private boolean dirty = true;
    protected SyncedDataset(ResourceLocation id) { this.id = id; }
    public final ResourceLocation id() { return id; }
    public final long version() { return version; }
    public final boolean dirty() { return dirty; }
    public final void markDirty() { dirty = true; }
    public abstract boolean visibleTo(ServerPlayer player);
    protected abstract CompoundTag snapshot(ServerPlayer player);
    public final PktSyncDataset fullSnapshot(ServerPlayer player) {
        if (!visibleTo(player)) throw new IllegalArgumentException("Dataset access denied");
        return new PktSyncDataset(id, version, true, snapshot(player));
    }
    public final void commitTick() { if (dirty) { version = Math.incrementExact(version); dirty = false; } }
}

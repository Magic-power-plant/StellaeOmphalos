package com.mpp.stellaeomphalos.network.sync;

import com.mpp.stellaeomphalos.OmphalosConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** The server config page is an actual consumer of versioned full/delta synchronization. */
public final class ServerConfigDataset extends SyncedDataset {
    public static final ResourceLocation ID = new ResourceLocation("stellaeomphalos", "server_config");
    private int revision = -1;
    public ServerConfigDataset() { super(ID); }
    public void refresh() {
        int next = OmphalosConfig.serverRevision();
        if (next != revision) { revision = next; markDirty(); }
    }
    @Override public boolean visibleTo(ServerPlayer player) { return true; }
    @Override protected CompoundTag snapshot(ServerPlayer player) {
        var data = new CompoundTag();
        var json = new com.google.gson.Gson();
        OmphalosConfig.SERVER.snapshot().forEach((key, value) -> data.putString(key, json.toJson(value)));
        return data;
    }
}

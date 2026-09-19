package com.mpp.stellaeomphalos.network.sync;

import com.mpp.stellaeomphalos.network.OmphalosChannel;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Server-session-owned subscriptions; multiple writes coalesce into one update per tick. */
public final class SyncDispatcher {
    private final Map<ResourceLocation, SyncedDataset> datasets = new LinkedHashMap<>();
    private final Map<java.util.UUID, Map<ResourceLocation, com.mpp.stellaeomphalos.network.toClient.PktSyncDataset>> previous = new LinkedHashMap<>();
    public void register(SyncedDataset dataset) {
        if (datasets.putIfAbsent(dataset.id(), dataset) != null) throw new IllegalArgumentException("Duplicate dataset");
    }
    public void flush(List<ServerPlayer> players) {
        for (var dataset : datasets.values()) {
            boolean updated = dataset.dirty();
            if (updated) dataset.commitTick();
            for (var player : players) {
                var known = previous.computeIfAbsent(player.getUUID(), ignored -> new LinkedHashMap<>());
                if (dataset.visibleTo(player)) {
                    if (updated || !known.containsKey(dataset.id())) send(player, dataset.fullSnapshot(player), !updated);
                } else if (known.remove(dataset.id()) != null) {
                    OmphalosChannel.send(player, new com.mpp.stellaeomphalos.network.toClient.PktSyncDataset(dataset.id(), dataset.version(), true, new net.minecraft.nbt.CompoundTag()));
                }
            }
        }
    }
    public void synchronize(ServerPlayer player) {
        datasets.values().stream().filter(dataset -> dataset.visibleTo(player))
                .forEach(dataset -> send(player, dataset.fullSnapshot(player), true));
    }
    public void request(ServerPlayer player, ResourceLocation id) {
        var dataset = datasets.get(id);
        if (dataset != null && dataset.visibleTo(player)) send(player, dataset.fullSnapshot(player), true);
    }
    private void send(ServerPlayer player, com.mpp.stellaeomphalos.network.toClient.PktSyncDataset current, boolean forceFull) {
        var known = previous.computeIfAbsent(player.getUUID(), ignored -> new LinkedHashMap<>());
        var prior = known.get(current.dataset());
        if (!forceFull && prior != null && prior.version() + 1 == current.version()) {
            OmphalosChannel.send(player, new com.mpp.stellaeomphalos.network.toClient.PktSyncDataset(current.dataset(), current.version(), false,
                    difference(prior.data(), current.data())));
        } else OmphalosChannel.send(player, current);
        known.put(current.dataset(), current);
    }
    public static net.minecraft.nbt.CompoundTag difference(net.minecraft.nbt.CompoundTag before, net.minecraft.nbt.CompoundTag after) {
        var patch = new net.minecraft.nbt.CompoundTag(); var updates = new net.minecraft.nbt.CompoundTag();
        var removed = new net.minecraft.nbt.ListTag();
        for (String key : after.getAllKeys()) if (!java.util.Objects.equals(before.get(key), after.get(key))) updates.put(key, after.get(key).copy());
        for (String key : before.getAllKeys()) if (!after.contains(key)) removed.add(net.minecraft.nbt.StringTag.valueOf(key));
        patch.put("Set", updates); patch.put("Removed", removed); return patch;
    }
    public void remove(java.util.UUID player) { previous.remove(player); }
    public void clear() { datasets.clear(); previous.clear(); }
}

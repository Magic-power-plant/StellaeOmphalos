package com.mpp.stellaeomphalos.network.sync;

import com.mpp.stellaeomphalos.network.toClient.PktSyncDataset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

/** Connection-local mirror. Returns false when a full resynchronization is required. */
public final class MirrorDataHub {
    private record Entry(long version, CompoundTag data) {}
    private final Map<ResourceLocation, Entry> entries = new LinkedHashMap<>();
    public boolean apply(PktSyncDataset packet) {
        var previous = entries.get(packet.dataset());
        if (previous != null && (packet.version() < previous.version() || !packet.fullSnapshot() && packet.version() == previous.version())) return true;
        if (!packet.fullSnapshot() && (previous == null || packet.version() != previous.version() + 1)) return false;
        CompoundTag next;
        if (packet.fullSnapshot()) next = packet.data();
        else {
            next = previous.data.copy();
            var patch = packet.data();
            var updates = patch.getCompound("Set");
            for (String key : updates.getAllKeys()) next.put(key, updates.get(key).copy());
            for (Tag removed : patch.getList("Removed", Tag.TAG_STRING)) next.remove(removed.getAsString());
        }
        entries.put(packet.dataset(), new Entry(packet.version(), next));
        return true;
    }
    public Optional<CompoundTag> snapshot(ResourceLocation id) { return Optional.ofNullable(entries.get(id)).map(entry -> entry.data.copy()); }
    public long version(ResourceLocation id) { return entries.containsKey(id) ? entries.get(id).version : 0; }
    public void clear() { entries.clear(); }
}

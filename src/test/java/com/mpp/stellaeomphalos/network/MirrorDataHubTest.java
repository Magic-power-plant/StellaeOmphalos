package com.mpp.stellaeomphalos.network;

import com.mpp.stellaeomphalos.network.sync.MirrorDataHub;
import com.mpp.stellaeomphalos.network.toClient.PktSyncDataset;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MirrorDataHubTest {
    @Test void versionsAndExplicitDeletionPreventStaleMirrors() {
        var hub = new MirrorDataHub(); var id = new ResourceLocation("stellaeomphalos", "test");
        var initial = new CompoundTag(); initial.putInt("Value", 7);
        assertTrue(hub.apply(new PktSyncDataset(id, 2, true, initial)));
        initial.putInt("Value", 99); assertEquals(7, hub.snapshot(id).orElseThrow().getInt("Value"));
        assertFalse(hub.apply(new PktSyncDataset(id, 4, false, new CompoundTag())));
        assertEquals(2, hub.version(id));
        var patch = new CompoundTag(); var removed = new ListTag(); removed.add(StringTag.valueOf("Value")); patch.put("Removed", removed);
        assertTrue(hub.apply(new PktSyncDataset(id, 3, false, patch)));
        assertFalse(hub.snapshot(id).orElseThrow().contains("Value"));
        var next = new CompoundTag(); next.putString("Name", "new");
        var delta = com.mpp.stellaeomphalos.network.sync.SyncDispatcher.difference(hub.snapshot(id).orElseThrow(), next);
        assertTrue(hub.apply(new PktSyncDataset(id, 4, false, delta)));
        assertEquals(next, hub.snapshot(id).orElseThrow());
        hub.clear(); assertTrue(hub.snapshot(id).isEmpty());
    }
}

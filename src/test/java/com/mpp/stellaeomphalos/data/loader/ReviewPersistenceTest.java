package com.mpp.stellaeomphalos.data.loader;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mpp.stellaeomphalos.lumen.transport.stasis.StasisData;
import com.mpp.stellaeomphalos.constellation.domain.StarStructureLedger;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.profiling.InactiveProfiler;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class ReviewPersistenceTest {
    @Test void directoryReloadRejectsPartialSnapshotsButAcceptsDeletion() {
        var id = new ResourceLocation("test", "entry");
        var current = new AtomicReference<Map<ResourceLocation, Integer>>(Map.of(id, 7));
        var loader = new CodecDirectoryLoader<Integer>("test", Codec.INT, current::set);
        loader.apply(Map.of(id, JsonParser.parseString("8"), new ResourceLocation("test", "broken"),
                JsonParser.parseString("\"bad\"")), null, InactiveProfiler.INSTANCE);
        assertEquals(Map.of(id, 7), current.get());
        loader.apply(Map.of(), null, InactiveProfiler.INSTANCE);
        assertTrue(current.get().isEmpty());
    }
    @Test void corruptedAndFutureDomainSavesKeepEvidenceAndRejectWrites() {
        for (var data : List.<VersionedSavedData<?>>of(new StasisData(), new StarStructureLedger())) {
            var input = data.save(new CompoundTag());
            input.putInt("dataVersion", 999);
            var original = input.copy();
            assertDoesNotThrow(() -> data.restoreOrPreserve(input, 3));
            assertTrue(data.readOnly());
            input.putString("Tamper", "external");
            data.setDirty();
            assertEquals(original, data.save(new CompoundTag()));
        }
        var data = new StasisData();
        var invalid = data.save(new CompoundTag());
        invalid.getCompound("payload").putString("Zones", "invalid");
        data.restoreOrPreserve(invalid, 0);
        data.store(new StasisData.State(List.of()));
        assertEquals(invalid, data.save(new CompoundTag()));
        MigrationReports.drain();
    }
}

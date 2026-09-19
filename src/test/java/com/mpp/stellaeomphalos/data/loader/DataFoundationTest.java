package com.mpp.stellaeomphalos.data.loader;

import com.mojang.serialization.Codec;
import com.mpp.stellaeomphalos.data.codec.PolicyEntry;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DataFoundationTest {
    private static final ResourceLocation ID = new ResourceLocation("stellaeomphalos", "test");
    @Test void tenBadResourcesProduceTenLocatedErrorsWithoutEscaping() {
        var table = new DataTable<>(ID, PolicyEntry.CODEC, (entries, report) -> {});
        var report = new DataLoadReport();
        for (int i = 0; i < 10; i++) {
            String json = i % 2 == 0 ? "{" : "{\"schema_version\":99}";
            assertTrue(DataTableLoader.decode(table, "file" + i + ".json", new StringReader(json), report).isEmpty());
        }
        assertEquals(10, report.issues().size());
        for (int i = 0; i < 10; i++) { assertEquals("file" + i + ".json", report.issues().get(i).file()); assertFalse(report.issues().get(i).pointer().isBlank()); }
    }
    @Test void failedReloadKeepsPreviousSnapshot() {
        var registry = new DataTableRegistry(); var table = registry.declare(new DataTable<>(ID, Codec.INT, (entries, report) -> {}));
        registry.publish(Map.of(ID, Map.of(ID, 7)), new DataLoadReport());
        var report = new DataLoadReport(); report.error("broken.json", "$.value", "Invalid value");
        registry.publish(Map.of(ID, Map.of(ID, 9)), report);
        assertEquals(7, registry.entries(table).get(ID)); assertEquals(1, registry.report().size());
        registry.clearSession(); assertTrue(registry.entries(table).isEmpty());
    }
    @Test void malformedOptionalFieldsAndDeepJsonAreRejected() {
        var table = new DataTable<>(ID, PolicyEntry.CODEC, (entries, report) -> {});
        for (String json : List.of("{\"schema_version\":1,\"enabled\":\"bad\"}",
                "{\"schema_version\":1,\"values\":[-1]}", "{\"schema_version\":1,\"schema_version\":1}",
                "[".repeat(100) + "0" + "]".repeat(100), "{schema_version:1}")) {
            var report = new DataLoadReport();
            assertTrue(DataTableLoader.decode(table, "invalid.json", new StringReader(json), report).isEmpty());
            assertEquals(1, report.issues().size());
        }
    }
    @Test void migratesEveryStepWithoutMutatingOriginal() {
        var chain = new MigrationChain("test", 2, List.of(step(0), step(1)));
        var input = header(0); input.getCompound("payload").putString("Name", "preserved");
        var output = chain.apply(input, 42);
        assertEquals(2, output.getInt("dataVersion")); assertEquals(42, output.getLong("migratedAt"));
        assertEquals(2, output.getCompound("payload").getInt("Count"));
        assertEquals("preserved", output.getCompound("payload").getString("Name"));
        assertEquals(0, input.getInt("dataVersion")); assertFalse(input.getCompound("payload").contains("Count"));
    }
    @Test void futureVersionDomainMismatchAndMigrationGapsPreserveInput() {
        var chain = new MigrationChain("test", 2, List.of(step(0)));
        var input = header(0); var before = input.copy();
        assertThrows(IllegalStateException.class, () -> chain.apply(input, 0)); assertEquals(before, input);
        assertThrows(IllegalArgumentException.class, () -> chain.apply(header(3), 0));
        input.putString("domain", "wrong"); assertThrows(IllegalArgumentException.class, () -> chain.apply(input, 0));
    }
    private static CompoundTag header(int version) {
        var tag = new CompoundTag(); tag.putInt("dataVersion", version); tag.putString("domain", "test"); tag.put("payload", new CompoundTag()); return tag;
    }
    private static DataMigrator step(int from) {
        return new DataMigrator() {
            public int fromVersion() { return from; }
            public CompoundTag migrate(CompoundTag payload) { payload.putInt("Count", payload.getInt("Count") + 1); return payload; }
        };
    }
}

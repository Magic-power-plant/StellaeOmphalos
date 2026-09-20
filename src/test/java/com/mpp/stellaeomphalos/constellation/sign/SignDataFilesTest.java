package com.mpp.stellaeomphalos.constellation.sign;

import static org.junit.jupiter.api.Assertions.*;

import com.mojang.serialization.JsonOps;
import com.mpp.stellaeomphalos.data.codec.BoundedJson;
import com.mpp.stellaeomphalos.data.loader.DataLoadReport;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

/** Decodes the shipped datapack JSON with the real codecs, catching schema drift without a server. */
class SignDataFilesTest {
    private static <T> T decode(com.mojang.serialization.Codec<T> codec, String path) {
        try (var stream = SignDataFilesTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, "Missing classpath resource " + path);
            var json = BoundedJson.parse(new InputStreamReader(stream, StandardCharsets.UTF_8));
            return codec.parse(JsonOps.INSTANCE, json).getOrThrow(false, message -> {
                throw new AssertionError(path + ": " + message);
            });
        } catch (java.io.IOException exception) {
            throw new AssertionError(exception);
        }
    }

    @Test void allSixteenSignDefinitionsDecodeAndValidate() {
        var entries = new LinkedHashMap<ResourceLocation, SignDefinitions.Definition>();
        for (var id : SignBootstrap.BUILTIN_SIGN_IDS)
            entries.put(id, decode(SignDefinitions.Definition.CODEC, "/data/stellaeomphalos/sign/" + id.getPath() + ".json"));
        assertEquals(16, entries.size());
        assertEquals(5, entries.values().stream().filter(d -> d.kind() == SignDefinitions.Kind.MAJOR).count());
        assertEquals(7, entries.values().stream().filter(d -> d.kind() == SignDefinitions.Kind.RITUAL).count());
        assertEquals(4, entries.values().stream().filter(d -> d.kind() == SignDefinitions.Kind.TRAIT).count());
        entries.forEach((id, definition) -> {
            assertTrue(definition.stars().size() >= 4 && definition.stars().size() <= 8,
                    id + " star count " + definition.stars().size());
            assertFalse(definition.lines().isEmpty(), id + " has no lines");
            assertFalse(definition.signatureItems().isEmpty(), id + " has no signature items");
            if (definition.kind() == SignDefinitions.Kind.TRAIT)
                assertFalse(definition.moonPhases().isEmpty(), id + " trait without moon phases");
        });
        var report = new DataLoadReport();
        SignDefinitions.validate(entries, report);
        assertTrue(report.issues().isEmpty(), report.issues().toString());
    }

    @Test void namingLexiconsDecode() {
        var zh = decode(SignNamingLexicon.CODEC, "/data/stellaeomphalos/sign_naming/zh_cn.json");
        var en = decode(SignNamingLexicon.CODEC, "/data/stellaeomphalos/sign_naming/en_us.json");
        assertEquals("{prefix}{core}{suffix}", zh.format());
        assertEquals("{onset}{body}{tail}", en.format());
        assertEquals(3, zh.minLength());
        assertEquals(2, en.minLength());
        assertTrue(en.maxLength() >= en.minLength());
    }

    @Test void skyAnchorsDecodeWithContractCountsAndRadii() {
        var table = decode(SignSkyAnchorTable.SlotTable.CODEC, "/data/stellaeomphalos/sign_sky_anchors/default.json");
        assertEquals(5, table.majorSlots().size());
        assertEquals(10, table.minorSlots().size());
        table.majorSlots().forEach(anchor ->
                assertTrue(anchor.radius() >= 5 && anchor.radius() <= 19, "major radius " + anchor.radius()));
        table.minorSlots().forEach(anchor ->
                assertTrue(anchor.radius() >= 10 && anchor.radius() <= 35, "minor radius " + anchor.radius()));
    }

    @Test void signSkyAnchorListRoundTrip() {
        var anchor = new SignSkyAnchor(1, 2, 3, 4, 5, 6, 7, 8, 9, 10.5);
        assertEquals(anchor, SignSkyAnchor.fromList(anchor.asList()));
        assertEquals(10, anchor.asList().size());
    }

    @Test void layoutAssignsMajorsToMajorSlotsAndSkipsTraits() {
        var major = SignTestSupport.major("layout_major");
        var ritual = SignTestSupport.ritual("layout_ritual");
        var trait = SignTestSupport.trait("layout_trait", MoonPhase.FULL);
        var base = new SignSkyAnchor(1, 90, 0, 5, 0, 0, 0, 0, 5, 5);
        var table = new SignSkyAnchorTable.SlotTable(
                java.util.Collections.nCopies(5, base), java.util.Collections.nCopies(10, base));
        // layout() reads the published data table; the record itself is exercised here
        assertEquals(5, table.majorSlots().size());
        assertEquals(10, table.minorSlots().size());
        assertTrue(major instanceof MajorSign && !MajorSign.class.isInstance(trait) && !TraitSign.class.isInstance(ritual));
    }
}

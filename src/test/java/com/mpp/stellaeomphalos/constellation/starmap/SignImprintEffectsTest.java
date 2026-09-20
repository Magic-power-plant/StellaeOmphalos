package com.mpp.stellaeomphalos.constellation.starmap;

import static org.junit.jupiter.api.Assertions.*;

import com.mpp.stellaeomphalos.constellation.sign.AbstractSign;
import com.mpp.stellaeomphalos.constellation.sign.SignBootstrap;
import com.mpp.stellaeomphalos.constellation.sign.SignRegistry;
import com.mpp.stellaeomphalos.data.loader.DataLoadReport;
import com.mpp.stellaeomphalos.data.loader.DataTableLoader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantments;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** JSON decode, validation error paths, and rebuild/query consistency of the imprint effect table. */
class SignImprintEffectsTest {
    private static final ResourceLocation TEST_SIGN = new ResourceLocation("stellaeomphalos", "imprint_fx_test");
    private static final ResourceLocation UNREGISTERED = new ResourceLocation("stellaeomphalos", "imprint_fx_absent");

    /**
     * Vanilla registry contents without the Forge networking hooks that full
     * {@code Bootstrap.bootStrap()} pulls in (they fail in a plain JUnit JVM): flip the bootstrap
     * guard, then let {@code BuiltInRegistries} populate and freeze itself.
     */
    @BeforeAll static void bootstrapRegistries() {
        try {
            var guard = net.minecraft.server.Bootstrap.class.getDeclaredField("isBootstrapped");
            guard.setAccessible(true);
            if (!guard.getBoolean(null)) {
                net.minecraft.SharedConstants.tryDetectVersion();
                guard.setBoolean(null, true);
                net.minecraft.core.registries.BuiltInRegistries.bootStrap();
            }
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private static java.util.Optional<SignImprintEffects.Entry> decode(String json, DataLoadReport report) {
        return DataTableLoader.decode(StarmapBootstrap.SIGN_IMPRINT_EFFECTS, "test.json", new StringReader(json), report);
    }

    @Test void shippedFilesDecodeAndValidateCleanly() {
        var entries = new LinkedHashMap<ResourceLocation, SignImprintEffects.Entry>();
        for (var id : SignBootstrap.BUILTIN_SIGN_IDS) {
            String path = "/data/stellaeomphalos/sign_imprint_effects/" + id.getPath() + ".json";
            try (var stream = SignImprintEffectsTest.class.getResourceAsStream(path)) {
                assertNotNull(stream, "Missing classpath resource " + path);
                var report = new DataLoadReport();
                var entry = DataTableLoader.decode(StarmapBootstrap.SIGN_IMPRINT_EFFECTS, path,
                        new InputStreamReader(stream, StandardCharsets.UTF_8), report);
                assertTrue(entry.isPresent(), path + ": " + report.issues());
                entries.put(id, entry.get());
            } catch (java.io.IOException exception) {
                throw new AssertionError(exception);
            }
        }
        assertEquals(16, entries.size());
        var report = new DataLoadReport();
        SignImprintEffects.validate(entries, report);
        assertTrue(report.issues().isEmpty(), report.issues().toString());
    }

    @Test void decodeRejectsBadSchemaVersionAndLevelRanges() {
        for (String json : List.of(
                "{\"schema_version\":2,\"sign\":\"stellaeomphalos:aevitas\"}",
                "{\"schema_version\":1,\"sign\":\"stellaeomphalos:aevitas\",\"enchantments\":[{\"enchantment\":\"minecraft:sharpness\",\"min_level\":3,\"max_level\":1}]}",
                "{\"schema_version\":1,\"sign\":\"stellaeomphalos:aevitas\",\"enchantments\":[{\"enchantment\":\"minecraft:sharpness\",\"min_level\":0,\"max_level\":1}]}",
                "{\"schema_version\":1,\"sign\":\"stellaeomphalos:aevitas\",\"potions\":[{\"effect\":\"minecraft:strength\",\"min_level\":2,\"max_level\":1}]}")) {
            var report = new DataLoadReport();
            assertTrue(decode(json, report).isEmpty(), json);
            assertEquals(1, report.issues().size());
            assertTrue(report.hasErrors());
        }
    }

    @Test void decodeDefaultsOptionalSections() {
        var report = new DataLoadReport();
        var entry = decode("{\"schema_version\":1,\"sign\":\"stellaeomphalos:alcara\"}", report);
        assertTrue(entry.isPresent(), report.issues().toString());
        assertTrue(entry.get().enchantments().isEmpty());
        assertTrue(entry.get().potions().isEmpty());
        var full = decode("{\"schema_version\":1,\"sign\":\"stellaeomphalos:discidia\","
                + "\"enchantments\":[{\"enchantment\":\"minecraft:sharpness\",\"min_level\":1,\"max_level\":3}],"
                + "\"potions\":[{\"effect\":\"minecraft:strength\",\"min_level\":0,\"max_level\":1}]}", new DataLoadReport());
        assertTrue(full.isPresent());
        assertFalse(full.get().enchantments().get(0).ignoreCompatibility(), "ignore_compatibility defaults to false");
    }

    @Test void validatorRejectsUnknownIdsSignMismatchAndDuplicateSigns() {
        var file = new ResourceLocation("stellaeomphalos", "aevitas");
        var unknownEnchant = new SignImprintEffects.Entry(1, file,
                List.of(new SignImprintEffects.AffixSpec(new ResourceLocation("minecraft", "no_such_ench"), 1, 1, false)), List.of());
        var unknownEffect = new SignImprintEffects.Entry(1, file, List.of(),
                List.of(new SignImprintEffects.PotionSpec(new ResourceLocation("minecraft", "no_such_fx"), 0, 0)));
        var mismatch = new SignImprintEffects.Entry(1, new ResourceLocation("stellaeomphalos", "armara"), List.of(), List.of());
        var valid = new SignImprintEffects.Entry(1, file, List.of(), List.of());

        var report = new DataLoadReport();
        SignImprintEffects.validate(Map.of(file, unknownEnchant), report);
        assertTrue(report.hasErrors(), "unknown enchantment id must error");

        report = new DataLoadReport();
        SignImprintEffects.validate(Map.of(file, unknownEffect), report);
        assertTrue(report.hasErrors(), "unknown mob effect id must error");

        report = new DataLoadReport();
        SignImprintEffects.validate(Map.of(file, mismatch), report);
        assertTrue(report.hasErrors(), "sign field must match the file name");

        report = new DataLoadReport();
        SignImprintEffects.validate(Map.of(file, valid,
                new ResourceLocation("stellaeomphalos", "second_file"),
                new SignImprintEffects.Entry(1, file, List.of(), List.of())), report);
        assertTrue(report.hasErrors(), "two files declaring the same sign must error");
    }

    @Test void convertResolvesHoldersAndRejectsMissingIds() {
        var valid = new SignImprintEffects.Entry(1, TEST_SIGN,
                List.of(new SignImprintEffects.AffixSpec(new ResourceLocation("minecraft", "sharpness"), 1, 3, false)),
                List.of(new SignImprintEffects.PotionSpec(new ResourceLocation("minecraft", "strength"), 0, 1)));
        var converted = SignImprintEffects.convert(valid);
        assertNotNull(converted);
        assertSame(Enchantments.SHARPNESS, converted.enchantments().get(0).enchantment().value());
        var missing = new SignImprintEffects.Entry(1, TEST_SIGN, List.of(),
                List.of(new SignImprintEffects.PotionSpec(new ResourceLocation("minecraft", "no_such_fx"), 0, 0)));
        assertNull(SignImprintEffects.convert(missing));
    }

    @Test void rebuildDrivesQueriesAndDataWinsOverCodeDefaults() {
        SignRegistry.rebuild(List.of(
                (com.mpp.stellaeomphalos.constellation.sign.Sign) new AbstractSign.Major(TEST_SIGN, 0xFFFFFF, List.of()).seal()));
        try {
            var spec = new SignImprintEffects.Entry(1, TEST_SIGN,
                    List.of(new SignImprintEffects.AffixSpec(new ResourceLocation("minecraft", "sharpness"), 1, 3, false)),
                    List.of(new SignImprintEffects.PotionSpec(new ResourceLocation("minecraft", "strength"), 0, 1)));
            var absent = new SignImprintEffects.Entry(1, UNREGISTERED,
                    List.of(new SignImprintEffects.AffixSpec(new ResourceLocation("minecraft", "power"), 1, 3, false)), List.of());
            StarmapBootstrap.rebuildImprintEffects(Map.of(TEST_SIGN, spec, UNREGISTERED, absent));

            var enchants = SignImprintEffectRegistry.enchantments(TEST_SIGN);
            assertEquals(1, enchants.size());
            assertSame(Enchantments.SHARPNESS, enchants.get(0).enchantment().value());
            assertEquals(3, enchants.get(0).maxLevel());
            assertEquals(1, SignImprintEffectRegistry.potions(TEST_SIGN).size());
            assertTrue(SignImprintEffectRegistry.enchantments(UNREGISTERED).isEmpty(),
                    "entries for unregistered signs are dropped at rebuild");

            var codeDefault = List.of(new SignImprintEffectRegistry.AffixEntry(
                    net.minecraft.core.Holder.direct(Enchantments.MENDING), 1, 1, false));
            SignImprintEffectRegistry.register(UNREGISTERED, codeDefault, List.of());
            StarmapBootstrap.rebuildImprintEffects(Map.of(UNREGISTERED, absent));
            assertEquals(codeDefault, SignImprintEffectRegistry.enchantments(UNREGISTERED),
                    "dropped datapack entry falls back to the code-registered default");
            StarmapBootstrap.rebuildImprintEffects(Map.of(TEST_SIGN, spec));
            assertEquals(codeDefault, SignImprintEffectRegistry.enchantments(UNREGISTERED),
                    "code default survives wholesale data rebuilds");
            assertEquals(1, SignImprintEffectRegistry.enchantments(TEST_SIGN).size(),
                    "datapack entries are restored on rebuild");

            StarmapBootstrap.rebuildImprintEffects(Map.of());
            assertEquals(codeDefault, SignImprintEffectRegistry.enchantments(UNREGISTERED));
            assertTrue(SignImprintEffectRegistry.enchantments(TEST_SIGN).isEmpty(),
                    "wholesale rebuild drops stale datapack entries");
        } finally {
            SignImprintEffectRegistry.replaceDataDriven(Map.of());
            SignRegistry.rebuild(List.of());    // other tests assume the registry is empty
        }
    }
}

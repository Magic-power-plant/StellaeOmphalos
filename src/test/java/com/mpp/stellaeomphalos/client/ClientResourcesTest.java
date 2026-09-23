package com.mpp.stellaeomphalos.client;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.*;

import org.junit.jupiter.api.Test;

import java.nio.file.*;

class ClientResourcesTest {
    private static final Path GENERATED = Path.of("src/generated/resources/assets/stellaeomphalos");
    private static final Path AUTHORED = Path.of("src/main/resources/assets/stellaeomphalos");

    @Test
    void everyModTextureReferencedByDistributedModelsExists() throws Exception {
        try (var files = Files.walk(GENERATED.resolve("models"))) {
            for (var file : files.filter(p -> p.toString().endsWith(".json")).toList()) {
                var model = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                if (!model.has("textures")) continue;
                for (var entry : model.getAsJsonObject("textures").entrySet()) {
                    String texture = entry.getValue().getAsString();
                    if (!texture.startsWith("stellaeomphalos:")) continue;
                    String relative =
                            "textures/" + texture.substring("stellaeomphalos:".length()) + ".png";
                    assertTrue(
                            Files.exists(GENERATED.resolve(relative))
                                    || Files.exists(AUTHORED.resolve(relative)),
                            file + " -> " + texture);
                }
            }
        }
    }

    @Test
    void everySoundHasLocalizedSubtitleAndPlayableOgg() throws Exception {
        var sounds =
                JsonParser.parseString(Files.readString(GENERATED.resolve("sounds.json")))
                        .getAsJsonObject();
        var en =
                JsonParser.parseString(Files.readString(GENERATED.resolve("lang/en_us.json")))
                        .getAsJsonObject();
        var zh =
                JsonParser.parseString(Files.readString(GENERATED.resolve("lang/zh_cn.json")))
                        .getAsJsonObject();
        for (var entry : sounds.entrySet()) {
            var sound = entry.getValue().getAsJsonObject();
            String key = sound.get("subtitle").getAsString();
            assertTrue(en.has(key) && zh.has(key), key);
            for (var clip : sound.getAsJsonArray("sounds")) {
                String name =
                        clip.getAsJsonObject()
                                .get("name")
                                .getAsString()
                                .substring("stellaeomphalos:".length());
                byte[] bytes = Files.readAllBytes(AUTHORED.resolve("sounds/" + name + ".ogg"));
                assertEquals(
                        "OggS",
                        new String(bytes, 0, 4, java.nio.charset.StandardCharsets.US_ASCII));
            }
        }
    }

    @Test
    void allShaderProgramsAndEightTelescopeStatesAreComplete() throws Exception {
        for (String name :
                java.util.List.of(
                        "beam_additive",
                        "sky_additive",
                        "soft_particle",
                        "glow_layer",
                        "ghost_block"))
            for (String ext : java.util.List.of("json", "vsh", "fsh"))
                assertTrue(
                        Files.size(AUTHORED.resolve("shaders/core/rendertype_" + name + "." + ext))
                                > 0);
        var states =
                JsonParser.parseString(
                                Files.readString(GENERATED.resolve("blockstates/spyglass.json")))
                        .getAsJsonObject()
                        .getAsJsonObject("variants");
        assertEquals(8, states.size());
        for (int i = 0; i < 8; i++) assertTrue(states.has("rotation=" + i));
    }
}

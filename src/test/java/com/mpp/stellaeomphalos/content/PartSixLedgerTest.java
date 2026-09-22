package com.mpp.stellaeomphalos.content;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Part-6 §6.2.1 注册总账的静态验收（AC-6.1 / AC-6.2 / AC-6.3 / AC-6.10）。
 *
 * <p>校验三件事：① 总账里的每个 id 都出现在主源码的注册调用中；② 每个可获得方块都有 DataGen
 * 产出的 blockstate 与掉落表；③ 装饰族的 blockstate 展开了 {@code variant × top × bottom}。
 */
class PartSixLedgerTest {

    private static final Path MAIN = Path.of("src/main/java");
    private static final Path GENERATED = Path.of("src/generated/resources/assets/stellaeomphalos");
    private static final Path GENERATED_DATA = Path.of("src/generated/resources/data/stellaeomphalos");

    /** §6.2.1 方块总账中本册交付的注册 id。 */
    private static final List<String> BLOCKS =
            List.of(
                    "marble",
                    "black_marble",
                    "infused_wood",
                    "marble_slab",
                    "marble_double_slab",
                    "marble_stairs",
                    "spyglass",
                    "quern",
                    "fountain",
                    "bore_head",
                    "lumen_infuser",
                    "star_chart_table",
                    "observatory",
                    "chalice",
                    "grove_beacon",
                    "luminaire",
                    "gate_node",
                    "rite_link",
                    "celestial_orrery",
                    "geode_ore",
                    "aquamarine_sand",
                    "sky_crystal_cluster",
                    "prism_crystal_cluster",
                    "glowbloom",
                    "cosmetic_rock",
                    "frame_shell",
                    "phase_barrier",
                    "proxy_foliage",
                    "mirage_shell",
                    "rupture_anchor",
                    "glow_mote",
                    "ephemeral_light",
                    "asterism_altar",
                    "beam_lens",
                    "beam_prism",
                    "beam_relay",
                    "collector",
                    "lumen_well",
                    "resonance_altar",
                    "rite_pedestal",
                    "constellation_frame");

    /** §6.2.2 物品总账中本册交付的注册 id（材料 / 水晶 / 工具 / 杖 / 功能 / 可穿戴）。 */
    private static final List<String> ITEMS =
            List.of(
                    "aquamarine",
                    "astral_ingot",
                    "star_dust",
                    "lens_blank",
                    "resonance_gem",
                    "parchment",
                    "geode",
                    "sky_crystal",
                    "resonant_geode",
                    "resonant_sky_crystal",
                    "warp_star",
                    "boon_gem_sky",
                    "boon_gem_day",
                    "boon_gem_night",
                    "boon_seal",
                    "geode_axe",
                    "geode_pickaxe",
                    "geode_shovel",
                    "geode_sword",
                    "charged_geode_axe",
                    "charged_geode_pickaxe",
                    "charged_geode_shovel",
                    "charged_geode_sword",
                    "runed_wand",
                    "builder_rod",
                    "swapper_rod",
                    "luminary_rod",
                    "grapnel_rod",
                    "astrolabe",
                    "spyglass_handheld",
                    "sky_resonator",
                    "resonance_linker",
                    "rosewood_bow",
                    "illumination_dust",
                    "nocturnal_dust",
                    "lore_shard",
                    "lore_capsule",
                    "lore_scroll",
                    "codex",
                    "sign_chart",
                    "star_glass",
                    "mantle",
                    "warded_amulet");

    /** §6.3.4 标记为"无掉落"的技术方块，以及只由原版合并机制引用的双半砖。 */
    private static final Set<String> NO_LOOT =
            Set.of(
                    "marble_double_slab",
                    "frame_shell",
                    "phase_barrier",
                    "proxy_foliage",
                    "mirage_shell",
                    "rupture_anchor",
                    "glow_mote",
                    "ephemeral_light");

    /** 复用其它方块模型的 id（双半砖用 `marble`；泉头按档位复用三个钻头模型）。 */
    private static final Set<String> REUSED_MODEL = Set.of("marble_double_slab", "bore_head");

    private static String mainSources() throws IOException {
        var builder = new StringBuilder();
        try (var files = Files.walk(MAIN)) {
            for (var file : files.filter(path -> path.toString().endsWith(".java")).toList())
                builder.append(Files.readString(file, StandardCharsets.UTF_8)).append('\n');
        }
        return builder.toString();
    }

    @Test
    void every_ledger_block_id_is_declared_in_source() throws IOException {
        var source = mainSources();
        for (String id : BLOCKS)
            assertTrue(
                    source.contains("\"" + id + "\""),
                    "Block id missing from registration source: " + id);
    }

    @Test
    void every_ledger_item_id_is_declared_in_source() throws IOException {
        var source = mainSources();
        for (String id : ITEMS)
            assertTrue(source.contains("\"" + id + "\""), "Item id missing from registration source: " + id);
    }

    @Test
    void generated_blockstates_cover_the_ledger() {
        for (String id : BLOCKS) {
            var blockstate = GENERATED.resolve("blockstates/" + id + ".json");
            assertTrue(Files.exists(blockstate), "Missing blockstate for " + id);
            var model = GENERATED.resolve("models/block/" + id + ".json");
            if (REUSED_MODEL.contains(id)) continue;
            assertTrue(Files.exists(model), "Missing block model for " + id);
        }
    }

    @Test
    void generated_loot_tables_cover_obtainable_blocks() {
        for (String id : BLOCKS) {
            if (NO_LOOT.contains(id)) continue;
            assertTrue(
                    Files.exists(GENERATED_DATA.resolve("loot_tables/blocks/" + id + ".json")),
                    "Missing loot table for " + id);
        }
    }

    @Test
    void decoration_pillars_enumerate_connection_states() throws IOException {
        for (String family : List.of("marble", "black_marble", "infused_wood")) {
            var text = Files.readString(GENERATED.resolve("blockstates/" + family + ".json"), StandardCharsets.UTF_8);
            assertTrue(text.contains("variant=raw"), family + " missing raw variant");
            assertTrue(text.contains("variant=pillar,top=false,bottom=false"), family + " missing pillar single");
            assertTrue(text.contains("variant=pillar,top=true,bottom=true"), family + " missing pillar middle");
        }
    }

    @Test
    void decoration_loot_tables_select_the_matching_variant() throws IOException {
        var text =
                Files.readString(
                        GENERATED_DATA.resolve("loot_tables/blocks/marble.json"), StandardCharsets.UTF_8);
        assertTrue(text.contains("minecraft:block_state_property"), "Variant condition missing");
        assertTrue(text.contains("stellaeomphalos:marble_runed"), "Runed variant drop missing");
    }

    /** §6.2.1.3：晶簇矿的两个变体由同一方块承载，星辉矿不再是独立注册 id。 */
    @Test
    void geode_ore_carries_both_variants_in_one_block() throws IOException {
        var blockstate =
                Files.readString(GENERATED.resolve("blockstates/geode_ore.json"), StandardCharsets.UTF_8);
        assertTrue(blockstate.contains("variant=geode"), "GEODE variant model missing");
        assertTrue(blockstate.contains("variant=astral"), "ASTRAL variant model missing");

        var loot =
                Files.readString(
                                GENERATED_DATA.resolve("loot_tables/blocks/geode_ore.json"),
                                StandardCharsets.UTF_8)
                        // 生成器使用带缩进的 pretty-print，比较前去掉全部空白。
                        .replaceAll("\\s+", "");
        assertTrue(loot.contains("\"variant\":\"geode\""), "GEODE drop condition missing");
        assertTrue(loot.contains("\"variant\":\"astral\""), "ASTRAL drop condition missing");
        assertTrue(loot.contains("minecraft:set_nbt"), "ASTRAL drop must carry its variant tag");

        try (var files = Files.walk(GENERATED)) {
            assertTrue(
                    files.filter(path -> path.getFileName().toString().contains("star_metal_ore"))
                            .findAny()
                            .isEmpty(),
                    "star_metal_ore resources must be gone after the variant merge");
        }
    }
}

package com.mpp.stellaeomphalos.content.blockentity.crafting;

import com.google.gson.*;
import com.mpp.stellaeomphalos.data.loader.FoundationDataProvider;

import net.minecraft.data.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Deterministic production catalogue. These factories run only during data generation. */
public final class CraftingDataProvider implements DataProvider {
    private static final String NS = "stellaeomphalos:";
    private static final String[] SIGNS = {
        "aevitas",
        "armara",
        "discidia",
        "evorsio",
        "vicio",
        "bootes",
        "fornax",
        "horologium",
        "lucerna",
        "mineralis",
        "octans",
        "pelotrio",
        "gelu",
        "ulteria",
        "alcara",
        "vorux"
    };
    private static final String[] COLORS = {
        "white",
        "orange",
        "magenta",
        "light_blue",
        "yellow",
        "lime",
        "pink",
        "gray",
        "light_gray",
        "cyan",
        "purple",
        "blue",
        "brown",
        "green",
        "red",
        "black"
    };
    private static final Gson GSON = new Gson();
    private final PackOutput output;

    public CraftingDataProvider(PackOutput output) {
        this.output = output;
        languages();
    }

    private static JsonObject json(Object value) {
        return GSON.toJsonTree(value).getAsJsonObject();
    }

    private static Map<String, Object> item(String name) {
        return Map.of("item", name.contains(":") ? name : NS + name);
    }

    private static Map<String, Object> result(String name) {
        return Map.of("kind", "static", "stack", item(name));
    }

    private static Map<String, Object> op(String name, Object... values) {
        var result = new LinkedHashMap<String, Object>();
        result.put("op", NS + name);
        for (int i = 0; i < values.length; i += 2) result.put((String) values[i], values[i + 1]);
        return result;
    }

    private static Map<String, Object> grid(Object center, Object edge) {
        return Map.of(
                "pattern", List.of(" E ", "ECE", " E "), "key", Map.of("C", center, "E", edge));
    }

    private static Map<String, Object> altar(
            String tier, Object center, Object edge, String output) {
        var data = new LinkedHashMap<String, Object>();
        data.put("type", NS + "asterism_crafting");
        data.put("tier", tier);
        data.put("grid", grid(center, edge));
        data.put(
                "lumen",
                switch (tier) {
                    case "resonance" -> 1400;
                    case "sign" -> 3200;
                    case "trait" -> 7500;
                    default -> 700;
                });
        data.put(
                "duration",
                switch (tier) {
                    case "resonance" -> 300;
                    case "sign" -> 500;
                    case "trait" -> 700;
                    default -> 100;
                });
        data.put("result", result(output));
        return data;
    }

    public static Map<String, JsonObject> recipes() {
        var recipes = new TreeMap<String, JsonObject>();
        String[] tiers = {"discovery", "resonance", "sign", "trait"};
        int[] durations = {400, 1200, 2000};
        for (int i = 0; i < 3; i++) {
            var data =
                    altar(
                            tiers[i],
                            item("minecraft:diamond"),
                            item("minecraft:amethyst_shard"),
                            "star_lens");
            data.put("type", NS + "asterism_upgrade");
            data.remove("tier");
            data.remove("result");
            data.put("from_tier", tiers[i]);
            data.put("to_tier", tiers[i + 1]);
            data.put("duration", durations[i]);
            data.put("flags", List.of("night_only", "no_item_output"));
            data.put("result_display", item("asterism_altar_" + tiers[i + 1]));
            recipes.put("altar/upgrade_" + tiers[i + 1], json(data));
        }
        recipes.put(
                "workbench/discovery_altar",
                json(
                        Map.of(
                                "type",
                                "minecraft:crafting_shaped",
                                "pattern",
                                List.of(" A ", "QAQ", "QQQ"),
                                "key",
                                Map.of(
                                        "A",
                                        item("minecraft:amethyst_shard"),
                                        "Q",
                                        item("minecraft:quartz")),
                                "result",
                                item("asterism_altar_discovery"))));
        recipes.put(
                "altar/lumen_collector",
                json(
                        altar(
                                "discovery",
                                item("minecraft:clock"),
                                item("raw_crystal"),
                                "lumen_collector")));
        recipes.put(
                "altar/lumen_relay",
                json(
                        altar(
                                "discovery",
                                item("minecraft:copper_ingot"),
                                item("minecraft:glass"),
                                "lumen_relay")));
        recipes.put(
                "altar/lumen_battery",
                json(
                        altar(
                                "discovery",
                                item("minecraft:redstone_block"),
                                item("minecraft:iron_ingot"),
                                "lumen_battery")));
        recipes.put(
                "altar/lumen_flask",
                json(
                        altar(
                                "discovery",
                                item("minecraft:glass_bottle"),
                                item("minecraft:quartz"),
                                "lumen_flask")));
        String[] machines = {
            "lumen_infuser",
            "grindwheel",
            "lumen_well",
            "lumen_chalice",
            "crafting_relay",
            "light_transmuter"
        };
        for (String machine : machines) {
            String center =
                    switch (machine) {
                        case "lumen_infuser" -> "iron_ingot";
                        case "grindwheel" -> "stick";
                        case "lumen_well" -> "bucket";
                        case "lumen_chalice" -> "gold_ingot";
                        case "light_transmuter" -> "glowstone_dust";
                        default -> "copper_ingot";
                    };
            recipes.put(
                    "altar/" + machine,
                    json(
                            altar(
                                    "discovery",
                                    item("minecraft:" + center),
                                    item(
                                            machine.equals("grindwheel")
                                                    ? "minecraft:stone"
                                                    : "minecraft:quartz"),
                                    machine)));
        }
        for (String tool : List.of("pickaxe", "axe", "shovel", "sword")) {
            var data =
                    altar(
                            "resonance",
                            item("minecraft:diamond_" + tool),
                            item("raw_crystal"),
                            "crystal_" + tool);
            data.put("duration", 450);
            data.put("flags", List.of("no_chain"));
            data.put(
                    "result",
                    Map.of(
                            "kind",
                            "static",
                            "stack",
                            item("crystal_" + tool),
                            "ops",
                            List.of(op("merge_crystal_traits"))));
            recipes.put("altar/crystal_" + tool, json(data));
        }
        var raw =
                altar(
                        "discovery",
                        item("minecraft:amethyst_shard"),
                        item("minecraft:quartz"),
                        "raw_crystal");
        raw.put(
                "result",
                Map.of(
                        "kind",
                        "static",
                        "stack",
                        Map.of(
                                "item",
                                NS + "raw_crystal",
                                "nbt",
                                Map.of(
                                        "CrystalTraits",
                                        Map.of("Size", 200, "Purity", 60, "Collect", 50)))));
        recipes.put("altar/raw_crystal", json(raw));
        for (String basic :
                List.of(
                        "star_sextant",
                        "illumination_wand",
                        "star_mantle",
                        "conversion_star",
                        "enchant_charm",
                        "drill_head",
                        "resonator",
                        "star_lens",
                        "prism_lens",
                        "ritual_base")) {
            String edge =
                    switch (basic) {
                        case "star_sextant" -> "iron_ingot";
                        case "illumination_wand" -> "stick";
                        case "star_mantle" -> "phantom_membrane";
                        case "conversion_star" -> "ender_pearl";
                        case "enchant_charm" -> "enchanted_book";
                        case "drill_head" -> "diamond";
                        case "resonator" -> "redstone";
                        case "star_lens" -> "glass";
                        case "prism_lens" -> "glass_pane";
                        default -> "quartz";
                    };
            var data =
                    altar(
                            basic.equals("ritual_base") ? "resonance" : "discovery",
                            basic.equals("ritual_base")
                                    ? Map.of("fluid", NS + "molten_lumen", "amount", 400)
                                    : item("raw_crystal"),
                            item("minecraft:" + edge),
                            basic);
            if (basic.equals("illumination_wand"))
                data.put(
                        "result",
                        Map.of(
                                "kind",
                                "static",
                                "stack",
                                Map.of("item", NS + basic, "nbt", Map.of("WandColor", 16777215))));
            if (basic.equals("enchant_charm")) {
                data.put("duration", 600);
                data.put(
                        "result",
                        Map.of(
                                "kind",
                                "static",
                                "stack",
                                item(basic),
                                "ops",
                                List.of(op("random_enchantment"))));
            }
            if (basic.equals("star_lens"))
                data.put(
                        "result",
                        Map.of(
                                "kind",
                                "static",
                                "stack",
                                item(basic),
                                "ops",
                                List.of(op("scale_count_by_traits", "divisor", 80, "min", 1))));
            if (basic.equals("prism_lens"))
                data.put(
                        "result",
                        Map.of(
                                "kind",
                                "static",
                                "stack",
                                item(basic),
                                "ops",
                                List.of(op("copy_crystal_traits"))));
            recipes.put("altar/" + basic, json(data));
        }
        var reroll =
                altar(
                        "sign",
                        item("enchant_charm"),
                        item("minecraft:lapis_lazuli"),
                        "enchant_charm");
        reroll.put("duration", 250);
        reroll.put(
                "result",
                Map.of(
                        "kind",
                        "inherit",
                        "fallback",
                        item("enchant_charm"),
                        "ops",
                        List.of(op("random_enchantment"))));
        recipes.put("altar/charm_reroll", json(reroll));
        var sextant =
                altar(
                        "sign",
                        Map.of(
                                "item",
                                NS + "star_sextant",
                                "exclude_nbt",
                                Map.of("Upgraded", true)),
                        item("minecraft:ender_pearl"),
                        "star_sextant");
        sextant.put(
                "result",
                Map.of(
                        "kind",
                        "inherit",
                        "source_slot",
                        "center",
                        "fallback",
                        item("star_sextant"),
                        "ops",
                        List.of(op("set_boolean", "key", "Upgraded", "value", true))));
        recipes.put("altar/sextant_upgrade", json(sextant));
        for (int i = 0; i < SIGNS.length; i++) {
            String sign = SIGNS[i], dye = "minecraft:" + COLORS[i] + "_dye";
            for (String output :
                    List.of("sign_focus", "sign_paper", "illumination_wand", "star_mantle")) {
                Object center =
                        output.equals("sign_focus")
                                ? item("raw_crystal")
                                : output.equals("sign_paper")
                                        ? item("minecraft:paper")
                                        : Map.of(
                                                "item",
                                                NS + output,
                                                "exclude_nbt",
                                                Map.of("SignId", NS + sign));
                if (output.equals("illumination_wand"))
                    center =
                            Map.of(
                                    "item",
                                    NS + output,
                                    "include_nbt",
                                    Map.of("WandColor", 16777215),
                                    "absent_nbt",
                                    List.of("SignId"));
                var data =
                        altar(
                                output.equals("sign_focus") ? "resonance" : "sign",
                                center,
                                item(dye),
                                output);
                if (output.equals("star_mantle"))
                    data.put(
                            "result",
                            Map.of(
                                    "kind",
                                    "inherit",
                                    "fallback",
                                    item(output),
                                    "ops",
                                    List.of(op("set_sign", "sign", NS + sign))));
                else
                    data.put(
                            "result",
                            Map.of(
                                    "kind",
                                    "static",
                                    "stack",
                                    item(output),
                                    "ops",
                                    List.of(op("set_sign", "sign", NS + sign))));
                if (!output.equals("sign_paper") && !output.equals("sign_focus"))
                    data.put("focus_sign", NS + sign);
                recipes.put("altar/" + output + "_" + sign, json(data));
            }
            var lens = altar("resonance", item("star_lens"), item(dye), "star_lens");
            lens.put(
                    "result",
                    Map.of(
                            "kind",
                            "inherit",
                            "fallback",
                            item("star_lens"),
                            "ops",
                            List.of(op("set_lens_color", "color", 0xFF000000 | i * 0x101010))));
            recipes.put("altar/lens_" + COLORS[i], json(lens));
            recipes.put(
                    "transmutation/" + COLORS[i] + "_concrete",
                    json(
                            Map.of(
                                    "type",
                                    NS + "light_transmutation",
                                    "input",
                                    Map.of("block", "minecraft:" + COLORS[i] + "_concrete_powder"),
                                    "result",
                                    Map.of("block", "minecraft:" + COLORS[i] + "_concrete"),
                                    "cost",
                                    80)));
            recipes.put(
                    "melting/" + COLORS[i] + "_glass",
                    json(
                            Map.of(
                                    "type",
                                    NS + "melting",
                                    "input",
                                    Map.of("block", "minecraft:" + COLORS[i] + "_stained_glass"),
                                    "result",
                                    Map.of("block", "minecraft:glass"),
                                    "duration",
                                    100)));
        }
        for (String upgrade : List.of("piercing", "wide", "deep", "swift")) {
            var data =
                    altar(
                            "trait",
                            Map.of(
                                    "item",
                                    NS + "drill_head",
                                    "exclude_nbt",
                                    Map.of("Upgrades", List.of(NS + upgrade))),
                            item("minecraft:diamond"),
                            "drill_head");
            data.put("flags", List.of("strict_match"));
            data.put(
                    "result",
                    Map.of(
                            "kind",
                            "inherit",
                            "fallback",
                            item("drill_head"),
                            "ops",
                            List.of(op("unlock_upgrade", "upgrade", NS + upgrade))));
            data.put(
                    "relay",
                    List.of(
                            Map.of(
                                    "index",
                                    0,
                                    "ingredient",
                                    item(
                                            "minecraft:"
                                                    + switch (upgrade) {
                                                        case "piercing" -> "flint";
                                                        case "wide" -> "iron_ingot";
                                                        case "deep" -> "gold_ingot";
                                                        default -> "redstone";
                                                    }),
                                    "bind_at",
                                    0.25)));
            recipes.put("altar/drill_" + upgrade, json(data));
        }
        for (String upgrade : List.of("range", "precision")) {
            var data =
                    altar(
                            "sign",
                            Map.of(
                                    "item",
                                    NS + "resonator",
                                    "exclude_nbt",
                                    Map.of("Upgrades", List.of(NS + upgrade))),
                            item(
                                    upgrade.equals("range")
                                            ? "minecraft:ender_pearl"
                                            : "minecraft:quartz"),
                            "resonator");
            data.put("flags", List.of("strict_match"));
            data.put(
                    "result",
                    Map.of(
                            "kind",
                            "inherit",
                            "fallback",
                            item("resonator"),
                            "ops",
                            List.of(op("unlock_upgrade", "upgrade", NS + upgrade))));
            recipes.put("altar/resonator_" + upgrade, json(data));
        }
        var collector =
                altar(
                        "sign",
                        Map.of(
                                "item",
                                NS + "resonant_crystal",
                                "include_nbt",
                                Map.of("Attuned", true)),
                        item("minecraft:glass"),
                        "collector_crystal");
        collector.put(
                "result",
                Map.of(
                        "kind",
                        "static",
                        "stack",
                        item("collector_crystal"),
                        "ops",
                        List.of(op("preserve_nbt"), op("copy_crystal_traits"))));
        recipes.put("altar/collector_crystal", json(collector));
        for (String crystal : List.of("raw_crystal", "resonant_crystal"))
            recipes.put(
                    "infusion/" + crystal,
                    json(
                            Map.of(
                                    "type",
                                    NS + "lumen_infusion",
                                    "input",
                                    item(
                                            crystal.equals("raw_crystal")
                                                    ? "minecraft:amethyst_shard"
                                                    : "raw_crystal"),
                                    "duration",
                                    200,
                                    "solvent",
                                    Map.of("chance", crystal.equals("raw_crystal") ? 0.05 : 1.0),
                                    "result",
                                    Map.of(
                                            "kind",
                                            "static",
                                            "stack",
                                            Map.of(
                                                    "item",
                                                    NS + crystal,
                                                    "nbt",
                                                    Map.of(
                                                            "CrystalTraits",
                                                            Map.of(
                                                                    "Size", 200, "Purity", 60,
                                                                    "Collect", 50),
                                                            "Attuned",
                                                            true))))));
        for (String tool : List.of("pickaxe", "axe", "shovel", "sword"))
            recipes.put(
                    "infusion/charged_" + tool,
                    json(
                            Map.of(
                                    "type",
                                    NS + "lumen_infusion",
                                    "input",
                                    item("crystal_" + tool),
                                    "duration",
                                    200,
                                    "result",
                                    Map.of(
                                            "kind",
                                            "inherit",
                                            "source_slot",
                                            "input",
                                            "fallback",
                                            item("crystal_" + tool),
                                            "ops",
                                            List.of(
                                                    op("set_tool_traits"),
                                                    op("clear_revert_counter"))))));
        String[][] grinds = {
            {"cobblestone", "gravel"},
            {"gravel", "sand"},
            {"sandstone", "sand"},
            {"red_sandstone", "red_sand"},
            {"quartz_block", "quartz"},
            {"amethyst_block", "amethyst_shard"},
            {"bone", "bone_meal"},
            {"blaze_rod", "blaze_powder"},
            {"wheat", "wheat_seeds"},
            {"clay", "clay_ball"},
            {"glowstone", "glowstone_dust"},
            {"stone", "cobblestone"}
        };
        for (var pair : grinds)
            recipes.put(
                    "grinding/" + pair[0],
                    json(
                            Map.of(
                                    "type",
                                    NS + "grindwheel",
                                    "input",
                                    item("minecraft:" + pair[0]),
                                    "result",
                                    item("minecraft:" + pair[1]),
                                    "chance",
                                    2,
                                    "bonus_chance",
                                    0.25)));
        for (String alteration : List.of("crystal", "tool", "hone"))
            recipes.put(
                    "grinding/alter_" + alteration,
                    json(
                            Map.of(
                                    "type",
                                    NS + "grindwheel",
                                    "alteration",
                                    NS + alteration,
                                    "hidden",
                                    true,
                                    "chance",
                                    alteration.equals("hone") ? 40 : 1)));
        int i = 0;
        for (String catalyst :
                List.of(
                        "minecraft:amethyst_shard",
                        "minecraft:quartz",
                        NS + "raw_crystal",
                        NS + "resonant_crystal",
                        "minecraft:glowstone_dust",
                        "minecraft:prismarine_crystals")) {
            recipes.put(
                    "liquefaction/catalyst_" + i,
                    json(
                            Map.of(
                                    "type",
                                    NS + "well_liquefaction",
                                    "catalyst",
                                    item(catalyst),
                                    "result_fluid",
                                    Map.of("fluid", NS + "molten_lumen", "amount", 1000),
                                    "production_multiplier",
                                    0.5 + i * 0.25,
                                    "shatter_multiplier",
                                    2 + i)));
            i++;
        }
        for (String other : List.of("water", "lava"))
            recipes.put(
                    "interaction/" + other,
                    json(
                            Map.of(
                                    "type",
                                    NS + "fluid_interaction",
                                    "first",
                                    Map.of("fluid", NS + "molten_lumen", "amount", 400),
                                    "second",
                                    Map.of("fluid", "minecraft:" + other, "amount", 400),
                                    "weight",
                                    10,
                                    "consume_chance_first",
                                    0.5,
                                    "result",
                                    Map.of(
                                            "kind",
                                            "block",
                                            "block",
                                            other.equals("water")
                                                    ? "minecraft:ice"
                                                    : "minecraft:obsidian"))));
        recipes.put(
                "interaction/crystal",
                json(
                        Map.of(
                                "type",
                                NS + "fluid_interaction",
                                "first",
                                Map.of("fluid", NS + "molten_lumen", "amount", 400),
                                "second",
                                Map.of("fluid", "minecraft:water", "amount", 1000),
                                "weight",
                                1,
                                "result",
                                Map.of(
                                        "kind",
                                        "random_crystal",
                                        "item",
                                        NS + "raw_crystal",
                                        "item_half_life",
                                        true))));
        recipes.put(
                "melting/ice",
                json(
                        Map.of(
                                "type",
                                NS + "melting",
                                "input",
                                Map.of("block", "minecraft:ice"),
                                "result",
                                Map.of("block", "minecraft:water"),
                                "duration",
                                100)));
        recipes.put(
                "workbench/light_wand",
                json(
                        Map.of(
                                "type",
                                NS + "light_proximity_crafting",
                                "pattern",
                                List.of(" Q ", " S ", " S "),
                                "key",
                                Map.of("Q", item("minecraft:quartz"), "S", item("minecraft:stick")),
                                "result",
                                Map.of(
                                        "item",
                                        NS + "illumination_wand",
                                        "nbt",
                                        Map.of("WandColor", 16777215)),
                                "min_lumen",
                                20)));
        recipes.put("workbench/wand_recolor", json(Map.of("type", NS + "wand_recolor")));
        return Collections.unmodifiableMap(recipes);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        var futures = new ArrayList<CompletableFuture<?>>();
        recipes()
                .forEach(
                        (id, recipe) ->
                                save(
                                        cache,
                                        futures,
                                        "data/stellaeomphalos/recipes/" + id + ".json",
                                        recipe));
        for (String block :
                List.of(
                        "asterism_altar",
                        "lumen_infuser",
                        "grindwheel",
                        "lumen_well",
                        "lumen_chalice",
                        "crafting_relay",
                        "light_transmuter")) {
            save(
                    cache,
                    futures,
                    "assets/stellaeomphalos/blockstates/" + block + ".json",
                    json(Map.of("variants", Map.of("", Map.of("model", NS + "block/" + block)))));
            save(
                    cache,
                    futures,
                    "assets/stellaeomphalos/models/block/" + block + ".json",
                    json(
                            Map.of(
                                    "parent",
                                    "minecraft:block/cube_bottom_top",
                                    "textures",
                                    Map.of(
                                            "top",
                                            "minecraft:block/chiseled_quartz_block_top",
                                            "side",
                                            "minecraft:block/chiseled_quartz_block",
                                            "bottom",
                                            "minecraft:block/smooth_stone"))));
            if (!block.equals("asterism_altar"))
                save(
                        cache,
                        futures,
                        "data/stellaeomphalos/loot_tables/blocks/" + block + ".json",
                        json(
                                Map.of(
                                        "type",
                                        "minecraft:block",
                                        "pools",
                                        List.of(
                                                Map.of(
                                                        "rolls",
                                                        1,
                                                        "entries",
                                                        List.of(
                                                                Map.of(
                                                                        "type",
                                                                        "minecraft:item",
                                                                        "name",
                                                                        NS + block)),
                                                        "conditions",
                                                        List.of(
                                                                Map.of(
                                                                        "condition",
                                                                        "minecraft:survives_explosion")))))));
        }
        for (String block : List.of("lumen_collector", "lumen_relay", "lumen_battery")) {
            save(
                    cache,
                    futures,
                    "assets/stellaeomphalos/blockstates/" + block + ".json",
                    json(Map.of("variants", Map.of("", Map.of("model", NS + "block/" + block)))));
            save(
                    cache,
                    futures,
                    "assets/stellaeomphalos/models/block/" + block + ".json",
                    json(
                            Map.of(
                                    "parent",
                                    "minecraft:block/cube_all",
                                    "textures",
                                    Map.of(
                                            "all",
                                            block.equals("lumen_collector")
                                                    ? "minecraft:block/amethyst_block"
                                                    : block.equals("lumen_relay")
                                                            ? "minecraft:block/quartz_pillar"
                                                            : "minecraft:block/chiseled_quartz_block"))));
            save(
                    cache,
                    futures,
                    "assets/stellaeomphalos/models/item/" + block + ".json",
                    json(Map.of("parent", NS + "block/" + block)));
            save(
                    cache,
                    futures,
                    "data/stellaeomphalos/loot_tables/blocks/" + block + ".json",
                    json(
                            Map.of(
                                    "type",
                                    "minecraft:block",
                                    "pools",
                                    List.of(
                                            Map.of(
                                                    "rolls",
                                                    1,
                                                    "entries",
                                                    List.of(
                                                            Map.of(
                                                                    "type",
                                                                    "minecraft:item",
                                                                    "name",
                                                                    NS + block)),
                                                    "conditions",
                                                    List.of(
                                                            Map.of(
                                                                    "condition",
                                                                    "minecraft:survives_explosion")))))));
        }
        var altarPools = new ArrayList<Object>();
        for (var tier : com.mpp.stellaeomphalos.crafting.altar.recipe.AsterismTier.values())
            altarPools.add(
                    Map.of(
                            "rolls",
                            1,
                            "entries",
                            List.of(
                                    Map.of(
                                            "type",
                                            "minecraft:item",
                                            "name",
                                            NS + "asterism_altar_" + tier.getSerializedName())),
                            "conditions",
                            List.of(
                                    Map.of(
                                            "condition",
                                            "minecraft:block_state_property",
                                            "block",
                                            NS + "asterism_altar",
                                            "properties",
                                            Map.of("tier", tier.getSerializedName())),
                                    Map.of("condition", "minecraft:survives_explosion"))));
        save(
                cache,
                futures,
                "data/stellaeomphalos/loot_tables/blocks/asterism_altar.json",
                json(Map.of("type", "minecraft:block", "pools", altarPools)));
        CraftingContent.ITEMS.forEach(
                (id, item) -> {
                    boolean block =
                            id.startsWith("asterism_altar_")
                                    || List.of(
                                                    "lumen_infuser",
                                                    "grindwheel",
                                                    "lumen_well",
                                                    "lumen_chalice",
                                                    "crafting_relay",
                                                    "light_transmuter")
                                            .contains(id);
                    save(
                            cache,
                            futures,
                            "assets/stellaeomphalos/models/item/" + id + ".json",
                            json(
                                    block
                                            ? Map.of(
                                                    "parent",
                                                    NS
                                                            + "block/"
                                                            + (id.startsWith("asterism_altar_")
                                                                    ? "asterism_altar"
                                                                    : id))
                                            : Map.of(
                                                    "parent",
                                                    "minecraft:item/generated",
                                                    "textures",
                                                    Map.of(
                                                            "layer0",
                                                            id.contains("crystal")
                                                                            || id.contains("focus")
                                                                    ? "minecraft:item/amethyst_shard"
                                                                    : id.contains("paper")
                                                                            ? "minecraft:item/paper"
                                                                            : id.contains("flask")
                                                                                    ? "minecraft:item/glass_bottle"
                                                                                    : "minecraft:item/echo_shard"))));
                });
        save(
                cache,
                futures,
                "data/stellaeomphalos/tags/blocks/altar_frame.json",
                json(
                        Map.of(
                                "replace",
                                false,
                                "values",
                                List.of(
                                        "minecraft:quartz_pillar",
                                        "minecraft:chiseled_quartz_block"))));
        var sounds = new LinkedHashMap<String, Object>();
        var vanillaSounds =
                Map.of(
                        "craft_loop",
                        "minecraft:block.amethyst_block.resonate",
                        "craft_finish",
                        "minecraft:block.enchantment_table.use",
                        "infusion_bubble",
                        "minecraft:block.bubble_column.bubble_pop",
                        "grindwheel_spin",
                        "minecraft:block.grindstone.use");
        vanillaSounds.forEach(
                (id, event) ->
                        sounds.put(
                                id,
                                Map.of("sounds", List.of(Map.of("name", event, "type", "event")))));
        save(cache, futures, "assets/stellaeomphalos/sounds.json", json(sounds));
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private void save(
            CachedOutput cache, List<CompletableFuture<?>> futures, String path, JsonObject json) {
        futures.add(DataProvider.saveStable(cache, json, output.getOutputFolder().resolve(path)));
    }

    private static void languages() {
        FoundationDataProvider.language(
                "stellaeomphalos.crafting.flask_contents", "%s: %s / 2000 mB", "%s：%s / 2000 mB");
        FoundationDataProvider.language("stellaeomphalos.crafting.ready", "Ready", "已完成");
        FoundationDataProvider.language(
                "stellaeomphalos.crafting.reset",
                "Recipe mutations reset to the datapack baseline",
                "配方修改已回滚到数据包基线");
        FoundationDataProvider.language(
                "itemGroup.stellaeomphalos.crafting", "Celestial Crafting", "星辉制作");
        String[] names = {"发现", "共鸣", "星象", "特质", "璀璨"};
        int index = 0;
        for (var tier : com.mpp.stellaeomphalos.crafting.altar.recipe.AsterismTier.values()) {
            String key = "asterism_altar_" + tier.getSerializedName();
            FoundationDataProvider.language(
                    "item.stellaeomphalos." + key, tier.name() + " Altar", names[index++] + "阶星坛");
        }
        var zh =
                Map.ofEntries(
                        Map.entry("lumen_infuser", "星辉注魔器"),
                        Map.entry("grindwheel", "星磨轮"),
                        Map.entry("lumen_well", "星辉井"),
                        Map.entry("lumen_chalice", "星辉盏"),
                        Map.entry("crafting_relay", "制作中继"),
                        Map.entry("light_transmuter", "光照嬗变核心"),
                        Map.entry("sign_focus", "星象聚焦晶"),
                        Map.entry("lumen_flask", "星辉瓶"),
                        Map.entry("raw_crystal", "原生晶体"),
                        Map.entry("resonant_crystal", "共鸣晶体"),
                        Map.entry("star_lens", "星辉透镜"),
                        Map.entry("prism_lens", "棱镜透镜"),
                        Map.entry("star_sextant", "星辉六分仪"),
                        Map.entry("illumination_wand", "照明法杖"),
                        Map.entry("star_mantle", "星披"),
                        Map.entry("sign_paper", "星象纸"),
                        Map.entry("conversion_star", "星辉转换星"),
                        Map.entry("enchant_charm", "附魔护符"),
                        Map.entry("drill_head", "钻孔头"),
                        Map.entry("resonator", "谐振器"),
                        Map.entry("ritual_base", "星仪基座材料"),
                        Map.entry("charged_tool", "充能工具"),
                        Map.entry("collector_crystal", "收集晶体"),
                        Map.entry("crystal_pickaxe", "晶体镐"),
                        Map.entry("crystal_axe", "晶体斧"),
                        Map.entry("crystal_shovel", "晶体锹"),
                        Map.entry("crystal_sword", "晶体剑"));
        zh.forEach(
                (id, name) -> {
                    String english =
                            Arrays.stream(id.split("_"))
                                    .map(s -> Character.toUpperCase(s.charAt(0)) + s.substring(1))
                                    .collect(java.util.stream.Collectors.joining(" "));
                    FoundationDataProvider.language("item.stellaeomphalos." + id, english, name);
                    FoundationDataProvider.language("block.stellaeomphalos." + id, english, name);
                    FoundationDataProvider.language(
                            "container.stellaeomphalos." + id, english, name);
                });
        FoundationDataProvider.language(
                "block.stellaeomphalos.asterism_altar", "Asterism Altar", "星坛");
        FoundationDataProvider.language(
                "container.stellaeomphalos.asterism", "Asterism Altar", "星坛");
        FoundationDataProvider.language("stellaeomphalos.crafting.start", "Start", "开始");
        FoundationDataProvider.language("stellaeomphalos.crafting.abort", "Abort", "中止");
        FoundationDataProvider.language("stellaeomphalos.crafting.collect", "Collect", "取出");
        FoundationDataProvider.language(
                "stellaeomphalos.crafting.energy", "Lumen: %s / %s", "星辉：%s / %s");
    }

    @Override
    public String getName() {
        return "Stellae Omphalos crafting recipes and resources";
    }
}

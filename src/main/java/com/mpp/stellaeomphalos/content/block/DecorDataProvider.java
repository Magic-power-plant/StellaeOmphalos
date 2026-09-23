package com.mpp.stellaeomphalos.content.block;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mpp.stellaeomphalos.Omphalos;
import com.mpp.stellaeomphalos.content.block.DecorFamily.DecorVariant;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraftforge.data.event.GatherDataEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 装饰变体族的资源生产（《方块物品实体完整清单》§6.3.3）。
 *
 * <p>blockstate 由 {@code variant × top × bottom} 展开（§6.3.2），模型与掉落表按注册总账逐项生成；
 * 贴图资源由 {@code src/main/part4/assets} 提供，全部为项目原创占位资源。
 */
public final class DecorDataProvider implements DataProvider {
    private static final String NS = Omphalos.MODID;

    private final PackOutput output;

    public DecorDataProvider(PackOutput output) {
        this.output = output;
    }

    public static void gather(GatherDataEvent event) {
        event.getGenerator()
                .addProvider(
                        event.includeClient() || event.includeServer(),
                        new DecorDataProvider(event.getGenerator().getPackOutput()));
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        var futures = new ArrayList<CompletableFuture<?>>();
        for (var family : DecorFamily.values()) {
            emitFamily(cache, futures, family);
        }
        emitSlabAndStairs(cache, futures);
        emitSimpleBlocks(cache, futures);
        emitLightBlocks(cache, futures);
        emitInvisibleItemBlocks(cache, futures);
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    /** 有物品但零掉落的不可见技术方块（§6.3.4 的 invisible 行）。 */
    private static final List<String> INVISIBLE_ITEM_BLOCKS =
            List.of("proxy_foliage", "rupture_anchor");

    private void emitInvisibleItemBlocks(CachedOutput cache, List<CompletableFuture<?>> out) {
        for (String id : INVISIBLE_ITEM_BLOCKS) {
            var blockstate = new JsonObject();
            var variants = new JsonObject();
            variants.add("", model("block/" + id));
            blockstate.add("variants", variants);
            out.add(save(cache, blockstate, "assets/" + NS + "/blockstates/" + id + ".json"));
            var model = new JsonObject();
            model.addProperty("parent", "minecraft:block/cube_all");
            model.add("textures", all("minecraft:block/glass"));
            out.add(save(cache, model, "assets/" + NS + "/models/block/" + id + ".json"));
            var itemModel = new JsonObject();
            itemModel.addProperty("parent", NS + ":block/" + id);
            out.add(save(cache, itemModel, "assets/" + NS + "/models/item/" + id + ".json"));
        }
    }

    /** 《方块物品实体完整清单》§6.2.1.2 / §6.2.1.3 / §6.2.1.4 / §6.2.1.5 中普通 cube_all 方块的资源。 */
    private void emitSimpleBlocks(CachedOutput cache, List<CompletableFuture<?>> out) {
        for (String id : SIMPLE_BLOCKS) {
            var blockstate = new JsonObject();
            var variants = new JsonObject();
            if (id.equals("spyglass")) {
                for (int rotation = 0; rotation < 8; rotation++) {
                    var variant = model("block/spyglass_" + (rotation % 2));
                    variant.addProperty("y", (rotation / 2 * 90 + 180) % 360);
                    variants.add("rotation=" + rotation, variant);
                }
            } else variants.add("", model("block/" + id));
            blockstate.add("variants", variants);
            out.add(save(cache, blockstate, "assets/" + NS + "/blockstates/" + id + ".json"));

            var model = new JsonObject();
            model.addProperty("parent", "minecraft:block/cube_all");
            model.add("textures", all(switch (id) {
                case "cosmetic_rock", "constellation_frame", "resonance_altar" -> NS + ":block/marble_chiseled";
                case "star_chart_table" -> NS + ":block/infused_wood_planks";
                case "grove_beacon" -> NS + ":block/infused_wood_engraved";
                case "beam_lens", "beam_prism", "celestial_orrery" -> NS + ":block/black_marble_chiseled";
                default -> NS + ":block/" + id;
            }));
            if (id.equals("spyglass")) {
                model = telescopeModel(false);
                out.add(save(cache, model, "assets/" + NS + "/models/block/spyglass_0.json"));
                out.add(save(cache, telescopeModel(true), "assets/" + NS + "/models/block/spyglass_1.json"));
            }
            out.add(save(cache, model, "assets/" + NS + "/models/block/" + id + ".json"));

            var itemModel = new JsonObject();
            itemModel.addProperty("parent", NS + ":block/" + id);
            out.add(save(cache, itemModel, "assets/" + NS + "/models/item/" + id + ".json"));

            var pools = new JsonArray();
            var pool = new JsonObject();
            pool.addProperty("rolls", 1);
            var entries = new JsonArray();
            entries.add(plainItem(NS + ":" + id, true));
            pool.add("entries", entries);
            pools.add(pool);
            var loot = new JsonObject();
            loot.addProperty("type", "minecraft:block");
            loot.add("pools", pools);
            out.add(save(cache, loot, "data/" + NS + "/loot_tables/blocks/" + id + ".json"));
        }
    }

    private static JsonObject telescopeModel(boolean diagonal) {
        var result = new JsonObject();
        result.addProperty("parent", "minecraft:block/block");
        var textures = new JsonObject();
        textures.addProperty("body", "minecraft:block/polished_deepslate");
        textures.addProperty("lens", "minecraft:block/blue_stained_glass");
        textures.addProperty("particle", "minecraft:block/polished_deepslate");
        result.add("textures", textures);
        var elements = new JsonArray();
        elements.add(telescopePart(5, 0, 5, 11, 3, 11, false, false));
        elements.add(telescopePart(7, 3, 7, 9, 16, 9, false, false));
        elements.add(telescopePart(5, 14, 0, 11, 20, 16, diagonal, true));
        result.add("elements", elements);
        return result;
    }

    private static JsonObject telescopePart(int x, int y, int z, int ex, int ey, int ez,
            boolean diagonal, boolean lens) {
        var part = new JsonObject();
        var from = new JsonArray(); from.add(x); from.add(y); from.add(z);
        var to = new JsonArray(); to.add(ex); to.add(ey); to.add(ez);
        part.add("from", from); part.add("to", to);
        if (diagonal) {
            var rotation = new JsonObject();
            var origin = new JsonArray(); origin.add(8); origin.add(16); origin.add(8);
            rotation.add("origin", origin); rotation.addProperty("axis", "y"); rotation.addProperty("angle", 45);
            part.add("rotation", rotation);
        }
        var faces = new JsonObject();
        for (String side : List.of("up", "down", "north", "south", "east", "west")) {
            var face = new JsonObject(); face.addProperty("texture", lens && side.equals("north") ? "#lens" : "#body");
            faces.add(side, face);
        }
        part.add("faces", faces);
        return part;
    }

    /** 需要自动生成 cube_all 资源与掉落表的《方块物品实体完整清单》方块 id。 */
    private static final List<String> SIMPLE_BLOCKS =
            List.of(
                    "cosmetic_rock",
                    "constellation_frame",
                    "spyglass",
                    "star_chart_table",
                    "grove_beacon",
                    "celestial_orrery",
                    "beam_lens",
                    "beam_prism",
                    "resonance_altar");

    /** 不可获得的光源方块：只有 blockstate 与模型，没有物品模型与掉落表。 */
    private static final List<String> LIGHT_BLOCKS = List.of("glow_mote", "ephemeral_light");

    private void emitLightBlocks(CachedOutput cache, List<CompletableFuture<?>> out) {
        for (String id : LIGHT_BLOCKS) {
            var blockstate = new JsonObject();
            var variants = new JsonObject();
            variants.add("", model("block/" + id));
            blockstate.add("variants", variants);
            out.add(save(cache, blockstate, "assets/" + NS + "/blockstates/" + id + ".json"));
            var model = new JsonObject();
            model.addProperty("parent", "minecraft:block/cube_all");
            model.add("textures", all("minecraft:block/glass"));
            out.add(save(cache, model, "assets/" + NS + "/models/block/" + id + ".json"));
        }
    }

    private void emitFamily(CachedOutput cache, List<CompletableFuture<?>> out, DecorFamily family) {
        // blockstate：非柱体变体 1 个变体键，柱体变体 4 个连接状态
        var variants = new JsonObject();
        for (var variant : family.variants()) {
            String model = "block/" + family.itemId(variant);
            if (variant == DecorVariant.PILLAR) {
                for (boolean top : new boolean[] {false, true})
                    for (boolean bottom : new boolean[] {false, true}) {
                        var entry = new JsonObject();
                        entry.addProperty("model", NS + ":" + model);
                        if (top && !bottom) entry.addProperty("x", 180);
                        variants.add(
                                "variant=pillar,top=" + top + ",bottom=" + bottom, entry);
                    }
            } else {
                var entry = new JsonObject();
                entry.addProperty("model", NS + ":" + model);
                variants.add("variant=" + variant.getSerializedName(), entry);
            }
        }
        // The shared EnumProperty also exposes variants used only by the other families.
        for (var unused : DecorVariant.values()) if (!family.variants().contains(unused))
            variants.add("variant=" + unused.getSerializedName(), model("block/" + family.id()));
        var blockstate = new JsonObject();
        blockstate.add("variants", variants);
        out.add(
                save(
                        cache,
                        blockstate,
                        "assets/" + NS + "/blockstates/" + family.id() + ".json"));

        // 模型与物品模型
        for (var variant : family.variants()) {
            var model = new JsonObject();
            model.addProperty(
                    "parent",
                    variant == DecorVariant.PILLAR
                            ? "minecraft:block/cube_column"
                            : "minecraft:block/cube_all");
            var textures = new JsonObject();
            String texture = NS + ":block/" + family.itemId(variant);
            if (variant == DecorVariant.PILLAR) {
                textures.addProperty("end", texture);
                textures.addProperty("side", texture);
            } else {
                textures.addProperty("all", texture);
            }
            model.add("textures", textures);
            out.add(
                    save(
                            cache,
                            model,
                            "assets/"
                                    + NS
                                    + "/models/block/"
                                    + family.itemId(variant)
                                    + ".json"));
            var itemModel = new JsonObject();
            itemModel.addProperty("parent", NS + ":block/" + family.itemId(variant));
            out.add(
                    save(
                            cache,
                            itemModel,
                            "assets/"
                                    + NS
                                    + "/models/item/"
                                    + family.itemId(variant)
                                    + ".json"));
        }

        // 掉落表：每个变体一条带 block_state_property 条件的条目
        var pools = new JsonArray();
        var pool = new JsonObject();
        pool.addProperty("rolls", 1);
        var entries = new JsonArray();
        for (var variant : family.variants()) {
            var entry = new JsonObject();
            entry.addProperty("type", "minecraft:item");
            entry.addProperty("name", NS + ":" + family.itemId(variant));
            var conditions = new JsonArray();
            var condition = new JsonObject();
            condition.addProperty("condition", "minecraft:block_state_property");
            condition.addProperty("block", NS + ":" + family.id());
            var properties = new JsonObject();
            properties.addProperty("variant", variant.getSerializedName());
            condition.add("properties", properties);
            conditions.add(condition);
            var functions = new JsonArray();
            var decay = new JsonObject();
            decay.addProperty("function", "minecraft:explosion_decay");
            functions.add(decay);
            entry.add("conditions", conditions);
            entry.add("functions", functions);
            entries.add(entry);
        }
        pool.add("entries", entries);
        pools.add(pool);
        var loot = new JsonObject();
        loot.addProperty("type", "minecraft:block");
        loot.add("pools", pools);
        out.add(
                save(
                        cache,
                        loot,
                        "data/" + NS + "/loot_tables/blocks/" + family.id() + ".json"));
    }

    private void emitSlabAndStairs(CachedOutput cache, List<CompletableFuture<?>> out) {
        var slabVariants = new JsonObject();
        slabVariants.add("type=bottom", model("block/marble_slab"));
        slabVariants.add("type=top", model("block/marble_slab_top"));
        slabVariants.add("type=double", model("block/marble"));
        var slabState = new JsonObject();
        slabState.add("variants", slabVariants);
        out.add(save(cache, slabState, "assets/" + NS + "/blockstates/marble_slab.json"));

        var doubleState = new JsonObject();
        var doubleVariants = new JsonObject();
        doubleVariants.add("", model("block/marble"));
        doubleState.add("variants", doubleVariants);
        out.add(
                save(
                        cache,
                        doubleState,
                        "assets/" + NS + "/blockstates/marble_double_slab.json"));

        var slabModel = new JsonObject();
        slabModel.addProperty("parent", "minecraft:block/slab");
        slabModel.add("textures", sides(NS + ":block/marble_bricks"));
        out.add(save(cache, slabModel, "assets/" + NS + "/models/block/marble_slab.json"));
        var slabTop = new JsonObject();
        slabTop.addProperty("parent", "minecraft:block/slab_top");
        slabTop.add("textures", sides(NS + ":block/marble_bricks"));
        out.add(save(cache, slabTop, "assets/" + NS + "/models/block/marble_slab_top.json"));
        out.add(
                save(
                        cache,
                        parent(NS + ":block/marble_slab"),
                        "assets/" + NS + "/models/item/marble_slab.json"));

        var stairsState = new JsonObject();
        var stairsVariants = new JsonObject();
        for (var facing : new String[] {"north", "east", "south", "west"})
            for (var half : new String[] {"bottom", "top"})
                for (var shape : new String[] {"straight", "inner_left", "inner_right", "outer_left", "outer_right"}) {
                    var entry = model("block/marble_stairs" + shapeSuffix(shape));
                    int y = switch (facing) {
                        case "south" -> 90;
                        case "west" -> 180;
                        case "north" -> 270;
                        default -> 0;
                    };
                    if (shape.endsWith("left")) y += 270;
                    if (half.equals("top") && !shape.equals("straight")) y += 90;
                    entry.addProperty("y", y % 360);
                    if (half.equals("top")) entry.addProperty("x", 180);
                    entry.addProperty("uvlock", true);
                    stairsVariants.add(
                            "facing=" + facing + ",half=" + half + ",shape=" + shape
                                   ,
                            entry);
                }
        stairsState.add("variants", stairsVariants);
        out.add(save(cache, stairsState, "assets/" + NS + "/blockstates/marble_stairs.json"));

        emitStairsModel(cache, out, "marble_stairs", "minecraft:block/stairs");
        emitStairsModel(cache, out, "marble_stairs_inner", "minecraft:block/inner_stairs");
        emitStairsModel(cache, out, "marble_stairs_outer", "minecraft:block/outer_stairs");
        out.add(
                save(
                        cache,
                        parent(NS + ":block/marble_stairs"),
                        "assets/" + NS + "/models/item/marble_stairs.json"));

        var slabPools = new JsonArray();
        var slabPool = new JsonObject();
        slabPool.addProperty("rolls", 1);
        var slabEntries = new JsonArray();
        slabEntries.add(
                conditionalItem(
                        NS + ":marble_slab",
                        NS + ":marble_slab",
                        "type",
                        "double",
                        true));
        slabEntries.add(plainItem(NS + ":marble_slab", true));
        slabPool.add("entries", slabEntries);
        slabPools.add(slabPool);
        var slabLoot = new JsonObject();
        slabLoot.addProperty("type", "minecraft:block");
        slabLoot.add("pools", slabPools);
        out.add(save(cache, slabLoot, "data/" + NS + "/loot_tables/blocks/marble_slab.json"));

        var stairsPools = new JsonArray();
        var stairsPool = new JsonObject();
        stairsPool.addProperty("rolls", 1);
        var stairsEntries = new JsonArray();
        stairsEntries.add(plainItem(NS + ":marble_stairs", true));
        stairsPool.add("entries", stairsEntries);
        stairsPools.add(stairsPool);
        var stairsLoot = new JsonObject();
        stairsLoot.addProperty("type", "minecraft:block");
        stairsLoot.add("pools", stairsPools);
        out.add(
                save(
                        cache,
                        stairsLoot,
                        "data/" + NS + "/loot_tables/blocks/marble_stairs.json"));

        out.add(
                save(
                        cache,
                        shaped(
                                List.of("M  ", "MM ", "MMM"),
                                java.util.Map.of("M", NS + ":marble_bricks"),
                                NS + ":marble_stairs",
                                4),
                        "data/" + NS + "/recipes/world/marble_stairs.json"));
        out.add(
                save(
                        cache,
                        shaped(
                                List.of("MMM"),
                                java.util.Map.of("M", NS + ":marble_bricks"),
                                NS + ":marble_slab",
                                6),
                        "data/" + NS + "/recipes/world/marble_slab.json"));
    }

    private void emitStairsModel(
            CachedOutput cache, List<CompletableFuture<?>> out, String name, String parent) {
        var model = new JsonObject();
        model.addProperty("parent", parent);
        model.add("textures", sides(NS + ":block/marble_bricks"));
        out.add(save(cache, model, "assets/" + NS + "/models/block/" + name + ".json"));
    }

    /** 原版 slab/stairs 父模型使用 bottom/top/side 三个贴图槽。 */
    private static JsonObject sides(String texture) {
        var textures = new JsonObject();
        textures.addProperty("bottom", texture);
        textures.addProperty("top", texture);
        textures.addProperty("side", texture);
        return textures;
    }

    private static String shapeSuffix(String shape) {
        return switch (shape) {
            case "straight" -> "";
            case "inner_left", "inner_right" -> "_inner";
            case "outer_left", "outer_right" -> "_outer";
            default -> "";
        };
    }

    private static JsonObject model(String path) {
        var entry = new JsonObject();
        entry.addProperty("model", NS + ":" + path);
        return entry;
    }

    private static JsonObject parent(String value) {
        var json = new JsonObject();
        json.addProperty("parent", value);
        return json;
    }

    private static JsonObject all(String texture) {
        var textures = new JsonObject();
        textures.addProperty("all", texture);
        return textures;
    }

    private static JsonObject plainItem(String name, boolean explosionDecay) {
        var entry = new JsonObject();
        entry.addProperty("type", "minecraft:item");
        entry.addProperty("name", name);
        if (explosionDecay) entry.add("functions", decayFunctions());
        return entry;
    }

    private static JsonObject conditionalItem(
            String name, String block, String property, String value, boolean explosionDecay) {
        var entry = plainItem(name, explosionDecay);
        var conditions = new JsonArray();
        var condition = new JsonObject();
        condition.addProperty("condition", "minecraft:block_state_property");
        condition.addProperty("block", block);
        var properties = new JsonObject();
        properties.addProperty(property, value);
        condition.add("properties", properties);
        conditions.add(condition);
        entry.add("conditions", conditions);
        return entry;
    }

    private static JsonArray decayFunctions() {
        var functions = new JsonArray();
        var decay = new JsonObject();
        decay.addProperty("function", "minecraft:explosion_decay");
        functions.add(decay);
        return functions;
    }

    private static JsonObject shaped(
            List<String> pattern, java.util.Map<String, String> key, String result, int resultCount) {
        var json = new JsonObject();
        json.addProperty("type", "minecraft:crafting_shaped");
        var patternJson = new JsonArray();
        pattern.forEach(patternJson::add);
        json.add("pattern", patternJson);
        var keyJson = new JsonObject();
        key.forEach(
                (k, v) -> {
                    var value = new JsonObject();
                    value.addProperty("item", v);
                    keyJson.add(k, value);
                });
        json.add("key", keyJson);
        var resultJson = new JsonObject();
        resultJson.addProperty("item", result);
        resultJson.addProperty("count", resultCount);
        json.add("result", resultJson);
        return json;
    }

    private CompletableFuture<?> save(CachedOutput cache, JsonObject json, String path) {
        return DataProvider.saveStable(cache, json, output.getOutputFolder().resolve(path));
    }

    @Override
    public String getName() {
        return "Stellae Omphalos Part-6 decoration families";
    }
}

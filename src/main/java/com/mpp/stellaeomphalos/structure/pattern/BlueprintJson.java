package com.mpp.stellaeomphalos.structure.pattern;

import com.google.gson.*;
import com.mojang.logging.LogUtils;

import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.*;
import java.util.function.Function;

/** Bounded JSON DSL. A bad palette or missing include rejects the entire definition. */
public final class BlueprintJson {
    private BlueprintJson() {}

    public static BlockPos pos(JsonElement json) {
        var a = json.getAsJsonArray();
        if (a.size() != 3) throw new IllegalArgumentException("Expected three coordinates");
        return new BlockPos(integer(a.get(0)), integer(a.get(1)), integer(a.get(2)));
    }

    private static int integer(JsonElement e) {
        int n = e.getAsInt();
        if (e.getAsDouble() != n || Math.abs(n) > 128)
            throw new IllegalArgumentException("Invalid coordinate");
        return n;
    }

    public static BlockRule rule(JsonObject o) {
        if (o.has("block")) {
            var id = new ResourceLocation(o.get("block").getAsString());
            if (!BuiltInRegistries.BLOCK.containsKey(id))
                throw new IllegalArgumentException("Unknown block " + id);
            BlockState s = BuiltInRegistries.BLOCK.get(id).defaultBlockState();
            if (o.has("properties"))
                for (var e : o.getAsJsonObject("properties").entrySet())
                    s = property(s, e.getKey(), e.getValue().getAsString());
            return BlockRule.state(s);
        }
        if (o.has("tag"))
            return BlockRule.tag(
                    TagKey.create(
                            Registries.BLOCK, new ResourceLocation(o.get("tag").getAsString())));
        if (o.has("air") && o.get("air").getAsBoolean()) return BlockRule.air();
        if (o.has("solid") && o.get("solid").getAsBoolean()) return new BlockRule.Solid();
        for (var mode : BlockRule.Composite.Mode.values()) {
            String key = mode.name().toLowerCase(Locale.ROOT);
            if (o.has(key)) {
                var rules = new ArrayList<BlockRule>();
                o.getAsJsonArray(key).forEach(v -> rules.add(rule(v.getAsJsonObject())));
                return new BlockRule.Composite(mode, rules);
            }
        }
        throw new IllegalArgumentException("Unknown block rule");
    }

    private static <T extends Comparable<T>> BlockState set(BlockState s, Property<T> p, String v) {
        return s.setValue(
                p,
                p.getValue(v)
                        .orElseThrow(
                                () -> new IllegalArgumentException("Bad property value " + v)));
    }

    private static BlockState property(BlockState s, String key, String value) {
        var p = s.getBlock().getStateDefinition().getProperty(key);
        if (p == null) throw new IllegalArgumentException("Unknown property " + key);
        return set(s, p, value);
    }

    public static BuildBlueprint decode(
            ResourceLocation id,
            JsonObject root,
            Function<ResourceLocation, BuildBlueprint> includes) {
        if (!root.has("format") || root.get("format").getAsInt() != 1)
            throw new IllegalArgumentException("Unsupported blueprint format");
        var b =
                PatternBlueprintBuilder.named(id)
                        .mirrorable(
                                !root.has("mirrorable") || root.get("mirrorable").getAsBoolean());
        if (root.has("include"))
            b.include(
                    includes.apply(new ResourceLocation(root.get("include").getAsString())),
                    BlockPos.ZERO);
        if (root.has("no_paste")) b.noPaste(root.get("no_paste").getAsBoolean());
        var palette = new HashMap<String, BlockRule>();
        if (root.has("palette"))
            root.getAsJsonObject("palette")
                    .entrySet()
                    .forEach(e -> palette.put(e.getKey(), rule(e.getValue().getAsJsonObject())));
        var placeholders = new HashMap<String, List<BlockRule>>();
        if (root.has("placeholders"))
            root.getAsJsonObject("placeholders")
                    .entrySet()
                    .forEach(
                            e -> {
                                var list = new ArrayList<BlockRule>();
                                e.getValue()
                                        .getAsJsonArray()
                                        .forEach(v -> list.add(rule(v.getAsJsonObject())));
                                placeholders.put(e.getKey(), List.copyOf(list));
                            });
        for (var entry : array(root, "layers")) {
            var l = entry.getAsJsonObject();
            int y = integer(l.get("y"));
            var severity =
                    l.has("severity")
                            ? MismatchSeverity.valueOf(
                                    l.get("severity").getAsString().toUpperCase(Locale.ROOT))
                            : MismatchSeverity.REQUIRED;
            if (l.has("cells"))
                for (var cell : l.getAsJsonArray("cells")) {
                    var c = cell.getAsJsonArray();
                    if (c.size() != 3) throw new IllegalArgumentException("Invalid cell");
                    b.put(
                            new BlockPlacement(
                                    new BlockPos(integer(c.get(0)), y, integer(c.get(1))),
                                    lookup(palette, c.get(2).getAsString()),
                                    severity));
                }
            if (l.has("with")) {
                var r = lookup(palette, l.get("with").getAsString());
                String pattern = l.has("pattern") ? l.get("pattern").getAsString() : "fill";
                int x1, z1, x2, z2;
                if (l.has("fill")) {
                    var a = l.getAsJsonArray("fill");
                    if (a.size() != 4) throw new IllegalArgumentException("Fill rectangle");
                    x1 = Math.min(integer(a.get(0)), integer(a.get(2)));
                    x2 = Math.max(integer(a.get(0)), integer(a.get(2)));
                    z1 = Math.min(integer(a.get(1)), integer(a.get(3)));
                    z2 = Math.max(integer(a.get(1)), integer(a.get(3)));
                } else {
                    int radius = l.has("radius") ? integer(l.get("radius")) : 0;
                    if (radius < 0) throw new IllegalArgumentException("Radius");
                    x1 = z1 = -radius;
                    x2 = z2 = radius;
                }
                if (!Set.of("fill", "ring", "hollow_ring", "cross", "checker").contains(pattern))
                    throw new IllegalArgumentException("Unknown pattern " + pattern);
                for (int x = x1; x <= x2; x++)
                    for (int z = z1; z <= z2; z++)
                        if (switch (pattern) {
                            case "ring", "hollow_ring" -> x == x1 || x == x2 || z == z1 || z == z2;
                            case "cross" -> x == 0 || z == 0;
                            case "checker" -> ((x + z) & 1) == 0;
                            default -> true;
                        }) b.put(new BlockPlacement(new BlockPos(x, y, z), r, severity));
            }
        }
        for (var entry : array(root, "fills")) {
            var o = entry.getAsJsonObject();
            b.cube(
                    pos(o.get("from")),
                    pos(o.get("to")),
                    o.has("air") ? BlockRule.air() : lookup(palette, o.get("with").getAsString()));
        }
        for (var entry : array(root, "placeholder_areas")) {
            var o = entry.getAsJsonObject();
            var allowed = placeholders.get(o.get("name").getAsString());
            if (allowed == null) throw new IllegalArgumentException("Unknown placeholder");
            b.placeholderArea(
                    o.get("name").getAsString(), pos(o.get("from")), pos(o.get("to")), allowed);
        }
        array(root, "unique").forEach(p -> b.unique(pos(p)));
        for (var entry : array(root, "post")) {
            var o = entry.getAsJsonObject();
            String type = o.get("type").getAsString();
            if (type.equals("stellaeomphalos:decay")) {
                float chance = o.get("chance").getAsFloat();
                if (chance < 0 || chance > 1) throw new IllegalArgumentException("Decay chance");
                var rule = rule(o.getAsJsonObject("match"));
                b.post(
                        (level, origin, t, placed, ctx) -> {
                            var rng = ctx.random();
                            for (var p : placed)
                                if (rule.matches(level.getBlockState(p))
                                        && rng.nextFloat() < chance)
                                    level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                        });
            } else if (type.equals("minecraft:chest_loot")) {
                var at = pos(o.get("at"));
                var loot = new ResourceLocation(o.get("loot_table").getAsString());
                b.post(
                        (level, origin, t, placed, ctx) -> {
                            var p = origin.offset(t.apply(at));
                            if (placed.contains(p)
                                    && level.getBlockEntity(p)
                                            instanceof
                                            net.minecraft.world.level.block.entity
                                                                    .RandomizableContainerBlockEntity
                                                            chest)
                                chest.setLootTable(loot, ctx.seed());
                        });
            } else if (type.equals("stellaeomphalos:biome_top_cover")) {
                var chances = o.getAsJsonArray("chance_by_height");
                if (chances.size() != 4)
                    throw new IllegalArgumentException("Cover requires four chances");
                double[] rates = new double[4];
                for (int i = 0; i < 4; i++) {
                    rates[i] = chances.get(i).getAsDouble();
                    if (rates[i] < 0 || rates[i] > 1)
                        throw new IllegalArgumentException("Cover chance");
                }
                b.post(
                        (level, origin, t, placed, ctx) -> {
                            var rng = ctx.random();
                            for (var p : placed)
                                if (rng.nextDouble()
                                                < rates[
                                                        Math.min(
                                                                3,
                                                                Math.max(
                                                                        0,
                                                                        p.getY() - origin.getY()))]
                                        && level.getBlockState(p).isSolid()
                                        && level.hasChunkAt(p.above())
                                        && level.getBlockState(p.above()).isAir())
                                    level.setBlock(
                                            p.above(),
                                            com.mpp.stellaeomphalos.core.platform
                                                    .WorldBehaviorBridge.tables()
                                                    .surfaceCover(level.getBiome(p)),
                                            2);
                        });
            } else if (type.equals("stellaeomphalos:block_entity_state")) {
                var at = pos(o.get("at"));
                var params =
                        com.mojang.serialization.JsonOps.INSTANCE.convertTo(
                                net.minecraft.nbt.NbtOps.INSTANCE, o.get("state"));
                b.post(
                        (level, origin, t, placed, ctx) -> {
                            var p = origin.offset(t.apply(at));
                            if (placed.contains(p)
                                    && level.getBlockEntity(p) != null
                                    && params instanceof net.minecraft.nbt.CompoundTag n) {
                                var be = level.getBlockEntity(p);
                                var data = be.saveWithoutMetadata();
                                data.put("BlueprintState", n.copy());
                                be.load(data);
                                be.setChanged();
                            }
                        });
            } else
                LogUtils.getLogger().warn("Blueprint {} skips unknown post processor {}", id, type);
        }
        return b.build();
    }

    private static BlockRule lookup(Map<String, BlockRule> palette, String key) {
        var r = palette.get(key);
        if (r == null) throw new IllegalArgumentException("Unknown palette key " + key);
        return r;
    }

    private static JsonArray array(JsonObject o, String key) {
        return o.has(key) ? o.getAsJsonArray(key) : new JsonArray();
    }
}

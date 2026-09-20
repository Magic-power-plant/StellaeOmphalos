package com.mpp.stellaeomphalos.data.codec;

import com.google.gson.*;

import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.function.IntFunction;

/** Eleven built-in result capabilities, plus an explicit registration seam for integrations. */
public final class ResultOpRegistry {
    @FunctionalInterface
    public interface Operation {
        void apply(
                JsonObject parameters,
                ItemStack result,
                ItemStack source,
                IntFunction<ItemStack> inputs);
    }

    private static final Map<String, Operation> OPS = new LinkedHashMap<>();

    static {
        register(
                "set_sign",
                (p, r, s, i) ->
                        r.getOrCreateTag()
                                .putString(
                                        "SignId",
                                        RecipeJson.id(p.get("sign").getAsString()).toString()));
        register(
                "set_trait_sign",
                (p, r, s, i) ->
                        r.getOrCreateTag()
                                .putString(
                                        "TraitSignId",
                                        RecipeJson.id(p.get("sign").getAsString()).toString()));
        register(
                "copy_crystal_traits",
                (p, r, s, i) -> r.getOrCreateTag().put("CrystalTraits", traits(s).copy()));
        register(
                "set_tool_traits",
                (p, r, s, i) -> r.getOrCreateTag().put("ToolTraits", traits(s).copy()));
        register(
                "merge_crystal_traits",
                (p, r, s, i) -> {
                    int count = 0, size = 0, purity = 0, collect = 0, fracture = 0;
                    for (int n = 0; n < 25; n++) {
                        var input = i.apply(n);
                        var t = traits(input);
                        if (t.isEmpty()) continue;
                        count++;
                        size += t.getInt("Size");
                        purity += t.getInt("Purity");
                        collect += t.getInt("Collect");
                        fracture = Math.max(fracture, t.getInt("Fracture"));
                    }
                    if (count == 0) return;
                    var t = new CompoundTag();
                    t.putInt("Size", size);
                    t.putInt("Purity", purity / count);
                    t.putInt("Collect", collect / count);
                    t.putInt("Fracture", fracture);
                    t.putInt("MaxCollect", RecipeJson.integer(p, "min_capacity", 100, 1, 100000));
                    r.getOrCreateTag().put("ToolTraits", t);
                });
        register(
                "scale_count_by_traits",
                (p, r, s, i) ->
                        r.setCount(
                                Math.max(
                                        RecipeJson.integer(p, "min", 1, 1, 64),
                                        traits(s).getInt("Size")
                                                / RecipeJson.integer(
                                                        p, "divisor", 80, 1, 100000))));
        register(
                "set_boolean",
                (p, r, s, i) -> {
                    String key = p.get("key").getAsString();
                    if (!key.matches("[A-Z][A-Za-z0-9]*"))
                        throw new JsonParseException("Noncanonical NBT key");
                    r.getOrCreateTag().putBoolean(key, RecipeJson.flag(p, "value", true));
                });
        register(
                "unlock_upgrade",
                (p, r, s, i) -> {
                    String value = RecipeJson.id(p.get("upgrade").getAsString()).toString();
                    var upgrades = r.getOrCreateTag().getList("Upgrades", 8).copy();
                    if (!upgrades.contains(StringTag.valueOf(value)))
                        upgrades.add(StringTag.valueOf(value));
                    r.getOrCreateTag().put("Upgrades", upgrades);
                });
        register(
                "set_lens_color",
                (p, r, s, i) -> r.getOrCreateTag().putInt("LensColor", p.get("color").getAsInt()));
        register(
                "clear_revert_counter",
                (p, r, s, i) -> {
                    r.getOrCreateTag().remove("RevertCounter");
                    r.getOrCreateTag()
                            .putInt(
                                    "ChargeCapacity",
                                    Math.max(100, r.getOrCreateTag().getInt("ChargeCapacity")));
                });
        register(
                "random_enchantment",
                (p, r, s, i) -> {
                    var random = net.minecraft.util.RandomSource.create();
                    String[] choices = {
                        "minecraft:protection",
                        "minecraft:unbreaking",
                        "minecraft:efficiency",
                        "minecraft:sharpness"
                    };
                    var affix = new CompoundTag();
                    affix.putString("Enchantment", choices[random.nextInt(choices.length)]);
                    affix.putInt("Level", 1 + random.nextInt(3));
                    r.getOrCreateTag().put("CharmAffix", affix);
                });
        register(
                "preserve_nbt",
                (p, r, s, i) -> {
                    if (s.hasTag()) r.setTag(s.getTag().copy());
                });
    }

    private ResultOpRegistry() {}

    public static synchronized void register(String id, Operation op) {
        if (OPS.putIfAbsent(id, op) != null)
            throw new IllegalArgumentException("Duplicate result operation");
    }

    private static String name(JsonObject json) {
        var id = RecipeJson.id(RecipeJson.text(json, "op", ""));
        if (!id.getNamespace().equals("stellaeomphalos"))
            throw new JsonParseException("Unknown operation namespace");
        return id.getPath();
    }

    public static void validate(JsonObject json) {
        String name = name(json);
        if (!OPS.containsKey(name)) throw new JsonParseException("Unknown result operation");
        switch (name) {
            case "set_sign", "set_trait_sign" -> RecipeJson.id(json.get("sign").getAsString());
            case "unlock_upgrade" -> RecipeJson.id(json.get("upgrade").getAsString());
            case "scale_count_by_traits" -> {
                RecipeJson.integer(json, "divisor", 80, 1, 100000);
                RecipeJson.integer(json, "min", 1, 1, 64);
            }
            case "merge_crystal_traits" -> RecipeJson.integer(json, "min_capacity", 100, 1, 100000);
            case "set_boolean" -> {
                if (!RecipeJson.text(json, "key", "").matches("[A-Z][A-Za-z0-9]*"))
                    throw new JsonParseException("Noncanonical NBT key");
            }
            case "set_lens_color" -> {
                if (!json.has("color")) throw new JsonParseException("Missing lens color");
                json.get("color").getAsInt();
            }
            default -> {}
        }
    }

    public static Set<String> ids() {
        return Set.copyOf(OPS.keySet());
    }

    public static void apply(
            JsonObject json, ItemStack result, ItemStack source, IntFunction<ItemStack> inputs) {
        OPS.get(name(json)).apply(json, result, source, inputs);
    }

    public static CompoundTag traits(ItemStack stack) {
        if (!stack.hasTag()) return new CompoundTag();
        return stack.getTag().contains("CrystalTraits")
                ? stack.getTag().getCompound("CrystalTraits")
                : stack.getTag().getCompound("ToolTraits");
    }
}

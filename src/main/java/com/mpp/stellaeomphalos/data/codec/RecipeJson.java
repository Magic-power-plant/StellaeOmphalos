package com.mpp.stellaeomphalos.data.codec;

import com.google.gson.*;
import com.mojang.serialization.*;

import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

/** Bounded JSON boundary shared by recipes, network codecs and script mutations. */
public final class RecipeJson {
    public static final Codec<JsonObject> CODEC =
            Codec.PASSTHROUGH.comapFlatMap(
                    value -> {
                        var json = value.convert(JsonOps.INSTANCE).getValue();
                        return json.isJsonObject() && json.toString().length() <= 30000
                                ? DataResult.success(json.getAsJsonObject().deepCopy())
                                : DataResult.error(() -> "Expected bounded recipe object");
                    },
                    value -> new Dynamic<>(JsonOps.INSTANCE, value.deepCopy()));

    private RecipeJson() {}

    public static String text(JsonObject json, String key, String fallback) {
        return json.has(key) ? json.get(key).getAsString() : fallback;
    }

    public static int integer(JsonObject json, String key, int fallback, int min, int max) {
        double number = json.has(key) ? json.get(key).getAsDouble() : fallback;
        if (!Double.isFinite(number) || number != Math.rint(number) || number < min || number > max)
            throw new JsonParseException("Invalid " + key);
        return (int) number;
    }

    public static long amount(JsonObject json, String key, long fallback) {
        try {
            long value =
                    json.has(key) ? json.get(key).getAsBigDecimal().longValueExact() : fallback;
            if (value < 0 || value > 1000000000000L) throw new ArithmeticException();
            return value;
        } catch (ArithmeticException e) {
            throw new JsonParseException("Invalid " + key);
        }
    }

    public static double decimal(
            JsonObject json, String key, double fallback, double min, double max) {
        double value = json.has(key) ? json.get(key).getAsDouble() : fallback;
        if (!Double.isFinite(value) || value < min || value > max)
            throw new JsonParseException("Invalid " + key);
        return value;
    }

    public static boolean flag(JsonObject json, String key, boolean fallback) {
        return json.has(key) ? json.get(key).getAsBoolean() : fallback;
    }

    public static JsonObject object(JsonObject json, String key) {
        return json.has(key) ? json.getAsJsonObject(key) : new JsonObject();
    }

    public static ResourceLocation id(String name) {
        var id = ResourceLocation.tryParse(name);
        if (id == null) throw new JsonParseException("Invalid id " + name);
        return id;
    }

    public static CompoundTag nbt(JsonElement json) {
        if (json == null || json.isJsonNull()) return new CompoundTag();
        try {
            return TagParser.parseTag(
                    json.isJsonPrimitive() ? json.getAsString() : json.toString());
        } catch (Exception e) {
            throw new JsonParseException("Invalid item NBT", e);
        }
    }

    public static ItemStack stack(JsonObject json) {
        if (!json.has("item")) return ItemStack.EMPTY;
        var id = id(json.get("item").getAsString());
        if (!ForgeRegistries.ITEMS.containsKey(id))
            throw new JsonParseException("Unknown item " + id);
        var stack =
                new ItemStack(ForgeRegistries.ITEMS.getValue(id), integer(json, "count", 1, 1, 64));
        if (stack.isEmpty()) throw new JsonParseException("Empty result item");
        if (json.has("nbt")) stack.setTag(nbt(json.get("nbt")));
        return stack;
    }

    public static BlockState blockState(JsonObject json) {
        if (json.has("block_state")) {
            var data = json.getAsJsonObject("block_state");
            return BlockState.CODEC
                    .parse(JsonOps.INSTANCE, data)
                    .getOrThrow(
                            false,
                            message -> {
                                throw new JsonParseException(message);
                            });
        }
        var id = id(text(json, "block", "minecraft:air"));
        if (!ForgeRegistries.BLOCKS.containsKey(id))
            throw new JsonParseException("Unknown block " + id);
        return ForgeRegistries.BLOCKS.getValue(id).defaultBlockState();
    }
}

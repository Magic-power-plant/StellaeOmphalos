package com.mpp.stellaeomphalos.crafting.altar.recipe;

import com.google.gson.JsonObject;
import com.mpp.stellaeomphalos.core.bootstrap.RegistrationGuard;
import com.mpp.stellaeomphalos.core.registry.ModRecipeTypes;
import com.mpp.stellaeomphalos.crafting.grinding.GrindwheelRecipe;
import com.mpp.stellaeomphalos.crafting.infusion.LumenInfusionRecipe;
import com.mpp.stellaeomphalos.crafting.special.*;
import com.mpp.stellaeomphalos.crafting.transmutation.*;
import com.mpp.stellaeomphalos.data.codec.RecipeJson;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.*;

import java.util.*;
import java.util.function.BiFunction;

/** Recipe factories are supplied to the L0 registry holders; no reverse layer dependency. */
public final class RecipeCatalog {
    private static final Map<String, RegistrationGuard<RecipeType<?>>> TYPES =
            new LinkedHashMap<>();
    private static final Map<String, RegistrationGuard<RecipeSerializer<?>>> SERIALIZERS =
            new LinkedHashMap<>();
    private static final Map<String, BiFunction<ResourceLocation, JsonObject, ? extends Recipe<?>>>
            FACTORIES = new LinkedHashMap<>();

    static {
        declare("asterism_crafting", AsterismRecipe::new);
        declare("asterism_upgrade", AsterismUpgradeRecipe::new);
        declare("lumen_infusion", LumenInfusionRecipe::new);
        declare("quern", GrindwheelRecipe::new);
        declare("light_transmutation", LightTransmutationRecipe::new);
        declare("well_liquefaction", WellLiquefactionRecipe::new);
        declare("fluid_interaction", FluidInteractionRecipe::new);
        declare("melting", MeltRecipe::new);
        declare("light_proximity_crafting", LightProximityRecipe::new);
        SERIALIZERS.put(
                "wand_recolor",
                ModRecipeTypes.SERIALIZERS.declare(
                        "wand_recolor",
                        () -> new SimpleCraftingRecipeSerializer<>(WandRecolorRecipe::new)));
    }

    private RecipeCatalog() {}

    private static <R extends Recipe<?>> void declare(
            String name, BiFunction<ResourceLocation, JsonObject, R> factory) {
        TYPES.put(
                name,
                ModRecipeTypes.TYPES.declare(
                        name,
                        () ->
                                new RecipeType<R>() {
                                    public String toString() {
                                        return "stellaeomphalos:" + name;
                                    }
                                }));
        SERIALIZERS.put(
                name, ModRecipeTypes.SERIALIZERS.declare(name, () -> new Serializer<>(factory)));
        FACTORIES.put(name, factory);
    }

    public static void initialize() {}

    public static Set<String> families() {
        return Set.copyOf(TYPES.keySet());
    }

    public static RecipeType<?> type(String name) {
        return TYPES.get(name).get();
    }

    public static RecipeSerializer<?> serializer(String name) {
        return SERIALIZERS.get(name).get();
    }

    public static Recipe<?> parse(ResourceLocation id, JsonObject json) {
        var type = RecipeJson.id(RecipeJson.text(json, "type", ""));
        if (!type.getNamespace().equals("stellaeomphalos")
                || !FACTORIES.containsKey(type.getPath()))
            throw new IllegalArgumentException("Unknown recipe family");
        return FACTORIES.get(type.getPath()).apply(id, bounded(json));
    }

    private static JsonObject bounded(JsonObject json) {
        return RecipeJson.CODEC
                .parse(com.mojang.serialization.JsonOps.INSTANCE, json)
                .getOrThrow(
                        false,
                        message -> {
                            throw new com.google.gson.JsonParseException(message);
                        });
    }

    private record Serializer<R extends Recipe<?>>(
            BiFunction<ResourceLocation, JsonObject, R> factory) implements RecipeSerializer<R> {
        public R fromJson(ResourceLocation id, JsonObject json) {
            return factory.apply(id, bounded(json));
        }

        public R fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return factory.apply(id, buffer.readJsonWithCodec(RecipeJson.CODEC));
        }

        public void toNetwork(FriendlyByteBuf buffer, R recipe) {
            buffer.writeJsonWithCodec(
                    RecipeJson.CODEC,
                    ((com.mpp.stellaeomphalos.core.platform.RecipeDefinition) recipe).definition());
        }
    }
}

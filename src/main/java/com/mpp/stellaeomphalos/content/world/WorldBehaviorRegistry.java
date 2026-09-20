package com.mpp.stellaeomphalos.content.world;

import com.google.gson.*;
import com.mojang.serialization.*;
import com.mpp.stellaeomphalos.core.platform.WorldBehaviorBridge;
import com.mpp.stellaeomphalos.data.loader.CodecDirectoryLoader;

import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.fml.ModList;

import java.util.*;

/** Every behavior dataset is a replaceable datapack snapshot. No built-in runtime lookup table. */
public final class WorldBehaviorRegistry implements WorldBehaviorBridge.Tables {
    private static volatile Map<ResourceLocation, SpringFluidEntry> springs = Map.of();
    private static final Map<String, Map<ResourceLocation, JsonObject>> tables = new HashMap<>();
    public static final Codec<JsonObject> JSON =
            Codec.PASSTHROUGH.comapFlatMap(
                    d -> {
                        var value = d.convert(JsonOps.INSTANCE).getValue();
                        return value.isJsonObject()
                                ? DataResult.success(value.getAsJsonObject())
                                : DataResult.error(() -> "Expected object");
                    },
                    o -> new Dynamic<>(JsonOps.INSTANCE, o));

    public static void reload(AddReloadListenerEvent e) {
        e.addListener(
                new CodecDirectoryLoader<>(
                        "stellaeomphalos/spring_fluid", SpringFluidEntry.CODEC, v -> springs = v));
        for (String name :
                List.of(
                        "herdable",
                        "ore_weight",
                        "vegetation",
                        "tree_species",
                        "surface_cover",
                        "acceleration_policy"))
            e.addListener(
                    new CodecDirectoryLoader<>(
                            "stellaeomphalos/" + name, JSON, v -> tables.put(name, v)));
        for (String name :
                List.of("transmutation", "liquid_interaction", "meltable", "liquefaction"))
            e.addListener(
                    new CodecDirectoryLoader<>(
                            "stellaeomphalos/" + name,
                            JSON,
                            v ->
                                    com.mpp.stellaeomphalos.data.registry.WorldRecipeTables.publish(
                                            name, v)));
    }

    public static List<SpringFluidEntry> springs() {
        return springs.values().stream()
                .filter(
                        e ->
                                net.minecraftforge.registries.ForgeRegistries.FLUIDS.containsKey(
                                                e.fluid())
                                        && (e.requiredMod().isEmpty()
                                                || ModList.get().isLoaded(e.requiredMod())))
                .toList();
    }

    public static int temperature(ResourceLocation fluid) {
        String t =
                springs.values().stream()
                        .filter(e -> e.fluid().equals(fluid))
                        .map(SpringFluidEntry::temperatureClass)
                        .findFirst()
                        .orElse("ANY");
        return switch (t) {
            case "COLD" -> 0;
            case "WARM" -> 1;
            case "HOT" -> 2;
            default -> 3;
        };
    }

    private static List<JsonObject> values(String name) {
        return tables.getOrDefault(name, Map.of()).entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .toList();
    }

    private static int number(JsonObject o, String name, int fallback) {
        return o.has(name) ? o.get(name).getAsInt() : fallback;
    }

    private static Optional<JsonObject> herd(Animal animal) {
        return values("herdable").stream()
                .sorted(
                        Comparator.comparingInt((JsonObject o) -> number(o, "priority", 0))
                                .reversed())
                .filter(
                        o ->
                                o.has("entity")
                                        ? BuiltInRegistries.ENTITY_TYPE
                                                .getKey(animal.getType())
                                                .toString()
                                                .equals(o.get("entity").getAsString())
                                        : o.has("tag")
                                                && animal.getType()
                                                        .is(
                                                                TagKey.create(
                                                                        Registries.ENTITY_TYPE,
                                                                        new ResourceLocation(
                                                                                o.get("tag")
                                                                                        .getAsString()))))
                .findFirst();
    }

    public boolean herdable(Animal animal) {
        return herd(animal).isPresent();
    }

    public List<ItemStack> herdDrops(ServerLevel level, Animal animal) {
        var definition = herd(animal);
        if (definition.isEmpty()) return List.of();
        var o = definition.get();
        if (o.has("item")) {
            var id = new ResourceLocation(o.get("item").getAsString());
            return BuiltInRegistries.ITEM.containsKey(id)
                    ? List.of(new ItemStack(BuiltInRegistries.ITEM.get(id)))
                    : List.of();
        }
        var loot =
                o.has("loot_table")
                        ? new ResourceLocation(o.get("loot_table").getAsString())
                        : animal.getType().getDefaultLootTable();
        var params =
                new LootParams.Builder(level)
                        .withParameter(LootContextParams.THIS_ENTITY, animal)
                        .withParameter(LootContextParams.ORIGIN, animal.position())
                        .withParameter(
                                LootContextParams.DAMAGE_SOURCE, level.damageSources().generic())
                        .withLuck(number(o, "luck_bonus", 0))
                        .create(LootContextParamSets.ENTITY);
        return level.getServer().getLootData().getLootTable(loot).getRandomItems(params).stream()
                .limit(8)
                .toList();
    }

    public static BlockState randomOre(RandomSource random, String dataset) {
        var entries =
                values("ore_weight").stream()
                        .filter(
                                o ->
                                        !o.has("dataset")
                                                || o.get("dataset").getAsString().equals(dataset))
                        .filter(o -> number(o, "weight", 0) > 0)
                        .toList();
        long sum = entries.stream().mapToLong(o -> number(o, "weight", 0)).sum();
        if (sum <= 0) return null;
        for (int attempt = 0; attempt < 150; attempt++) {
            long draw = (long) (random.nextDouble() * sum);
            JsonObject chosen = null;
            for (var entry : entries)
                if ((draw -= number(entry, "weight", 0)) < 0) {
                    chosen = entry;
                    break;
                }
            if (chosen == null) continue;
            if (chosen.has("block")) {
                var id = new ResourceLocation(chosen.get("block").getAsString());
                if (BuiltInRegistries.BLOCK.containsKey(id))
                    return BuiltInRegistries.BLOCK.get(id).defaultBlockState();
            } else if (chosen.has("tag")) {
                var key =
                        TagKey.create(
                                Registries.BLOCK,
                                new ResourceLocation(chosen.get("tag").getAsString()));
                var tag = BuiltInRegistries.BLOCK.getTag(key);
                if (tag.isPresent() && tag.get().size() > 0)
                    return tag.get()
                            .get(random.nextInt(tag.get().size()))
                            .value()
                            .defaultBlockState();
            }
        }
        return null;
    }

    public BlockState ore(RandomSource random, String dataset) {
        return randomOre(random, dataset);
    }

    public static List<BlockState> vegetation(String category) {
        var result = new ArrayList<BlockState>();
        for (var o : values("vegetation"))
            if (category.equals("all")
                    || !o.has("category")
                    || o.get("category").getAsString().equals(category)) {
                var tag =
                        TagKey.create(
                                Registries.BLOCK, new ResourceLocation(o.get("tag").getAsString()));
                BuiltInRegistries.BLOCK
                        .getTag(tag)
                        .ifPresent(
                                s ->
                                        s.forEach(
                                                h ->
                                                        result.addAll(
                                                                h.value()
                                                                        .getStateDefinition()
                                                                        .getPossibleStates())));
            }
        if (result.isEmpty() && !category.equals("all")) return vegetation("all");
        return List.copyOf(result);
    }

    public static boolean treeSpecies(BlockState state, String species) {
        for (var o : values("tree_species"))
            if (o.get("name").getAsString().equals(species))
                for (String kind : List.of("logs", "leaves"))
                    if (state.is(
                            TagKey.create(
                                    Registries.BLOCK,
                                    new ResourceLocation(o.get(kind).getAsString())))) return true;
        return false;
    }

    public BlockState surfaceCover(
            net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biome) {
        BlockState fallback = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        for (var entry : values("surface_cover")) {
            var id = ResourceLocation.tryParse(entry.get("block").getAsString());
            if (id == null || !BuiltInRegistries.BLOCK.containsKey(id)) continue;
            var state = BuiltInRegistries.BLOCK.get(id).defaultBlockState();
            if (entry.has("biome_tag")
                    && biome.is(
                            TagKey.create(
                                    Registries.BIOME,
                                    new ResourceLocation(entry.get("biome_tag").getAsString()))))
                return state;
            if (entry.has("fallback") && entry.get("fallback").getAsBoolean()) fallback = state;
        }
        return fallback;
    }

    public boolean canAccelerate(ServerLevel level, BlockEntity entity) {
        if (entity == null || AccelerationPolicy.get(level).blocked(entity)) return false;
        if (entity.getBlockState()
                .is(
                        TagKey.create(
                                Registries.BLOCK,
                                new ResourceLocation("stellaeomphalos", "no_accelerate"))))
            return false;
        for (var o : values("acceleration_policy")) {
            if (o.has("package_prefix")
                    && entity.getClass()
                            .getName()
                            .startsWith(o.get("package_prefix").getAsString())) return false;
            if (o.has("class")) {
                try {
                    if (Class.forName(
                                    o.get("class").getAsString(),
                                    false,
                                    entity.getClass().getClassLoader())
                            .isAssignableFrom(entity.getClass())) return false;
                } catch (ClassNotFoundException ignored) {
                }
            }
        }
        return true;
    }

    public void failedAcceleration(ServerLevel level, BlockEntity entity, Exception failure) {
        AccelerationPolicy.get(level).record(entity);
    }
}

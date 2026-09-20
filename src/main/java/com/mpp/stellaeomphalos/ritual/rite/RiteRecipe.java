package com.mpp.stellaeomphalos.ritual.rite;

import com.google.gson.*;
import com.mojang.serialization.*;

import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

/** Validated immutable recipe. Item stacks are materialized only at settlement. */
public record RiteRecipe(
        ResourceLocation sign,
        boolean requiresSignRisen,
        int passiveLumenThreshold,
        int minSize,
        int minPurity,
        ResourceLocation tunedTo,
        Map<String, AmplifierRequirement> amplifierSlots,
        int tickInterval,
        float baseIntensity,
        int cycleTicks,
        List<WeightedOutput> outputs,
        int lumenPerCycle,
        List<ResourceLocation> effects,
        CompoundTag effectParams,
        boolean requiresStructureIntact,
        boolean repeating) {
    public record AmplifierRequirement(int min, int max, Set<ResourceLocation> tiers) {
        public AmplifierRequirement {
            tiers = Set.copyOf(tiers);
            if (min < 0 || max < min || max > 64)
                throw new IllegalArgumentException("Amplifier bounds");
        }
    }

    public record WeightedOutput(ResourceLocation item, int min, int max, int weight) {
        public WeightedOutput {
            if (!ForgeRegistries.ITEMS.containsKey(item)
                    || min < 1
                    || max < min
                    || max > 64
                    || weight < 1
                    || weight > 1000000) throw new IllegalArgumentException("Invalid rite output");
        }

        public ItemStack create(net.minecraft.util.RandomSource random) {
            return new ItemStack(
                    ForgeRegistries.ITEMS.getValue(item), min + random.nextInt(max - min + 1));
        }
    }

    public RiteRecipe {
        amplifierSlots = Map.copyOf(amplifierSlots);
        outputs = List.copyOf(outputs);
        effects = List.copyOf(effects);
        effectParams = effectParams.copy();
        if (tickInterval < 1
                || tickInterval > 1200
                || cycleTicks < 1
                || cycleTicks > 24000000
                || lumenPerCycle < 0
                || lumenPerCycle > 100000000
                || baseIntensity < 0
                || baseIntensity > 2
                || !Float.isFinite(baseIntensity)
                || effects.size() > 32
                || outputs.size() > 64) throw new IllegalArgumentException("Rite limits");
    }

    @Override
    public CompoundTag effectParams() {
        return effectParams.copy();
    }

    public static final Codec<RiteRecipe> CODEC =
            Codec.PASSTHROUGH.comapFlatMap(
                    dynamic -> {
                        try {
                            return DataResult.success(
                                    parse(
                                            dynamic.convert(JsonOps.INSTANCE)
                                                    .getValue()
                                                    .getAsJsonObject()));
                        } catch (RuntimeException e) {
                            return DataResult.error(() -> e.getMessage());
                        }
                    },
                    r -> new Dynamic<>(JsonOps.INSTANCE, r.json()));

    public static RiteRecipe parse(JsonObject o) {
        var crystal = o.has("crystal") ? o.getAsJsonObject("crystal") : new JsonObject();
        var slots = new LinkedHashMap<String, AmplifierRequirement>();
        if (o.has("amplifier_slots"))
            o.getAsJsonObject("amplifier_slots")
                    .entrySet()
                    .forEach(
                            e -> {
                                var s = e.getValue().getAsJsonObject();
                                var tiers = new HashSet<ResourceLocation>();
                                if (s.has("tiers"))
                                    s.getAsJsonArray("tiers")
                                            .forEach(
                                                    t ->
                                                            tiers.add(
                                                                    new ResourceLocation(
                                                                            t.getAsString())));
                                slots.put(
                                        e.getKey(),
                                        new AmplifierRequirement(
                                                integer(s, "min", 0), integer(s, "max", 8), tiers));
                            });
        var outputs = new ArrayList<WeightedOutput>();
        if (o.has("outputs"))
            o.getAsJsonArray("outputs")
                    .forEach(
                            v -> {
                                var r = v.getAsJsonObject();
                                outputs.add(
                                        new WeightedOutput(
                                                new ResourceLocation(r.get("item").getAsString()),
                                                integer(r, "min", 1),
                                                integer(r, "max", 1),
                                                integer(r, "weight", 1)));
                            });
        var effects = new ArrayList<ResourceLocation>();
        if (o.has("effects"))
            o.getAsJsonArray("effects")
                    .forEach(v -> effects.add(new ResourceLocation(v.getAsString())));
        var params =
                o.has("effect_params")
                        ? (CompoundTag)
                                JsonOps.INSTANCE.convertTo(NbtOps.INSTANCE, o.get("effect_params"))
                        : new CompoundTag();
        return new RiteRecipe(
                new ResourceLocation(o.get("sign").getAsString()),
                bool(o, "requires_sign_risen", true),
                integer(o, "passive_lumen_threshold", 0),
                integer(crystal, "min_size", 0),
                integer(crystal, "min_purity", 0),
                crystal.has("tuned_to")
                        ? new ResourceLocation(crystal.get("tuned_to").getAsString())
                        : null,
                slots,
                integer(o, "tick_interval", 20),
                o.has("base_intensity") ? o.get("base_intensity").getAsFloat() : 0.35F,
                integer(o, "cycle_ticks", 1200),
                outputs,
                integer(o, "lumen_per_cycle", 0),
                effects,
                params,
                bool(o, "requires_structure_intact", true),
                bool(o, "repeating", true));
    }

    private static int integer(JsonObject o, String key, int fallback) {
        return o.has(key) ? o.get(key).getAsInt() : fallback;
    }

    private static boolean bool(JsonObject o, String k, boolean d) {
        return o.has(k) ? o.get(k).getAsBoolean() : d;
    }

    public JsonObject json() {
        var o = new JsonObject();
        o.addProperty("sign", sign.toString());
        o.addProperty("requires_sign_risen", requiresSignRisen);
        o.addProperty("passive_lumen_threshold", passiveLumenThreshold);
        var c = new JsonObject();
        c.addProperty("min_size", minSize);
        c.addProperty("min_purity", minPurity);
        if (tunedTo != null) c.addProperty("tuned_to", tunedTo.toString());
        o.add("crystal", c);
        var slots = new JsonObject();
        amplifierSlots.forEach(
                (name, s) -> {
                    var entry = new JsonObject();
                    entry.addProperty("min", s.min());
                    entry.addProperty("max", s.max());
                    var tiers = new JsonArray();
                    s.tiers().forEach(t -> tiers.add(t.toString()));
                    entry.add("tiers", tiers);
                    slots.add(name, entry);
                });
        o.add("amplifier_slots", slots);
        o.addProperty("tick_interval", tickInterval);
        o.addProperty("base_intensity", baseIntensity);
        o.addProperty("cycle_ticks", cycleTicks);
        o.addProperty("lumen_per_cycle", lumenPerCycle);
        var out = new JsonArray();
        outputs.forEach(
                v -> {
                    var e = new JsonObject();
                    e.addProperty("item", v.item().toString());
                    e.addProperty("min", v.min());
                    e.addProperty("max", v.max());
                    e.addProperty("weight", v.weight());
                    out.add(e);
                });
        o.add("outputs", out);
        var fx = new JsonArray();
        effects.forEach(e -> fx.add(e.toString()));
        o.add("effects", fx);
        o.add("effect_params", NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE, effectParams));
        o.addProperty("requires_structure_intact", requiresStructureIntact);
        o.addProperty("repeating", repeating);
        return o;
    }
}

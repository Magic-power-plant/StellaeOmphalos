package com.mpp.stellaeomphalos.knowledge.advancement;

import com.google.gson.*;

import net.minecraft.resources.ResourceLocation;

import java.util.*;

/** Empty whitelist means any; malformed elements are errors rather than inverted string tests. */
public record MilestoneCriteria(
        Set<ResourceLocation> names,
        String category,
        int minimum,
        String tier,
        boolean minimumTier) {
    public MilestoneCriteria {
        names = Set.copyOf(names);
        if (minimum < 0) throw new IllegalArgumentException("Negative milestone minimum");
        if (!tier.isEmpty()) tierRank(tier);
    }

    public static MilestoneCriteria parse(JsonObject json) {
        var names = new LinkedHashSet<ResourceLocation>();
        for (String key : List.of("signs", "rites", "recipes", "boons"))
            if (json.has(key)) {
                var list = json.getAsJsonArray(key);
                if (list.size() > 4096) throw new IllegalArgumentException("Whitelist too large");
                for (var value : list) {
                    if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString())
                        throw new IllegalArgumentException("Milestone name must be a string");
                    names.add(new ResourceLocation(value.getAsString()));
                }
            }
        int count =
                json.has("count")
                        ? json.get("count").getAsInt()
                        : json.has("rank") ? json.get("rank").getAsInt() : 0;
        return new MilestoneCriteria(
                names,
                json.has("category") ? json.get("category").getAsString() : "",
                count,
                json.has("tier")
                        ? json.get("tier").getAsString()
                        : json.has("min_tier") ? json.get("min_tier").getAsString() : "",
                json.has("min_tier"));
    }

    private static int tierRank(String value) {
        String name = value.toUpperCase(java.util.Locale.ROOT);
        name =
                switch (name) {
                    case "RESONANCE" -> "ATTUNEMENT";
                    case "SIGN" -> "CONSTELLATION";
                    case "TRAIT" -> "RADIANCE";
                    default -> name;
                };
        return com.mpp.stellaeomphalos.core.platform.StarTier.valueOf(name).ordinal();
    }

    public boolean matches(ResourceLocation subject, String kind, int amount, String actualTier) {
        return (names.isEmpty() || names.contains(subject))
                && (category.isEmpty() || category.equals(kind))
                && amount >= minimum
                && (tier.isEmpty()
                        || (minimumTier
                                ? tierRank(actualTier) >= tierRank(tier)
                                : tierRank(tier) == tierRank(actualTier)));
    }
}

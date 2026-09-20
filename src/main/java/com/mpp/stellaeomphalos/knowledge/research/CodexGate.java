package com.mpp.stellaeomphalos.knowledge.research;

import com.google.gson.*;
import com.mojang.serialization.*;
import com.mpp.stellaeomphalos.core.platform.StarTier;

import net.minecraft.resources.ResourceLocation;

import java.util.*;

/** Immutable expression tree; codecs reject unknown sources and excessive nesting. */
public record CodexGate(
        String source, String value, List<CodexGate> children, GateLevel blockedLevel) {
    public static final Codec<CodexGate> CODEC =
            Codec.PASSTHROUGH.comapFlatMap(
                    dynamic -> {
                        try {
                            return DataResult.success(
                                    parse(dynamic.convert(JsonOps.INSTANCE).getValue(), 0));
                        } catch (RuntimeException e) {
                            return DataResult.error(() -> "Invalid gate: " + e.getMessage());
                        }
                    },
                    gate -> new Dynamic<>(JsonOps.INSTANCE, gate.json()));

    public CodexGate {
        children = List.copyOf(children);
    }

    public static CodexGate any() {
        return new CodexGate("always", "", List.of(), GateLevel.HIDDEN);
    }

    public static CodexGate tier(StarTier tier) {
        return new CodexGate("tier", tier.name(), List.of(), GateLevel.SILHOUETTE);
    }

    public static CodexGate all(CodexGate... gates) {
        return new CodexGate("all", "", List.of(gates), GateLevel.HIDDEN);
    }

    public GateVerdict evaluate(GateContext context) {
        var r = context.record();
        if (!r.valid()) return new GateVerdict(GateLevel.HIDDEN, "invalid_record", List.of());
        if (source.equals("all") || source.equals("any")) {
            var values = children.stream().map(g -> g.evaluate(context)).toList();
            var level = source.equals("all") ? GateLevel.ACTIVE : GateLevel.HIDDEN;
            String reason = "";
            var satisfied = new ArrayList<String>();
            for (var verdict : values) {
                if (source.equals("all")
                        ? verdict.level().compareTo(level) < 0
                        : verdict.level().compareTo(level) > 0) {
                    level = verdict.level();
                    reason = verdict.blockedBy();
                }
                satisfied.addAll(verdict.satisfiedBy());
            }
            return new GateVerdict(level, reason, satisfied);
        }
        boolean allowed =
                switch (source) {
                    case "always" -> true;
                    case "tier" -> r.tier().reaches(StarTier.valueOf(value));
                    case "sign" -> r.knownSigns().contains(new ResourceLocation(value));
                    case "seen_sign" -> r.seenSigns().contains(new ResourceLocation(value));
                    case "node" -> r.researchedNodes().contains(new ResourceLocation(value));
                    case "branch" -> r.branches().contains(value);
                    case "dimension" -> context.dimension().equals(new ResourceLocation(value));
                    case "first_join" -> !r.firstJoinRewarded() == Boolean.parseBoolean(value);
                    case "stage" -> !context.stagesInstalled() || context.stages().contains(value);
                    case "tag" -> context.tags().contains(value);
                    case "not" -> !children.get(0).evaluate(context).active();
                    default -> false;
                };
        String reason = source + ":" + value;
        return new GateVerdict(
                allowed ? GateLevel.ACTIVE : blockedLevel,
                allowed ? "" : reason,
                allowed ? List.of(reason) : List.of());
    }

    public JsonObject json() {
        var json = new JsonObject();
        if (source.equals("always")) return json;
        if (source.equals("all") || source.equals("any")) {
            var array = new JsonArray();
            children.forEach(g -> array.add(g.json()));
            json.add(source, array);
        } else if (source.equals("not")) json.add("not", children.get(0).json());
        else if (source.equals("first_join")) json.addProperty(source, Boolean.parseBoolean(value));
        else json.addProperty(source, value);
        json.addProperty("blocked", blockedLevel.name());
        return json;
    }

    private static CodexGate parse(JsonElement element, int depth) {
        if (depth > 16) throw new IllegalArgumentException("Gate nesting exceeds 16");
        var json = element.getAsJsonObject();
        if (json.size() == 0) return any();
        var keys = new ArrayList<>(json.keySet());
        keys.remove("blocked");
        keys.remove("discovered");
        if (keys.size() != 1) throw new IllegalArgumentException("One gate source required");
        String source = keys.get(0);
        if (!Set.of(
                        "all",
                        "any",
                        "not",
                        "tier",
                        "sign",
                        "seen_sign",
                        "node",
                        "branch",
                        "dimension",
                        "first_join",
                        "stage",
                        "tag")
                .contains(source)) throw new IllegalArgumentException("Unknown source " + source);
        GateLevel blocked =
                json.has("blocked")
                        ? GateLevel.valueOf(json.get("blocked").getAsString())
                        : GateLevel.SILHOUETTE;
        var children = new ArrayList<CodexGate>();
        String value = "";
        if (source.equals("all") || source.equals("any")) {
            var array = json.getAsJsonArray(source);
            if (array.isEmpty() || array.size() > 64)
                throw new IllegalArgumentException("Gate list must contain 1..64 entries");
            array.forEach(e -> children.add(parse(e, depth + 1)));
        } else if (source.equals("not")) children.add(parse(json.get(source), depth + 1));
        else {
            value = json.get(source).getAsString();
            if (source.equals("tier")) StarTier.valueOf(value);
            if (Set.of("sign", "seen_sign", "node", "dimension").contains(source)
                    && (value.isBlank() || ResourceLocation.tryParse(value) == null))
                throw new IllegalArgumentException("Invalid resource id");
            if (source.equals("sign")
                    && json.has("discovered")
                    && !json.get("discovered").getAsBoolean()) source = "seen_sign";
        }
        return new CodexGate(source, value, children, blocked);
    }
}

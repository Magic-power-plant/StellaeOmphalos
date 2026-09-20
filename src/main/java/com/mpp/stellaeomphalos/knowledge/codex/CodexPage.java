package com.mpp.stellaeomphalos.knowledge.codex;

import com.google.gson.*;
import com.mojang.serialization.*;
import com.mpp.stellaeomphalos.knowledge.research.CodexGate;

import net.minecraft.resources.ResourceLocation;

import java.util.*;

/** Page data is independent of physical client classes and disposable page views. */
public record CodexPage(
        PageKind kind,
        String title,
        String body,
        String reference,
        String layout,
        CodexGate visibleWhen,
        boolean sliceable,
        List<Integer> previewShift) {
    public CodexPage {
        previewShift = List.copyOf(previewShift);
        if (previewShift.size() != 3) throw new IllegalArgumentException("Expected xyz shift");
    }

    public static final Codec<CodexPage> CODEC =
            Codec.PASSTHROUGH.comapFlatMap(
                    dynamic -> {
                        try {
                            var j = dynamic.convert(JsonOps.INSTANCE).getValue().getAsJsonObject();
                            var kind =
                                    PageKind.valueOf(
                                            j.get("kind").getAsString().toUpperCase(Locale.ROOT));
                            var gate =
                                    j.has("visible_when")
                                            ? CodexGate.CODEC
                                                    .parse(JsonOps.INSTANCE, j.get("visible_when"))
                                                    .getOrThrow(false, s -> {})
                                            : CodexGate.any();
                            String field =
                                    switch (kind) {
                                        case RECIPE, RECIPE_LIGHT, RECIPE_ALTAR -> "recipe_id";
                                        case STRUCTURE -> "blueprint";
                                        case CELESTIAL -> "sign";
                                        case BOON -> "boon";
                                        default -> "reference";
                                    };
                            String ref = text(j, field);
                            if (Set.of(
                                                    PageKind.RECIPE,
                                                    PageKind.RECIPE_LIGHT,
                                                    PageKind.RECIPE_ALTAR,
                                                    PageKind.STRUCTURE,
                                                    PageKind.CELESTIAL,
                                                    PageKind.BOON)
                                            .contains(kind)
                                    && (ref.isBlank() || ResourceLocation.tryParse(ref) == null))
                                throw new IllegalArgumentException("Missing or invalid " + field);
                            var shift = new ArrayList<Integer>(List.of(0, 0, 0));
                            if (j.has("preview_shift")) {
                                shift.clear();
                                j.getAsJsonArray("preview_shift")
                                        .forEach(v -> shift.add(v.getAsInt()));
                            }
                            return DataResult.success(
                                    new CodexPage(
                                            kind,
                                            text(j, "title"),
                                            text(j, "body"),
                                            ref,
                                            text(j, "layout"),
                                            gate,
                                            j.has("sliceable") && j.get("sliceable").getAsBoolean(),
                                            shift));
                        } catch (RuntimeException e) {
                            return DataResult.error(() -> "Invalid codex page: " + e.getMessage());
                        }
                    },
                    page -> new Dynamic<>(JsonOps.INSTANCE, page.json()));

    private static String text(JsonObject j, String key) {
        return j.has(key) ? j.get(key).getAsString() : "";
    }

    public JsonObject json() {
        var j = new JsonObject();
        j.addProperty("kind", kind.name().toLowerCase(Locale.ROOT));
        j.addProperty("title", title);
        j.addProperty("body", body);
        j.addProperty("layout", layout);
        String key =
                switch (kind) {
                    case RECIPE, RECIPE_LIGHT, RECIPE_ALTAR -> "recipe_id";
                    case STRUCTURE -> "blueprint";
                    case CELESTIAL -> "sign";
                    case BOON -> "boon";
                    default -> "reference";
                };
        j.addProperty(key, reference);
        j.add("visible_when", visibleWhen.json());
        j.addProperty("sliceable", sliceable);
        var shift = new JsonArray();
        previewShift.forEach(shift::add);
        j.add("preview_shift", shift);
        return j;
    }
}

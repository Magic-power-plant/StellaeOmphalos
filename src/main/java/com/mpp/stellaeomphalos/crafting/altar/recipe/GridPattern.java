package com.mpp.stellaeomphalos.crafting.altar.recipe;

import com.google.gson.*;
import com.mpp.stellaeomphalos.data.codec.*;

import net.minecraft.world.Container;

import java.util.*;

/**
 * Original shaped matching: crop, sliding windows and mirrored patterns, with strict empty cells.
 */
public final class GridPattern {
    private final Map<Integer, MaterialSpec> cells;
    private final int width, height;
    private final boolean shrink;

    public GridPattern(JsonObject json) {
        var slots = new TreeMap<Integer, MaterialSpec>();
        if (json.has("grid_literal")) {
            var literal = json.getAsJsonArray("grid_literal");
            if (literal.size() != 9) throw new JsonParseException("grid_literal must have 9 cells");
            for (int i = 0; i < 9; i++)
                if (!literal.get(i).isJsonNull()) slots.put(i, MaterialSpec.parse(literal.get(i)));
            width = 3;
            height = 3;
            shrink = false;
        } else {
            var grid = RecipeJson.object(json, "grid");
            var pattern = grid.getAsJsonArray("pattern");
            var keys = RecipeJson.object(grid, "key");
            if (pattern == null || pattern.isEmpty() || pattern.size() > 3)
                throw new JsonParseException("Invalid grid height");
            int w = pattern.get(0).getAsString().length();
            if (w < 1 || w > 3) throw new JsonParseException("Invalid grid width");
            for (int y = 0; y < pattern.size(); y++) {
                String row = pattern.get(y).getAsString();
                if (row.length() != w) throw new JsonParseException("Ragged pattern");
                for (int x = 0; x < w; x++)
                    if (row.charAt(x) != ' ') {
                        var material = keys.get(String.valueOf(row.charAt(x)));
                        if (material == null)
                            throw new JsonParseException("Undefined pattern symbol");
                        slots.put(y * 3 + x, MaterialSpec.parse(material));
                    }
            }
            shrink = RecipeJson.flag(grid, "shrink", true);
            if (shrink && !slots.isEmpty()) {
                int minX = slots.keySet().stream().mapToInt(i -> i % 3).min().orElse(0),
                        minY = slots.firstKey() / 3;
                int maxX = slots.keySet().stream().mapToInt(i -> i % 3).max().orElse(0),
                        maxY = slots.lastKey() / 3;
                var cropped = new TreeMap<Integer, MaterialSpec>();
                slots.forEach((i, m) -> cropped.put((i / 3 - minY) * 3 + i % 3 - minX, m));
                slots = cropped;
                width = maxX - minX + 1;
                height = maxY - minY + 1;
            } else {
                width = 3;
                height = 3;
            }
        }
        if (slots.isEmpty()) throw new JsonParseException("Empty altar pattern");
        cells = Map.copyOf(slots);
    }

    public Map<Integer, MaterialSpec> display() {
        return cells;
    }

    public Optional<Map<Integer, MaterialSpec>> match(Container input) {
        for (int y = 0; y <= 3 - height; y++)
            for (int x = 0; x <= 3 - width; x++)
                for (boolean mirror : new boolean[] {false, true}) {
                    var mapped = new TreeMap<Integer, MaterialSpec>();
                    final int dx = x, dy = y;
                    cells.forEach(
                            (slot, spec) ->
                                    mapped.put(
                                            (slot / 3 + dy) * 3
                                                    + dx
                                                    + (mirror ? width - 1 - slot % 3 : slot % 3),
                                            spec));
                    boolean valid = true;
                    for (int slot = 0; slot < 9; slot++) {
                        var spec = mapped.get(slot);
                        if (spec == null
                                ? !input.getItem(slot).isEmpty()
                                : !spec.test(input.getItem(slot))) {
                            valid = false;
                            break;
                        }
                    }
                    if (valid) return Optional.of(Map.copyOf(mapped));
                }
        return Optional.empty();
    }
}

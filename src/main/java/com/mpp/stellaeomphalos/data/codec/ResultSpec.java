package com.mpp.stellaeomphalos.data.codec;

import com.google.gson.*;

import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/** Immutable result description. Every evaluation returns a fresh stack. */
public final class ResultSpec {
    private final String kind;
    private final int source;
    private final ItemStack display;
    private final List<JsonObject> operations;

    public ResultSpec(JsonObject json) {
        kind = RecipeJson.text(json, "kind", "static");
        if (!Set.of("static", "inherit", "empty").contains(kind))
            throw new JsonParseException("Unknown result kind");
        source = slot(RecipeJson.text(json, "source_slot", "center"));
        display =
                RecipeJson.stack(
                        RecipeJson.object(json, kind.equals("inherit") ? "fallback" : "stack"));
        var ops = new ArrayList<JsonObject>();
        if (json.has("ops"))
            for (var value : json.getAsJsonArray("ops")) {
                var op = value.getAsJsonObject().deepCopy();
                ResultOpRegistry.validate(op);
                ops.add(op);
            }
        operations = List.copyOf(ops);
        if (!kind.equals("empty") && display.isEmpty())
            throw new JsonParseException("Result needs a nonempty display stack");
    }

    public static int slot(String name) {
        return switch (name) {
            case "upper_left" -> 0;
            case "upper_center" -> 1;
            case "upper_right" -> 2;
            case "left_center" -> 3;
            case "center" -> 4;
            case "right_center" -> 5;
            case "lower_left" -> 6;
            case "lower_center" -> 7;
            case "lower_right" -> 8;
            case "focus" -> 25;
            case "input" -> 0;
            default -> {
                try {
                    int n = Integer.parseInt(name);
                    if (n < 0 || n > 25) throw new NumberFormatException();
                    yield n;
                } catch (NumberFormatException e) {
                    throw new JsonParseException("Invalid source slot " + name);
                }
            }
        };
    }

    public ItemStack displayStack() {
        return display.copy();
    }

    public boolean empty() {
        return kind.equals("empty");
    }

    public ItemStack assemble(java.util.function.IntFunction<ItemStack> inputs) {
        if (empty()) return ItemStack.EMPTY;
        ItemStack original = inputs.apply(source);
        ItemStack output =
                kind.equals("inherit") && !original.isEmpty()
                        ? original.copyWithCount(1)
                        : display.copy();
        for (var op : operations) ResultOpRegistry.apply(op, output, original, inputs);
        output.setCount(Math.min(output.getMaxStackSize(), Math.max(1, output.getCount())));
        return output;
    }
}

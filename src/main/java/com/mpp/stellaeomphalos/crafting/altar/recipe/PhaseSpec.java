package com.mpp.stellaeomphalos.crafting.altar.recipe;

import com.google.gson.JsonObject;
import com.mpp.stellaeomphalos.data.codec.RecipeJson;

import net.minecraft.nbt.CompoundTag;

public record PhaseSpec(Kind kind, double fraction, int timeout, CompoundTag payload) {
    public enum Kind {
        BIND_RELAY,
        CONSUME_RELAY,
        EMIT_EFFECT,
        GATE_CHECK
    }

    public PhaseSpec {
        if (!Double.isFinite(fraction) || fraction < 0 || fraction > 1 || timeout < 1)
            throw new IllegalArgumentException("Invalid phase");
        payload = payload.copy();
    }

    @Override
    public CompoundTag payload() {
        return payload.copy();
    }

    public static PhaseSpec parse(JsonObject json) {
        return new PhaseSpec(
                Kind.valueOf(
                        RecipeJson.text(json, "kind", "GATE_CHECK")
                                .toUpperCase(java.util.Locale.ROOT)),
                RecipeJson.decimal(json, "at_fraction", 0, 0, 1),
                RecipeJson.integer(json, "timeout", 100, 1, 20000),
                RecipeJson.nbt(json.get("payload")));
    }
}

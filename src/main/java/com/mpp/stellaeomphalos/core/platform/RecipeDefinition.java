package com.mpp.stellaeomphalos.core.platform;

import com.google.gson.JsonObject;

import net.minecraft.resources.ResourceLocation;

/** Lower-layer indexing surface keeps the recipe hub independent of crafting implementations. */
public interface RecipeDefinition extends RecipeDisplayData {
    ResourceLocation family();

    JsonObject definition();

    long contentHash();

    default int priority() {
        return 0;
    }
}

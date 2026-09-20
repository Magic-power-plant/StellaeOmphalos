package com.mpp.stellaeomphalos.core.platform;

/** Optional integrations append deterministic mutations on every datapack reload. */
public final class RecipeScriptEvent extends net.minecraftforge.eventbus.api.Event {
    private final RecipeMutationApi recipes;

    public RecipeScriptEvent(RecipeMutationApi recipes) {
        this.recipes = recipes;
    }

    public RecipeMutationApi recipes() {
        return recipes;
    }
}

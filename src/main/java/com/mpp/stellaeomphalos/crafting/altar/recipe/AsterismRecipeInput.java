package com.mpp.stellaeomphalos.crafting.altar.recipe;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/** Snapshot used for matching and simulation; no player menu or mutable machine reference. */
public final class AsterismRecipeInput extends SimpleContainer {
    private final AsterismTier tier;
    private final long lumen;
    private final ResourceLocation focus;
    private final java.util.function.Predicate<ResourceLocation> advancement;

    public AsterismRecipeInput(
            AsterismTier tier,
            long lumen,
            ResourceLocation focus,
            java.util.function.Predicate<ResourceLocation> advancement,
            List<ItemStack> stacks) {
        super(26);
        this.tier = tier;
        this.lumen = lumen;
        this.focus = focus;
        this.advancement = advancement;
        for (int i = 0; i < Math.min(26, stacks.size()); i++) setItem(i, stacks.get(i).copy());
    }

    public AsterismTier tier() {
        return tier;
    }

    public long lumen() {
        return lumen;
    }

    public boolean focused(ResourceLocation sign) {
        return sign.equals(focus);
    }

    public boolean unlocked(ResourceLocation id) {
        return advancement.test(id);
    }
}

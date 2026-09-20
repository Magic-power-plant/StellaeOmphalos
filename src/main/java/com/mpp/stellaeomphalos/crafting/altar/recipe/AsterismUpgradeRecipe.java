package com.mpp.stellaeomphalos.crafting.altar.recipe;

import com.google.gson.*;
import com.mpp.stellaeomphalos.data.codec.RecipeJson;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class AsterismUpgradeRecipe extends AsterismRecipe {
    private final AsterismTier target;
    private final ItemStack display;

    public AsterismUpgradeRecipe(ResourceLocation id, JsonObject json) {
        super(id, "asterism_upgrade", json);
        target = AsterismTier.parse(RecipeJson.text(json, "to_tier", ""));
        if (target.ordinal() != tier().ordinal() + 1
                || target == AsterismTier.RADIANCE
                || !flag("night_only")
                || !flag("no_item_output")) throw new JsonParseException("Invalid altar upgrade");
        display = RecipeJson.stack(RecipeJson.object(json, "result_display"));
        if (display.isEmpty()) throw new JsonParseException("Upgrade display missing");
    }

    public AsterismTier target() {
        return target;
    }

    @Override
    public boolean matches(Container input, Level level) {
        return input instanceof AsterismRecipeInput ctx
                && ctx.tier() == tier()
                && super.matches(input, level);
    }

    @Override
    public ItemStack getResultItem(RegistryAccess access) {
        return display.copy();
    }

    @Override
    public ItemStack assemble(Container input, RegistryAccess access) {
        return ItemStack.EMPTY;
    }
}

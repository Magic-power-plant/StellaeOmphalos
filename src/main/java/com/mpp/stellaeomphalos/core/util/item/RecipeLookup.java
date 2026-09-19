package com.mpp.stellaeomphalos.core.util.item;

import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

public final class RecipeLookup {
    private RecipeLookup() {}
    public static <C extends Container, T extends Recipe<C>> List<T> byOutput(RecipeManager manager, RecipeType<T> type, RegistryAccess access, ItemStack output) {
        return manager.getAllRecipesFor(type).stream().filter(recipe -> ItemStack.isSameItemSameTags(recipe.getResultItem(access), output)).toList();
    }
}

package com.mpp.stellaeomphalos.crafting.special;

import com.mpp.stellaeomphalos.crafting.altar.recipe.RecipeCatalog;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

/** Exactly two occupied cells; recoloring deliberately creates a fresh wand. */
public final class WandRecolorRecipe extends CustomRecipe {
    public WandRecolorRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer input, Level level) {
        int wand = 0, dye = 0;
        for (int i = 0; i < input.getContainerSize(); i++) {
            var s = input.getItem(i);
            if (s.isEmpty()) continue;
            if (new ResourceLocation("stellaeomphalos", "luminary_rod")
                    .equals(ForgeRegistries.ITEMS.getKey(s.getItem()))) wand++;
            else if (s.getItem() instanceof DyeItem) dye++;
            else return false;
        }
        return wand == 1 && dye == 1;
    }

    @Override
    public ItemStack assemble(CraftingContainer input, RegistryAccess access) {
        Item item = Items.AIR;
        int color = 0;
        for (int i = 0; i < input.getContainerSize(); i++) {
            var s = input.getItem(i);
            if (s.isEmpty()) continue;
            if (s.getItem() instanceof DyeItem dye) color = dye.getDyeColor().getTextColor();
            else item = s.getItem();
        }
        var output = new ItemStack(item);
        output.getOrCreateTag().putInt("WandColor", color);
        return output;
    }

    @Override
    public boolean canCraftInDimensions(int w, int h) {
        return w * h >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeCatalog.serializer("wand_recolor");
    }
}

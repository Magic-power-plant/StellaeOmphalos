package com.mpp.stellaeomphalos.crafting.special;

import com.google.gson.JsonObject;
import com.mpp.stellaeomphalos.core.platform.RecipeDefinition;
import com.mpp.stellaeomphalos.crafting.altar.recipe.RecipeCatalog;
import com.mpp.stellaeomphalos.data.codec.*;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

import java.util.*;

/** Uses the vanilla crafting type so the workbench actually discovers this recipe. */
public final class LightProximityRecipe implements CraftingRecipe, RecipeDefinition {
    private final ShapedRecipe shaped;
    private final JsonObject json;
    private final long lumen;
    private final int distance;

    public LightProximityRecipe(ResourceLocation id, JsonObject json) {
        this.json = json.deepCopy();
        shaped = new ShapedRecipe.Serializer().fromJson(id, json);
        lumen = RecipeJson.amount(json, "min_lumen", 1);
        distance = RecipeJson.integer(json, "max_distance", 32, 1, 64);
    }

    @Override
    public boolean matches(CraftingContainer input, Level level) {
        if (input.getWidth() != 3 || input.getHeight() != 3 || !shaped.matches(input, level))
            return false;
        if (level instanceof net.minecraft.server.level.ServerLevel server
                && com.mpp.stellaeomphalos.crafting.altar.recipe.CraftingBootstrap.hub(
                                server.getServer())
                        .byId(getId())
                        .filter(r -> r == this)
                        .isEmpty()) return false;
        for (var player : level.players())
            if (player.containerMenu instanceof WorkbenchAnchorMenu menu
                    && menu.owns(input)
                    && menu.illuminated(lumen, distance)) return true;
        return false;
    }

    @Override
    public ItemStack assemble(CraftingContainer input, RegistryAccess access) {
        return shaped.assemble(input, access);
    }

    @Override
    public ItemStack getResultItem(RegistryAccess access) {
        return shaped.getResultItem(access).copy();
    }

    @Override
    public boolean canCraftInDimensions(int w, int h) {
        return w == 3 && h == 3;
    }

    @Override
    public ResourceLocation getId() {
        return shaped.getId();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeCatalog.serializer("light_proximity_crafting");
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }

    @Override
    public ResourceLocation family() {
        return new ResourceLocation("stellaeomphalos", "light_proximity_crafting");
    }

    @Override
    public JsonObject definition() {
        return json.deepCopy();
    }

    @Override
    public long contentHash() {
        return com.google.common.hash.Hashing.sha256().hashUnencodedChars(json.toString()).asLong();
    }

    @Override
    public List<ResourceLocation> catalysts() {
        return List.of(new ResourceLocation("minecraft", "crafting_table"));
    }

    @Override
    public Map<Integer, MaterialSpec> displaySlots() {
        var result = new TreeMap<Integer, MaterialSpec>();
        var ingredients = shaped.getIngredients();
        for (int i = 0; i < ingredients.size(); i++)
            if (!ingredients.get(i).isEmpty())
                result.put(
                        i,
                        new MaterialSpec.ItemMaterial(
                                ingredients.get(i),
                                new net.minecraft.nbt.CompoundTag(),
                                new net.minecraft.nbt.CompoundTag()));
        return Map.copyOf(result);
    }

    @Override
    public List<ItemStack> displayOutputs() {
        return List.of(getResultItem(RegistryAccess.EMPTY));
    }

    @Override
    public int displayDuration() {
        return 0;
    }

    @Override
    public Map<String, Number> displayScalars() {
        return Map.of("min_lumen", lumen, "max_distance", distance);
    }

    @Override
    public ResourceLocation displayCategory() {
        return new ResourceLocation("minecraft", "crafting");
    }

    @Override
    public boolean hidden() {
        return false;
    }
}

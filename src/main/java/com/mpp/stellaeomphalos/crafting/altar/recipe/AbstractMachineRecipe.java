package com.mpp.stellaeomphalos.crafting.altar.recipe;

import com.google.gson.JsonObject;
import com.mpp.stellaeomphalos.core.platform.RecipeDefinition;
import com.mpp.stellaeomphalos.data.codec.*;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

import java.util.*;

/** Immutable identity and serialization shared by the eight machine recipe families. */
public abstract class AbstractMachineRecipe implements Recipe<Container>, RecipeDefinition {
    private final ResourceLocation id;
    private final ResourceLocation family;
    private final JsonObject json;
    private final long hash;
    protected final ResultSpec result;

    protected AbstractMachineRecipe(ResourceLocation id, String family, JsonObject json) {
        this.id = id;
        this.family = new ResourceLocation("stellaeomphalos", family);
        this.json = json.deepCopy();
        this.hash =
                com.google.common.hash.Hashing.sha256()
                        .hashUnencodedChars(this.json.toString())
                        .asLong();
        var output =
                Set.of("asterism_crafting", "asterism_upgrade", "lumen_infusion", "grindwheel")
                                .contains(family)
                        ? RecipeJson.object(json, "result")
                        : new JsonObject();
        if (!output.has("kind")) {
            var wrapper = new JsonObject();
            wrapper.addProperty("kind", output.has("item") ? "static" : "empty");
            wrapper.add("stack", output);
            output = wrapper;
        }
        result = new ResultSpec(output);
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public ResourceLocation family() {
        return family;
    }

    @Override
    public JsonObject definition() {
        return json.deepCopy();
    }

    @Override
    public long contentHash() {
        return hash;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeCatalog.serializer(family.getPath());
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeCatalog.type(family.getPath());
    }

    @Override
    public ItemStack assemble(Container input, RegistryAccess access) {
        return result.assemble(
                slot -> slot < input.getContainerSize() ? input.getItem(slot) : ItemStack.EMPTY);
    }

    @Override
    public ItemStack getResultItem(RegistryAccess access) {
        return result.displayStack();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public String getGroup() {
        return RecipeJson.text(json, "group", "");
    }

    @Override
    public ResourceLocation displayCategory() {
        return family;
    }

    @Override
    public List<ResourceLocation> catalysts() {
        return List.of(family);
    }

    @Override
    public List<ItemStack> displayOutputs() {
        var output = getResultItem(RegistryAccess.EMPTY);
        return output.isEmpty() ? List.of() : List.of(output);
    }

    @Override
    public int displayDuration() {
        return RecipeJson.integer(json, "duration", 1, 1, 1000000);
    }

    @Override
    public Map<String, Number> displayScalars() {
        return Map.of();
    }

    @Override
    public boolean hidden() {
        return RecipeJson.flag(json, "hidden", false);
    }

    @Override
    public boolean matches(Container input, Level level) {
        return displaySlots().entrySet().stream()
                .allMatch(
                        e ->
                                e.getKey() < input.getContainerSize()
                                        && e.getValue().test(input.getItem(e.getKey())));
    }
}

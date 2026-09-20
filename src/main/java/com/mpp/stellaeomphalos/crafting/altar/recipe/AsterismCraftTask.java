package com.mpp.stellaeomphalos.crafting.altar.recipe;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

/** Task-owned relay claims and private recipe state survive chunk unloading. */
public final class AsterismCraftTask extends AbstractCraftTask {
    private final Map<Integer, BlockPos> bindings = new TreeMap<>();
    private CompoundTag recipeData = new CompoundTag();

    public AsterismCraftTask(ResourceLocation id, long hash, int ticks, UUID crafter) {
        super(id, hash, ticks, crafter);
    }

    public Map<Integer, BlockPos> bindings() {
        return Map.copyOf(bindings);
    }

    public void bind(int index, BlockPos pos) {
        bindings.put(index, pos.immutable());
    }

    public void unbind(int index) {
        bindings.remove(index);
    }

    public CompoundTag recipeData() {
        return recipeData.copy();
    }

    public void recipeData(CompoundTag data) {
        recipeData = data.copy();
    }

    @Override
    protected void onRecipeChanged() {
        bindings.clear();
        recipeData.remove("Phase");
        recipeData.remove("PhaseWait");
    }

    @Override
    public CompoundTag save() {
        var tag = super.save();
        var list = new net.minecraft.nbt.ListTag();
        bindings.forEach(
                (index, pos) -> {
                    var entry = new CompoundTag();
                    entry.putInt("Index", index);
                    entry.putLong("Position", pos.asLong());
                    list.add(entry);
                });
        tag.put("Bindings", list);
        tag.put("RecipeData", recipeData.copy());
        return tag;
    }

    public static Optional<AsterismCraftTask> load(CompoundTag tag) {
        var id = ResourceLocation.tryParse(tag.getString("Recipe"));
        if (id == null) return Optional.empty();
        var result =
                new AsterismCraftTask(
                        id,
                        tag.getLong("Hash"),
                        Math.max(1, tag.getInt("Total")),
                        tag.hasUUID("Crafter") ? tag.getUUID("Crafter") : new UUID(0, 0));
        result.restore(tag);
        result.recipeData = tag.getCompound("RecipeData").copy();
        var bindings = tag.getList("Bindings", 10);
        for (int i = 0; i < Math.min(128, bindings.size()); i++) {
            var e = bindings.getCompound(i);
            result.bind(e.getInt("Index"), BlockPos.of(e.getLong("Position")));
        }
        return Optional.of(result);
    }
}

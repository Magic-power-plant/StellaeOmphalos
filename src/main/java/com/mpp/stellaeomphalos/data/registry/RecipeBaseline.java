package com.mpp.stellaeomphalos.data.registry;

import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

/** Only identities and audit metadata persist; recipe content remains in datapacks. */
public final class RecipeBaseline extends SavedData {
    private Set<ResourceLocation> ids = Set.of(),
            removed = Set.of(),
            added = Set.of(),
            disabled = Set.of();
    private int epoch;

    public void record(
            Set<ResourceLocation> ids,
            Set<ResourceLocation> removed,
            Set<ResourceLocation> added,
            Set<ResourceLocation> disabled) {
        this.ids = Set.copyOf(ids);
        this.removed = Set.copyOf(removed);
        this.added = Set.copyOf(added);
        this.disabled = Set.copyOf(disabled);
        epoch++;
        setDirty();
    }

    public int epoch() {
        return epoch;
    }

    private static ListTag list(Set<ResourceLocation> values) {
        var list = new ListTag();
        values.stream().sorted().forEach(v -> list.add(StringTag.valueOf(v.toString())));
        return list;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.put("BaselineIds", list(ids));
        tag.put("ScriptRemoved", list(removed));
        tag.put("ScriptAdded", list(added));
        tag.put("FamilyDisabled", list(disabled));
        tag.putInt("Epoch", epoch);
        return tag;
    }

    private static Set<ResourceLocation> read(CompoundTag tag, String key) {
        var result = new HashSet<ResourceLocation>();
        for (var value : tag.getList(key, 8)) {
            var id = ResourceLocation.tryParse(value.getAsString());
            if (id != null) result.add(id);
        }
        return Set.copyOf(result);
    }

    public static RecipeBaseline load(CompoundTag tag) {
        var result = new RecipeBaseline();
        result.ids = read(tag, "BaselineIds");
        result.removed = read(tag, "ScriptRemoved");
        result.added = read(tag, "ScriptAdded");
        result.disabled = read(tag, "FamilyDisabled");
        result.epoch = Math.max(0, tag.getInt("Epoch"));
        return result;
    }
}

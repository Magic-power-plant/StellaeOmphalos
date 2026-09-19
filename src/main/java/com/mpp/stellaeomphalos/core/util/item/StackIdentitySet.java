package com.mpp.stellaeomphalos.core.util.item;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** Keys are defensive serialized copies excluding count, including capability state. */
public final class StackIdentitySet {
    private final Map<CompoundTag, ItemStack> entries = new LinkedHashMap<>();
    public boolean add(ItemStack stack) {
        if (stack.isEmpty()) return false;
        var key = stack.save(new CompoundTag()); key.remove("Count");
        var copy = stack.copy(); copy.setCount(1);
        return entries.putIfAbsent(key, copy) == null;
    }
    public List<ItemStack> snapshot() { return entries.values().stream().map(ItemStack::copy).toList(); }
    public int size() { return entries.size(); }
    public void clear() { entries.clear(); }
}

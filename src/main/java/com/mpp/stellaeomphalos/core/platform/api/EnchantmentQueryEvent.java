package com.mpp.stellaeomphalos.core.platform.api;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.eventbus.api.Event;

/** Version 1 extension event. Runs synchronously on the caller's thread; listeners must not mutate worlds. */
public final class EnchantmentQueryEvent extends Event {
    private final ItemStack stack;
    private final Map<Enchantment, Integer> levels;
    /** Receives an item and a copied level map. */
    public EnchantmentQueryEvent(ItemStack stack, Map<Enchantment, Integer> levels) {
        this.stack = stack.copy(); this.levels = new LinkedHashMap<>(levels);
    }
    /** Returns a defensive item copy. */
    public ItemStack stack() { return stack.copy(); }
    /** Sets a level, clamped to the representable enchantment range. */
    public void setLevel(Enchantment enchantment, int level) {
        java.util.Objects.requireNonNull(enchantment);
        if (level <= 0) levels.remove(enchantment); else levels.put(enchantment, Math.min(255, level));
    }
    /** Returns an immutable view of the query result. */
    public Map<Enchantment, Integer> levels() { return Map.copyOf(levels); }
}

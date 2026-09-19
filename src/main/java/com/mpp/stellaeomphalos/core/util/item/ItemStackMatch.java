package com.mpp.stellaeomphalos.core.util.item;

import java.util.Set;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.ItemStack;

public final class ItemStackMatch {
    public enum Rule { ITEM, AMOUNT_EXACT, AMOUNT_AT_MOST, TAG_STRICT, TAG_SUBSET, CAPS_COMPATIBLE }
    public static final Set<Rule> STRICT = Set.of(Rule.ITEM, Rule.AMOUNT_EXACT, Rule.TAG_STRICT, Rule.CAPS_COMPATIBLE);
    private ItemStackMatch() {}
    public static boolean matches(ItemStack expected, ItemStack actual, Set<Rule> rules) {
        if (rules.contains(Rule.AMOUNT_EXACT) && rules.contains(Rule.AMOUNT_AT_MOST)
                || rules.contains(Rule.TAG_STRICT) && rules.contains(Rule.TAG_SUBSET)) throw new IllegalArgumentException("Conflicting match rules");
        if (expected.isEmpty() || actual.isEmpty()) return expected.isEmpty() && actual.isEmpty();
        if (rules.contains(Rule.ITEM) && expected.getItem() != actual.getItem()) return false;
        if (rules.contains(Rule.AMOUNT_EXACT) && expected.getCount() != actual.getCount()) return false;
        if (rules.contains(Rule.AMOUNT_AT_MOST) && actual.getCount() > expected.getCount()) return false;
        if (rules.contains(Rule.TAG_STRICT) && !java.util.Objects.equals(expected.getTag(), actual.getTag())) return false;
        if (rules.contains(Rule.TAG_SUBSET) && !NbtUtils.compareNbt(expected.getTag(), actual.getTag(), true)) return false;
        return !rules.contains(Rule.CAPS_COMPATIBLE) || expected.areCapsCompatible(actual);
    }
}

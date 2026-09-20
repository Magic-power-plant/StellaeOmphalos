package com.mpp.stellaeomphalos.crafting.grinding;

import net.minecraft.world.item.ItemStack;

public record GrindOutcome(Kind kind, ItemStack output) {
    public enum Kind {
        UNCHANGED_SUCCESS,
        ITEM_CHANGE,
        FAIL_SILENT,
        FAIL_BREAK_ITEM
    }
}

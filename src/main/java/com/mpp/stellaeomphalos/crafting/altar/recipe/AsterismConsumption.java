package com.mpp.stellaeomphalos.crafting.altar.recipe;

import com.mpp.stellaeomphalos.data.codec.MaterialSpec;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

import java.util.*;

/** Entire plan validates before writes. Rollback includes every previously changed slot. */
public final class AsterismConsumption {
    public record SlotChange(
            IItemHandlerModifiable inventory, int slot, ItemStack before, ItemStack after) {
        public SlotChange {
            before = before.copy();
            after = after.copy();
        }

        @Override
        public ItemStack before() {
            return before.copy();
        }

        @Override
        public ItemStack after() {
            return after.copy();
        }
    }

    private AsterismConsumption() {}

    public static SlotChange simulate(
            IItemHandlerModifiable inventory, int slot, MaterialSpec material, boolean consume) {
        var before = inventory.getStackInSlot(slot).copy();
        if (!material.test(before)) throw new IllegalStateException("Material changed");
        return new SlotChange(
                inventory, slot, before, consume ? material.consumeOne(before) : before);
    }

    public static boolean apply(List<SlotChange> changes) {
        var slots = new IdentityHashMap<IItemHandlerModifiable, Set<Integer>>();
        for (var change : changes) {
            if (!slots.computeIfAbsent(change.inventory(), k -> new HashSet<>()).add(change.slot()))
                return false;
            if (!ItemStack.matches(
                    change.before(), change.inventory().getStackInSlot(change.slot())))
                return false;
        }
        int applied = 0;
        try {
            for (var change : changes) {
                change.inventory().setStackInSlot(change.slot(), change.after());
                applied++;
            }
            return true;
        } catch (RuntimeException failure) {
            for (int i = Math.min(applied, changes.size() - 1); i >= 0; i--) {
                var change = changes.get(i);
                change.inventory().setStackInSlot(change.slot(), change.before());
            }
            return false;
        }
    }
}

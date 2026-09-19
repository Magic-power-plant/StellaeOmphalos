package com.mpp.stellaeomphalos.core.util.item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

public final class InventoryOps {
    public record TransferResult(int moved, ItemStack remainder) {
        public TransferResult { remainder = remainder.copy(); }
        @Override public ItemStack remainder() { return remainder.copy(); }
    }
    private InventoryOps() {}
    public static long count(IItemHandler inventory, Predicate<ItemStack> predicate) {
        long count = 0;
        for (int i = 0; i < inventory.getSlots(); i++) if (predicate.test(inventory.getStackInSlot(i))) count += inventory.getStackInSlot(i).getCount();
        return count;
    }
    public static List<ItemStack> extract(IItemHandler inventory, Predicate<ItemStack> predicate, int maximum, boolean simulate) {
        if (maximum < 0) throw new IllegalArgumentException("Negative item count");
        var result = new ArrayList<ItemStack>(); int remaining = maximum;
        for (int slot = 0; slot < inventory.getSlots() && remaining > 0; slot++) {
            if (!predicate.test(inventory.getStackInSlot(slot))) continue;
            var extracted = inventory.extractItem(slot, remaining, simulate);
            if (!extracted.isEmpty()) { result.add(extracted); remaining -= extracted.getCount(); }
        }
        return List.copyOf(result);
    }
    public static TransferResult transfer(IItemHandler from, int slot, IItemHandler to, int maximum) {
        if (maximum < 0) throw new IllegalArgumentException("Negative transfer");
        var offered = from.extractItem(slot, maximum, true);
        var preview = ItemHandlerHelper.insertItemStacked(to, offered, true);
        int transferable = offered.getCount() - preview.getCount();
        var extracted = from.extractItem(slot, transferable, false);
        var remainder = ItemHandlerHelper.insertItemStacked(to, extracted, false);
        int moved = extracted.getCount() - remainder.getCount();
        if (!remainder.isEmpty()) remainder = from.insertItem(slot, remainder, false);
        return new TransferResult(moved, remainder);
    }
    public static ItemStack withCount(ItemStack stack, int count) {
        if (stack.isEmpty() || count <= 0) return ItemStack.EMPTY;
        var copy = stack.copy(); copy.setCount(count); return copy;
    }
}

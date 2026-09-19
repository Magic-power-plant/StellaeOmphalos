package com.mpp.stellaeomphalos.content.blockentity;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

/** Owns inventory capability lifetime and preserves stacks when a save has a different slot count. */
public abstract class InventoryBlockEntity extends TickingBlockEntity {
    private final ItemStackHandler inventory;
    private final EnumSet<Direction> accessibleFaces;
    private final List<ItemStack> overflow = new ArrayList<>();
    private LazyOptional<IItemHandler> capability;
    protected InventoryBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int slots, EnumSet<Direction> faces) {
        super(type, pos, state);
        if (slots < 1) throw new IllegalArgumentException("Empty inventory");
        accessibleFaces = faces.clone();
        inventory = new ItemStackHandler(slots) {
            @Override public boolean isItemValid(int slot, ItemStack stack) { return acceptsItem(slot, stack); }
            @Override public void setStackInSlot(int slot, ItemStack stack) {
                if (!stack.isEmpty() && !isItemValid(slot, stack)) throw new IllegalArgumentException("Rejected inventory stack");
                super.setStackInSlot(slot, stack);
            }
            @Override protected void onContentsChanged(int slot) { setChanged(); }
        };
        capability = LazyOptional.of(() -> inventory);
    }
    protected abstract boolean acceptsItem(int slot, ItemStack stack);
    protected final ItemStackHandler inventory() { return inventory; }
    protected final List<ItemStack> overflow() { return overflow.stream().map(ItemStack::copy).toList(); }
    protected final void clearOverflow() { overflow.clear(); setChanged(); }
    protected abstract void writeMachineState(CompoundTag tag);
    protected abstract void readMachineState(CompoundTag tag);
    @Override protected final void writePersistent(CompoundTag tag) {
        tag.put("Inventory", inventory.serializeNBT());
        var excess = new ListTag(); overflow.forEach(stack -> excess.add(stack.save(new CompoundTag())));
        tag.put("Overflow", excess); writeMachineState(tag);
    }
    @Override protected final void readPersistent(CompoundTag tag) {
        readMachineState(tag);
        for (int i = 0; i < inventory.getSlots(); i++) inventory.setStackInSlot(i, ItemStack.EMPTY);
        overflow.clear();
        for (Tag saved : tag.getList("Overflow", Tag.TAG_COMPOUND)) {
            var stack = ItemStack.of((CompoundTag) saved); if (!stack.isEmpty()) overflow.add(stack);
        }
        for (Tag saved : tag.getCompound("Inventory").getList("Items", Tag.TAG_COMPOUND)) {
            var itemTag = (CompoundTag) saved; int slot = itemTag.getInt("Slot"); var stack = ItemStack.of(itemTag);
            if (stack.isEmpty()) continue;
            if (slot >= 0 && slot < inventory.getSlots() && inventory.getStackInSlot(slot).isEmpty()
                    && inventory.isItemValid(slot, stack)) inventory.setStackInSlot(slot, stack);
            else overflow.add(stack);
        }
    }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (!isRemoved() && cap == ForgeCapabilities.ITEM_HANDLER && (side == null || accessibleFaces.contains(side))) return capability.cast();
        return super.getCapability(cap, side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); capability.invalidate(); }
    @Override public void reviveCaps() { super.reviveCaps(); capability = LazyOptional.of(() -> inventory); }
}

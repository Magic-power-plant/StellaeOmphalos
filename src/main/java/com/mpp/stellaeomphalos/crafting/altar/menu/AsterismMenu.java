package com.mpp.stellaeomphalos.crafting.altar.menu;

import com.mpp.stellaeomphalos.crafting.altar.recipe.AsterismTier;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.*;

/** Four machine menu types share authority checks and inventory transfer rules. */
public final class AsterismMenu extends AbstractContainerMenu {
    private final MachineAccess machine;
    private final BlockPos pos;
    private final ContainerData data;
    private final int machineSlots, focusSlot;
    private final String kind;
    private final AsterismTier openedTier;
    private long lastAction = -10;
    private int lastKind = -1;

    public AsterismMenu(int id, Inventory inv, MachineAccess machine) {
        this(
                id,
                inv,
                machine.position(),
                machine.machineKind(),
                machine.tier(),
                machine,
                machine.inventory(),
                machine.focusInventory(),
                machine.menuData());
    }

    public AsterismMenu(int id, Inventory inv, FriendlyByteBuf buf, String kind) {
        this(id, inv, buf.readBlockPos(), kind, AsterismTier.parse(buf.readUtf(32)));
    }

    private AsterismMenu(int id, Inventory inv, BlockPos pos, String kind, AsterismTier tier) {
        this(
                id,
                inv,
                pos,
                kind,
                tier,
                null,
                new ItemStackHandler(kind.equals("asterism") ? 25 : 1),
                new ItemStackHandler(1),
                new SimpleContainerData(8));
    }

    private AsterismMenu(
            int id,
            Inventory inv,
            BlockPos pos,
            String kind,
            AsterismTier tier,
            MachineAccess machine,
            IItemHandler inventory,
            IItemHandler focus,
            ContainerData data) {
        super(MachineMenus.type(kind), id);
        this.machine = machine;
        this.pos = pos.immutable();
        this.kind = kind;
        this.openedTier = tier;
        this.data = data;
        int visible = kind.equals("asterism") ? tier.visibleSlotCount() : 1;
        for (int i = 0; i < visible; i++) {
            int index = i;
            int x = AsterismMenuLayout.slotX(i);
            int y = AsterismMenuLayout.slotY(i);
            addSlot(
                    new SlotItemHandler(inventory, i, x, y) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return (machine == null
                                            || index
                                                    < (kind.equals("asterism")
                                                            ? machine.tier().visibleSlotCount()
                                                            : 1))
                                    && super.mayPlace(stack);
                        }
                    });
        }
        focusSlot = kind.equals("asterism") && tier.supports(AsterismTier.SIGN) ? visible : -1;
        if (focusSlot >= 0)
            addSlot(
                    new SlotItemHandler(focus, 0, 184, 40) {
                        @Override
                        public int getMaxStackSize() {
                            return 1;
                        }

                        @Override
                        public boolean mayPlace(ItemStack s) {
                            return validFocus(s);
                        }
                    });
        machineSlots = slots.size();
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(
                        new Slot(
                                inv,
                                9 + row * 9 + col,
                                34 + col * 18,
                                AsterismMenuLayout.PLAYER_Y + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inv, col, 34 + col * 18, AsterismMenuLayout.HOTBAR_Y));
        addDataSlots(data);
    }

    public static boolean validFocus(ItemStack stack) {
        return stack.getCount() <= 1
                && stack.getItem() instanceof SignFocusProvider provider
                && provider.sign(stack).isPresent();
    }

    public ContainerData data() {
        return data;
    }

    public BlockPos position() {
        return pos;
    }

    public String kind() {
        return kind;
    }

    @Override
    public boolean stillValid(Player player) {
        return machine == null
                ? player.level().isClientSide
                : machine.valid(player) && machine.tier() == openedTier;
    }

    @Override
    public boolean clickMenuButton(Player player, int action) {
        if (machine == null || !stillValid(player) || action < 0 || action > 2) return false;
        long now = player.level().getGameTime();
        if (lastKind == action && now - lastAction < 4) return false;
        lastAction = now;
        lastKind = action;
        return machine.action(player, action);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        var stack = slot.getItem();
        var copy = stack.copy();
        if (index < machineSlots) {
            if (!moveItemStackTo(stack, machineSlots, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            boolean moved = false;
            if (focusSlot >= 0
                    && stack.getItem() instanceof SignFocusProvider
                    && validFocus(stack.copyWithCount(1)))
                moved = moveItemStackTo(stack, focusSlot, focusSlot + 1, false);
            if (!moved)
                moved = moveItemStackTo(stack, 0, focusSlot < 0 ? machineSlots : focusSlot, false);
            if (!moved) {
                int split = machineSlots + 27;
                if (index < split) moved = moveItemStackTo(stack, split, slots.size(), false);
                else moved = moveItemStackTo(stack, machineSlots, split, false);
            }
            if (!moved) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return copy;
    }
}

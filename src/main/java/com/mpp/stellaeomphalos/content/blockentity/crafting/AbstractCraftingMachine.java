package com.mpp.stellaeomphalos.content.blockentity.crafting;

import com.mpp.stellaeomphalos.content.blockentity.lumen.LumenSinkBlockEntity;
import com.mpp.stellaeomphalos.crafting.altar.menu.*;
import com.mpp.stellaeomphalos.crafting.altar.recipe.*;
import com.mpp.stellaeomphalos.lumen.fluid.SingleTank;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.*;

import java.util.*;

/** Owns inventory, output custody, capabilities and persistence for every crafting machine. */
public abstract class AbstractCraftingMachine extends LumenSinkBlockEntity
        implements MachineAccess,
                MenuProvider,
                com.mpp.stellaeomphalos.structure.match.StructureDependent {
    protected final ItemStackHandler items;
    protected final ItemStackHandler focus =
            new ItemStackHandler(1) {
                @Override
                public int getSlotLimit(int slot) {
                    return 1;
                }

                @Override
                public boolean isItemValid(int slot, ItemStack stack) {
                    return tier().supports(AsterismTier.SIGN) && AsterismMenu.validFocus(stack);
                }

                @Override
                public void setStackInSlot(int slot, ItemStack stack) {
                    if (!stack.isEmpty() && !isItemValid(slot, stack))
                        throw new IllegalArgumentException("Invalid focus");
                    super.setStackInSlot(slot, stack);
                }

                @Override
                protected void onContentsChanged(int slot) {
                    setChanged();
                }
            };
    protected final SingleTank tank = new SingleTank(16000, this::setChanged);
    protected ItemStack pending = ItemStack.EMPTY;
    protected AbstractCraftTask task;
    private LazyOptional<IItemHandler> itemCapability;
    private LazyOptional<IFluidHandler> fluidCapability;
    private long lastSync;

    protected AbstractCraftingMachine(
            BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) {
        super(type, pos, state, 16000);
        items =
                new ItemStackHandler(slots) {
                    @Override
                    public boolean isItemValid(int slot, ItemStack stack) {
                        return slot
                                < (machineKind().equals("asterism")
                                        ? tier().visibleSlotCount()
                                        : getSlots());
                    }

                    @Override
                    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                        return isItemValid(slot, stack)
                                ? super.insertItem(slot, stack, simulate)
                                : stack;
                    }

                    @Override
                    public int getSlotLimit(int slot) {
                        return machineKind().equals("lumen_infuser")
                                        || machineKind().equals("lumen_well")
                                ? 1
                                : 64;
                    }

                    @Override
                    protected void onContentsChanged(int slot) {
                        setChanged();
                    }
                };
        itemCapability = LazyOptional.of(() -> items);
        fluidCapability = LazyOptional.of(() -> tank);
    }

    @Override
    public final BlockPos position() {
        return worldPosition;
    }

    @Override
    public AsterismTier tier() {
        return getBlockState().hasProperty(CraftingContent.TIER)
                ? getBlockState().getValue(CraftingContent.TIER)
                : AsterismTier.DISCOVERY;
    }

    @Override
    public IItemHandlerModifiable inventory() {
        return items;
    }

    @Override
    public IItemHandlerModifiable focusInventory() {
        return focus;
    }

    public final SingleTank tank() {
        return tank;
    }

    public ItemStack pendingOutput() {
        return pending.copy();
    }

    public Optional<AbstractCraftTask> activeTask() {
        return Optional.ofNullable(task);
    }

    @Override
    public boolean valid(Player player) {
        return level != null
                && player.level() == level
                && !isRemoved()
                && level.hasChunkAt(worldPosition)
                && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(
                                Vec3iCenter.x(worldPosition),
                                Vec3iCenter.y(worldPosition),
                                Vec3iCenter.z(worldPosition))
                        <= 64;
    }

    private com.mpp.stellaeomphalos.structure.match.StructureState frameState =
            com.mpp.stellaeomphalos.structure.match.StructureState.INDETERMINATE;

    public com.mpp.stellaeomphalos.structure.match.StructureState structureState() {
        return frameState;
    }

    public boolean isFormed() {
        return frameState.canProduce();
    }

    public void onStructureStateChanged(
            com.mpp.stellaeomphalos.structure.match.StructureState next,
            com.mpp.stellaeomphalos.structure.match.StructureState previous) {
        frameState = next;
        markClientDirty();
    }

    private java.util.Optional<net.minecraft.resources.ResourceLocation> frameId() {
        String path =
                machineKind().equals("lumen_infuser")
                        ? "pattern_starlight_infuser"
                        : machineKind().equals("asterism") && tier().requiresStructure()
                                ? switch (tier()) {
                                    case RESONANCE -> "pattern_altar_t2";
                                    case SIGN -> "pattern_altar_t3";
                                    default -> "pattern_altar_t4";
                                }
                                : null;
        return java.util.Optional.ofNullable(path)
                .map(p -> new net.minecraft.resources.ResourceLocation("stellaeomphalos", p));
    }

    public final void serverTick() {
        if (!(level instanceof ServerLevel server)) return;
        frameState =
                frameId()
                        .map(
                                id ->
                                        com.mpp.stellaeomphalos.structure.match
                                                .StructureIntegrityHub.of(server)
                                                .query(worldPosition, id))
                        .orElse(com.mpp.stellaeomphalos.structure.match.StructureState.FORMED);
        tickMachine(server);
        if (server.getGameTime() - lastSync >= 10) {
            lastSync = server.getGameTime();
            markClientDirty();
        }
        if (task != null && task.state() == CraftState.IDLE) task = null;
    }

    protected abstract void tickMachine(ServerLevel level);

    protected abstract boolean start(Player player);

    protected void onCollected(Player player) {}

    @Override
    public boolean action(Player player, int action) {
        if (!valid(player) || level.isClientSide) return false;
        return switch (action) {
            case 0 -> pending.isEmpty() && task == null && start(player);
            case 1 -> {
                if (task != null) {
                    task.abort();
                    task = null;
                    setChanged();
                }
                yield true;
            }
            case 2 -> {
                if (pending.isEmpty()) yield false;
                var output = pending;
                pending = ItemStack.EMPTY;
                setChanged();
                if (!player.getInventory().add(output)) player.drop(output, false);
                onCollected(player);
                yield true;
            }
            default -> false;
        };
    }

    protected boolean unlocked(UUID player, net.minecraft.resources.ResourceLocation id) {
        if (!(level instanceof ServerLevel server)) return false;
        var p = server.getServer().getPlayerList().getPlayer(player);
        var advancement = server.getServer().getAdvancements().getAdvancement(id);
        return p != null
                && advancement != null
                && p.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    public List<ItemStack> stacks() {
        var result = new ArrayList<ItemStack>();
        for (int i = 0; i < items.getSlots(); i++) result.add(items.getStackInSlot(i).copy());
        return result;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.stellaeomphalos." + machineKind());
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new AsterismMenu(id, inventory, this);
    }

    @Override
    public ContainerData menuData() {
        return new ContainerData() {
            public int get(int i) {
                return switch (i) {
                    case 0 -> (int) (lumenStored() & 65535);
                    case 1 -> (int) (lumenStored() >>> 16);
                    case 2 -> (int) lumenCapacity();
                    case 3 -> task == null ? 0 : (int) (task.progressFraction() * 1000);
                    case 4 -> task == null ? 0 : task.state().menuCode();
                    case 5 -> tier().menuCode();
                    case 6 -> pending.isEmpty() ? 0 : pending.getCount();
                    case 7 -> tank.getFluidInTank(0).getAmount();
                    default -> 0;
                };
            }

            public void set(int i, int v) {}

            public int getCount() {
                return 8;
            }
        };
    }

    @Override
    protected void writePersistent(CompoundTag tag) {
        super.writePersistent(tag);
        tag.put("Inventory", items.serializeNBT());
        tag.put("Focus", focus.serializeNBT());
        tag.put("PendingOutput", pending.save(new CompoundTag()));
        tag.put("Tank", tank.save());
        if (task != null) tag.put("Craft", task.save());
    }

    @Override
    protected void readPersistent(CompoundTag tag) {
        super.readPersistent(tag);
        if (tag.contains("Inventory")) {
            var inv = tag.getCompound("Inventory").copy();
            inv.putInt("Size", items.getSlots());
            items.deserializeNBT(inv);
        }
        if (tag.contains("Focus")) {
            var data = tag.getCompound("Focus").copy();
            data.putInt("Size", 1);
            focus.deserializeNBT(data);
            if (!focus.getStackInSlot(0).isEmpty()
                    && !AsterismMenu.validFocus(focus.getStackInSlot(0)))
                focus.setStackInSlot(0, ItemStack.EMPTY);
        }
        pending = ItemStack.of(tag.getCompound("PendingOutput"));
        if (tag.contains("Tank")) tank.restore(tag.getCompound("Tank"));
    }

    @Override
    protected void writeClientState(CompoundTag tag) {
        super.writeClientState(tag);
        tag.putString("StructureState", frameState.name());
        tag.putString("Tier", tier().name());
        tag.put("PendingOutput", pending.save(new CompoundTag()));
        tag.put("Tank", tank.save());
        if (task != null) tag.put("Craft", task.save());
    }

    @Override
    protected void readClientState(CompoundTag tag) {
        super.readClientState(tag);
        try {
            frameState =
                    com.mpp.stellaeomphalos.structure.match.StructureState.valueOf(
                            tag.getString("StructureState"));
        } catch (IllegalArgumentException ignored) {
            frameState = com.mpp.stellaeomphalos.structure.match.StructureState.INDETERMINATE;
        }
        pending = ItemStack.of(tag.getCompound("PendingOutput"));
        if (tag.contains("Tank")) tank.restore(tag.getCompound("Tank"));
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (!isRemoved()) {
            if (cap == ForgeCapabilities.ITEM_HANDLER) return itemCapability.cast();
            if (cap == ForgeCapabilities.FLUID_HANDLER
                    && (machineKind().equals("lumen_well")
                            || machineKind().equals("lumen_chalice")))
                return fluidCapability.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemCapability.invalidate();
        fluidCapability.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        itemCapability = LazyOptional.of(() -> items);
        fluidCapability = LazyOptional.of(() -> tank);
    }

    public void dropContents() {
        if (level instanceof ServerLevel server)
            com.mpp.stellaeomphalos.structure.match.StructureIntegrityHub.of(server)
                    .release(worldPosition);
        if (level == null || level.isClientSide) return;
        for (int i = 0; i < items.getSlots(); i++) {
            net.minecraft.world.Containers.dropItemStack(
                    level,
                    worldPosition.getX(),
                    worldPosition.getY(),
                    worldPosition.getZ(),
                    items.getStackInSlot(i));
            items.setStackInSlot(i, ItemStack.EMPTY);
        }
        net.minecraft.world.Containers.dropItemStack(
                level,
                worldPosition.getX(),
                worldPosition.getY(),
                worldPosition.getZ(),
                focus.getStackInSlot(0));
        focus.setStackInSlot(0, ItemStack.EMPTY);
        net.minecraft.world.Containers.dropItemStack(
                level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), pending);
        pending = ItemStack.EMPTY;
        task = null;
    }

    private static final class Vec3iCenter {
        static double x(BlockPos p) {
            return p.getX() + 0.5;
        }

        static double y(BlockPos p) {
            return p.getY() + 0.5;
        }

        static double z(BlockPos p) {
            return p.getZ() + 0.5;
        }
    }
}

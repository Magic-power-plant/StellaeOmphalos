package com.mpp.stellaeomphalos.crafting.special;

import com.mpp.stellaeomphalos.crafting.altar.menu.MachineMenus;
import com.mpp.stellaeomphalos.crafting.altar.recipe.CraftingEnvironment;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;

/** Coordinates live in this menu instance; closing the menu releases the anchor automatically. */
public final class WorkbenchAnchorMenu extends CraftingMenu {
    private final BlockPos anchor;
    private final Inventory inventory;
    private int illumination;

    public WorkbenchAnchorMenu(int id, Inventory inventory, BlockPos anchor) {
        super(id, inventory, ContainerLevelAccess.create(inventory.player.level(), anchor));
        this.anchor = anchor.immutable();
        this.inventory = inventory;
        addDataSlot(
                new DataSlot() {
                    public int get() {
                        return illumination;
                    }

                    public void set(int value) {
                        illumination = value;
                    }
                });
    }

    public BlockPos anchor() {
        return anchor;
    }

    public boolean owns(CraftingContainer input) {
        return slots.get(1).container == input;
    }

    public boolean illuminated(long minimum, int distance) {
        return inventory.player.level().isClientSide
                ? illumination >= minimum
                : CraftingEnvironment.illumination(inventory.player.level(), anchor, distance)
                        >= minimum;
    }

    @Override
    public MenuType<?> getType() {
        return MachineMenus.WORKBENCH.get();
    }

    @Override
    public void slotsChanged(net.minecraft.world.Container container) {
        super.slotsChanged(container);
        if (inventory == null
                || !(inventory.player instanceof net.minecraft.server.level.ServerPlayer player))
            return;
        if (!(slots.get(1).container instanceof CraftingContainer grid)
                || !(slots.get(0).container instanceof ResultContainer result)) return;
        var hub =
                com.mpp.stellaeomphalos.crafting.altar.recipe.CraftingBootstrap.hub(player.server);
        for (var candidate :
                hub.byInput(
                        new net.minecraft.resources.ResourceLocation(
                                "stellaeomphalos", "light_proximity_crafting"),
                        grid.getItems())) {
            var recipe = (LightProximityRecipe) candidate;
            if (recipe.matches(grid, player.level())
                    && result.setRecipeUsed(player.level(), player, recipe)) {
                var output = recipe.assemble(grid, player.level().registryAccess());
                result.setItem(0, output);
                setRemoteSlot(0, output);
                player.connection.send(
                        new net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket(
                                containerId, incrementStateId(), 0, output));
                break;
            }
        }
    }

    @Override
    public void clicked(
            int slot, int button, ClickType type, net.minecraft.world.entity.player.Player player) {
        if (slot == 0 && !player.level().isClientSide) slotsChanged(slots.get(1).container);
        super.clicked(slot, button, type, player);
    }

    @Override
    public net.minecraft.world.item.ItemStack quickMoveStack(
            net.minecraft.world.entity.player.Player player, int slot) {
        if (slot == 0 && !player.level().isClientSide) slotsChanged(slots.get(1).container);
        return super.quickMoveStack(player, slot);
    }

    @Override
    public void broadcastChanges() {
        if (!inventory.player.level().isClientSide) {
            int next =
                    (int)
                            Math.min(
                                    32767,
                                    CraftingEnvironment.illumination(
                                            inventory.player.level(), anchor, 32));
            if (next != illumination) {
                illumination = next;
                slotsChanged(slots.get(1).container);
            }
        }
        super.broadcastChanges();
    }
}

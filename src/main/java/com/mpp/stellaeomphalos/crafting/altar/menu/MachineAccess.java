package com.mpp.stellaeomphalos.crafting.altar.menu;

import com.mpp.stellaeomphalos.crafting.altar.recipe.AsterismTier;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraftforge.items.IItemHandlerModifiable;

/** Menu-facing capabilities; implementation and entity lifecycle belong to the content layer. */
public interface MachineAccess {
    BlockPos position();

    String machineKind();

    AsterismTier tier();

    IItemHandlerModifiable inventory();

    IItemHandlerModifiable focusInventory();

    ContainerData menuData();

    boolean valid(Player player);

    boolean action(Player player, int action);
}

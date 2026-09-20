package com.mpp.stellaeomphalos.content.blockentity.crafting;

import com.mpp.stellaeomphalos.crafting.altar.recipe.*;
import com.mpp.stellaeomphalos.crafting.transmutation.WellLiquefactionRecipe;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.capability.IFluidHandler;

public final class LumenWellBlockEntity extends AbstractCraftingMachine {
    public LumenWellBlockEntity(BlockPos pos, BlockState state) {
        super(CraftingContent.WELL_ENTITY.get(), pos, state, 1);
    }

    @Override
    public String machineKind() {
        return "lumen_well";
    }

    @Override
    protected boolean start(Player player) {
        return true;
    }

    @Override
    protected void tickMachine(ServerLevel server) {
        if (server.getGameTime() % 20 != 0
                || lumenStored() == 0
                || items.getStackInSlot(0).isEmpty()) return;
        var input = new SimpleContainer(items.getStackInSlot(0));
        for (var candidate :
                CraftingBootstrap.hub(server.getServer())
                        .byInput(CraftingBootstrap.id("well_liquefaction"), stacks())) {
            var recipe = (WellLiquefactionRecipe) candidate;
            if (!recipe.matches(input, server)) continue;
            var fluid = recipe.produce(lumenStored());
            int accepted = tank.fill(fluid, IFluidHandler.FluidAction.SIMULATE);
            if (accepted == 0) return;
            fluid.setAmount(accepted);
            tank.fill(fluid, IFluidHandler.FluidAction.EXECUTE);
            if (recipe.shatters(server.random)) items.setStackInSlot(0, ItemStack.EMPTY);
            setChanged();
            break;
        }
    }
}

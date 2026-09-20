package com.mpp.stellaeomphalos.content.blockentity.crafting;

import com.mpp.stellaeomphalos.crafting.transmutation.FluidInteractionRecipe;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.*;

public final class LumenChaliceBlockEntity extends AbstractCraftingMachine {
    public LumenChaliceBlockEntity(BlockPos pos, BlockState state) {
        super(CraftingContent.CHALICE_ENTITY.get(), pos, state, 1);
    }

    @Override
    public String machineKind() {
        return "lumen_chalice";
    }

    @Override
    protected boolean start(Player player) {
        return false;
    }

    @Override
    protected void tickMachine(ServerLevel server) {
        if (server.getGameTime() % 10 != 0 || tank.getFluidInTank(0).isEmpty()) return;
        for (var direction : Direction.Plane.HORIZONTAL) {
            var other = worldPosition.relative(direction);
            if (worldPosition.compareTo(other) >= 0
                    || !server.hasChunkAt(other)
                    || !(server.getBlockEntity(other) instanceof LumenChaliceBlockEntity chalice))
                continue;
            var resultPos = worldPosition.above();
            if (!server.getBlockState(resultPos).isAir()) continue;
            var a = tank.getFluidInTank(0);
            var b = chalice.tank.getFluidInTank(0);
            var matches = new ArrayList<FluidInteractionRecipe>();
            for (var r :
                    com.mpp.stellaeomphalos.crafting.transmutation.WorldRecipeIndex.of(server)
                            .interactions(a.getFluid(), b.getFluid())) {
                var recipe = (FluidInteractionRecipe) r;
                if (recipe.matches(a, b) || recipe.matches(b, a)) matches.add(recipe);
            }
            long total = matches.stream().mapToLong(FluidInteractionRecipe::weight).sum();
            if (total <= 0) continue;
            long ticket = (long) (server.random.nextDouble() * total);
            FluidInteractionRecipe chosen = null;
            for (var recipe : matches) {
                ticket -= recipe.weight();
                if (ticket < 0) {
                    chosen = recipe;
                    break;
                }
            }
            if (chosen == null) continue;
            boolean forward = chosen.matches(a, b);
            var first = forward ? tank : chalice.tank;
            var second = forward ? chalice.tank : tank;
            if (!chosen.first().test(first.getFluidInTank(0))
                    || !chosen.second().test(second.getFluidInTank(0))) continue;
            var drainedFirst =
                    server.random.nextDouble() < chosen.firstChance()
                            ? first.drain(
                                    chosen.first().amount(), IFluidHandler.FluidAction.EXECUTE)
                            : FluidStack.EMPTY;
            var drainedSecond =
                    server.random.nextDouble() < chosen.secondChance()
                            ? second.drain(
                                    chosen.second().amount(), IFluidHandler.FluidAction.EXECUTE)
                            : FluidStack.EMPTY;
            if (!chosen.output().apply(server, resultPos, server.getBlockState(resultPos))) {
                first.fill(drainedFirst, IFluidHandler.FluidAction.EXECUTE);
                second.fill(drainedSecond, IFluidHandler.FluidAction.EXECUTE);
            }
            break;
        }
    }
}

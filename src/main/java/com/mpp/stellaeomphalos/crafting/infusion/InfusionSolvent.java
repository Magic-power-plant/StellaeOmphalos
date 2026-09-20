package com.mpp.stellaeomphalos.crafting.infusion;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.*;

/**
 * Coordinates persist; live handlers are resolved afresh each server tick and never sent to
 * clients.
 */
public final class InfusionSolvent {
    public record Supply(BlockPos position, IFluidHandler handler, FluidStack before) {}

    public record Drain(Supply supply, int amount) {}

    private InfusionSolvent() {}

    public static List<BlockPos> positions(BlockPos origin) {
        var result = new ArrayList<BlockPos>();
        for (int x = -2; x <= 2; x++)
            for (int z = -2; z <= 2; z++)
                if (Math.abs(x) + Math.abs(z) == 3) result.add(origin.offset(x, 0, z));
        result.add(origin.offset(2, 0, 0));
        result.add(origin.offset(-2, 0, 0));
        result.add(origin.offset(0, 0, 2));
        result.add(origin.offset(0, 0, -2));
        return List.copyOf(result);
    }

    public static List<Supply> resolve(
            ServerLevel level, List<BlockPos> positions, LumenInfusionRecipe recipe) {
        var result = new ArrayList<Supply>();
        for (var pos : positions) {
            if (!level.hasChunkAt(pos)) continue;
            var entity = level.getBlockEntity(pos);
            if (entity == null) continue;
            entity.getCapability(ForgeCapabilities.FLUID_HANDLER)
                    .ifPresent(
                            handler -> {
                                for (int tank = 0; tank < handler.getTanks(); tank++) {
                                    var fluid = handler.getFluidInTank(tank);
                                    if (!fluid.isEmpty()
                                            && recipe.solvent().accepts(fluid.getFluid())
                                            && fluid.getAmount() >= recipe.chaliceRequiredAmount()
                                            && handler.drain(
                                                                    new FluidStack(
                                                                            fluid,
                                                                            recipe
                                                                                    .chaliceRequiredAmount()),
                                                                    IFluidHandler.FluidAction
                                                                            .SIMULATE)
                                                            .getAmount()
                                                    == recipe.chaliceRequiredAmount()) {
                                        result.add(new Supply(pos, handler, fluid.copy()));
                                        break;
                                    }
                                }
                            });
        }
        return List.copyOf(result);
    }

    public static List<BlockPos> direct(
            ServerLevel level, BlockPos origin, LumenInfusionRecipe recipe) {
        return positions(origin).stream()
                .filter(level::hasChunkAt)
                .filter(
                        p ->
                                level.getFluidState(p).isSource()
                                        && recipe.solvent()
                                                .accepts(level.getFluidState(p).getType()))
                .toList();
    }

    public static Optional<List<Drain>> plan(
            List<Supply> supplies,
            LumenInfusionRecipe recipe,
            net.minecraft.util.RandomSource random) {
        var requests = new LinkedHashMap<Supply, Integer>();
        int trials = 12;
        for (int i = 0; i < trials; i++)
            if (random.nextDouble() < recipe.chance()) {
                if (supplies.isEmpty()) return Optional.empty();
                var supply = supplies.get(random.nextInt(supplies.size()));
                requests.merge(supply, recipe.solvent().amount(), Integer::sum);
                if (!recipe.multiple()) break;
            }
        var drains = new ArrayList<Drain>();
        for (var e : requests.entrySet()) {
            var expected = new FluidStack(e.getKey().before(), e.getValue());
            var simulated =
                    e.getKey().handler().drain(expected, IFluidHandler.FluidAction.SIMULATE);
            if (!simulated.isFluidEqual(expected) || simulated.getAmount() != e.getValue())
                return Optional.empty();
            drains.add(new Drain(e.getKey(), e.getValue()));
        }
        return Optional.of(List.copyOf(drains));
    }

    public static boolean apply(List<Drain> drains) {
        var applied = new ArrayList<Drain>();
        for (var drain : drains) {
            var expected = new FluidStack(drain.supply().before(), drain.amount());
            var actual =
                    drain.supply().handler().drain(expected, IFluidHandler.FluidAction.EXECUTE);
            if (actual.getAmount() != drain.amount() || !actual.isFluidEqual(expected)) {
                if (!actual.isEmpty())
                    drain.supply().handler().fill(actual, IFluidHandler.FluidAction.EXECUTE);
                for (var rollback : applied)
                    rollback.supply()
                            .handler()
                            .fill(
                                    new FluidStack(rollback.supply().before(), rollback.amount()),
                                    IFluidHandler.FluidAction.EXECUTE);
                return false;
            }
            applied.add(drain);
        }
        return true;
    }

    public static void rollback(List<Drain> drains) {
        for (var drain : drains)
            drain.supply()
                    .handler()
                    .fill(
                            new FluidStack(drain.supply().before(), drain.amount()),
                            IFluidHandler.FluidAction.EXECUTE);
    }
}

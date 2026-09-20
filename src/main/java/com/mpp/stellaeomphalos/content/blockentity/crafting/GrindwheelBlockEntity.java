package com.mpp.stellaeomphalos.content.blockentity.crafting;

import com.mpp.stellaeomphalos.crafting.altar.recipe.*;
import com.mpp.stellaeomphalos.crafting.grinding.*;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public final class GrindwheelBlockEntity extends AbstractCraftingMachine {
    public GrindwheelBlockEntity(BlockPos pos, BlockState state) {
        super(CraftingContent.GRINDWHEEL_ENTITY.get(), pos, state, 1);
    }

    @Override
    public String machineKind() {
        return "grindwheel";
    }

    @Override
    protected void tickMachine(ServerLevel level) {}

    @Override
    protected boolean start(Player player) {
        if (!(level instanceof ServerLevel server)) return false;
        var input = new SimpleContainer(items.getStackInSlot(0).copy());
        var candidates =
                CraftingBootstrap.hub(server.getServer())
                        .byInput(CraftingBootstrap.id("grindwheel"), stacks())
                        .stream()
                        .map(r -> (GrindwheelRecipe) r)
                        .filter(r -> r.matches(input, server))
                        .toList();
        if (candidates.isEmpty()) return false;
        var recipe = candidates.get(server.random.nextInt(candidates.size()));
        server.playSound(
                null,
                worldPosition,
                CraftingContent.GRINDWHEEL_SPIN.get(),
                net.minecraft.sounds.SoundSource.BLOCKS,
                0.6F,
                1F);
        var outcome = recipe.grind(items.getStackInSlot(0).copy(), server.random);
        if (outcome.kind() == GrindOutcome.Kind.FAIL_SILENT) return true;
        if (recipe.alteration()) items.setStackInSlot(0, outcome.output());
        else {
            try {
                var change = AsterismConsumption.simulate(items, 0, recipe.input(), true);
                if (!AsterismConsumption.apply(List.of(change))) return false;
                pending = outcome.output();
            } catch (RuntimeException e) {
                return false;
            }
        }
        setChanged();
        return true;
    }
}

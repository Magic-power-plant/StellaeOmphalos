package com.mpp.stellaeomphalos.content.blockentity.crafting;

import com.mpp.stellaeomphalos.crafting.altar.recipe.*;
import com.mpp.stellaeomphalos.crafting.infusion.*;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public final class LumenInfuserBlockEntity extends AbstractCraftingMachine {
    public LumenInfuserBlockEntity(BlockPos pos, BlockState state) {
        super(CraftingContent.INFUSER_ENTITY.get(), pos, state, 1);
    }

    @Override
    public String machineKind() {
        return "lumen_infuser";
    }

    private SimpleContainer input() {
        return new SimpleContainer(items.getStackInSlot(0).copy());
    }

    @Override
    protected boolean start(Player player) {
        if (!(level instanceof ServerLevel server)
                || !CraftingEnvironment.structure(server, worldPosition, AsterismTier.RESONANCE))
            return false;
        for (var candidate :
                CraftingBootstrap.hub(server.getServer())
                        .byInput(CraftingBootstrap.id("lumen_infusion"), stacks())) {
            var recipe = (LumenInfusionRecipe) candidate;
            if (!recipe.matches(input(), server)
                    || recipe.advancement()
                            .filter(id -> !unlocked(player.getUUID(), id))
                            .isPresent()) continue;
            var supplies =
                    InfusionSolvent.resolve(
                            server, InfusionSolvent.positions(worldPosition), recipe);
            if (supplies.isEmpty()
                    && (!recipe.allowDirect()
                            || InfusionSolvent.direct(server, worldPosition, recipe).isEmpty()))
                continue;
            task =
                    new InfusionTask(
                            recipe.getId(),
                            recipe.contentHash(),
                            duration(recipe, !supplies.isEmpty()),
                            player.getUUID());
            setChanged();
            return true;
        }
        return false;
    }

    private int duration(LumenInfusionRecipe recipe, boolean accelerated) {
        return Math.max(
                1,
                (int)
                        Math.ceil(
                                recipe.displayDuration()
                                        * (accelerated ? recipe.acceleration() : 1)));
    }

    @Override
    protected void tickMachine(ServerLevel server) {
        if (!(task instanceof InfusionTask craft)) return;
        var current = CraftingBootstrap.hub(server.getServer()).byId(craft.recipeId());
        if (current.isEmpty() || !(current.get() instanceof LumenInfusionRecipe recipe)) {
            craft.advance(false, true, true, true, true, true);
            return;
        }
        var supplies =
                InfusionSolvent.resolve(server, InfusionSolvent.positions(worldPosition), recipe);
        craft.solventPositions(supplies.stream().map(InfusionSolvent.Supply::position).toList());
        var direct =
                recipe.allowDirect()
                        ? InfusionSolvent.direct(server, worldPosition, recipe)
                        : List.<BlockPos>of();
        craft.reconcile(recipe.contentHash(), duration(recipe, !supplies.isEmpty()));
        craft.advance(
                true,
                recipe.matches(input(), server),
                CraftingEnvironment.structure(server, worldPosition, AsterismTier.RESONANCE),
                !supplies.isEmpty() || !direct.isEmpty(),
                recipe.advancement().filter(id -> !unlocked(craft.crafter(), id)).isEmpty(),
                true);
        if (craft.state() == CraftState.FINISHED)
            craft.commit(() -> complete(server, recipe, supplies, direct));
        setChanged();
    }

    private boolean complete(
            ServerLevel server,
            LumenInfusionRecipe recipe,
            List<InfusionSolvent.Supply> supplies,
            List<BlockPos> direct) {
        if (!pending.isEmpty() || !recipe.matches(input(), server)) return false;
        AsterismConsumption.SlotChange change;
        net.minecraft.world.item.ItemStack output;
        try {
            change = AsterismConsumption.simulate(items, 0, recipe.input(), recipe.consumes());
            output = recipe.assemble(input(), server.registryAccess());
        } catch (RuntimeException e) {
            return false;
        }
        var drains = List.<InfusionSolvent.Drain>of();
        var blocks = new ArrayList<BlockPos>();
        if (!supplies.isEmpty()) {
            var planned = InfusionSolvent.plan(supplies, recipe, server.random);
            if (planned.isEmpty()) return false;
            drains = planned.get();
            if (!InfusionSolvent.apply(drains)) return false;
        } else {
            var shuffled = new ArrayList<>(direct);
            for (int i = shuffled.size() - 1; i > 0; i--)
                Collections.swap(shuffled, i, server.random.nextInt(i + 1));
            for (var pos : shuffled) {
                if (server.random.nextDouble() < recipe.chance()) {
                    blocks.add(pos);
                    if (!recipe.multiple()) break;
                }
            }
        }
        if (!AsterismConsumption.apply(List.of(change))) {
            InfusionSolvent.rollback(drains);
            return false;
        }
        for (var pos : blocks) server.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        server.playSound(
                null,
                worldPosition,
                CraftingContent.INFUSION_BUBBLE.get(),
                net.minecraft.sounds.SoundSource.BLOCKS,
                0.6F,
                1F);
        pending = output;
        setChanged();
        return true;
    }

    @Override
    protected void readPersistent(CompoundTag tag) {
        super.readPersistent(tag);
        task = InfusionTask.load(tag.getCompound("Craft")).orElse(null);
    }

    @Override
    protected void readClientState(CompoundTag tag) {
        super.readClientState(tag);
        task = InfusionTask.load(tag.getCompound("Craft")).orElse(null);
    }
}

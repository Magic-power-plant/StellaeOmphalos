package com.mpp.stellaeomphalos.content.blockentity.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

/** Single relay slot is consumed only by an altar's final transaction. */
public final class CraftingRelayBlockEntity extends AbstractCraftingMachine {
    public CraftingRelayBlockEntity(BlockPos pos, BlockState state) {
        super(CraftingContent.RELAY_ENTITY.get(), pos, state, 1);
    }

    @Override
    public String machineKind() {
        return "beam_relay";
    }

    @Override
    protected void tickMachine(ServerLevel level) {}

    @Override
    protected boolean start(Player player) {
        return false;
    }
}

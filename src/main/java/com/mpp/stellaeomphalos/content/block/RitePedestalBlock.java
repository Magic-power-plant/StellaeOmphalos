package com.mpp.stellaeomphalos.content.block;

import com.mpp.stellaeomphalos.content.blockentity.rite.RitePedestalBlockEntity;
import com.mpp.stellaeomphalos.structure.match.StructureIntegrityHub;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.util.FakePlayer;

public final class RitePedestalBlock extends Block implements EntityBlock {
    public RitePedestalBlock() {
        super(Properties.of().strength(3, 10).pushReaction(PushReaction.BLOCK));
    }

    public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
        return new RitePedestalBlockEntity(p, s);
    }

    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level l, BlockState s, BlockEntityType<T> type) {
        return l.isClientSide
                ? null
                : (world, p, state, be) -> {
                    if (be instanceof RitePedestalBlockEntity pedestal) pedestal.serverTick();
                };
    }

    public void setPlacedBy(
            Level l, BlockPos p, BlockState s, LivingEntity actor, ItemStack stack) {
        if (actor instanceof ServerPlayer player
                && !(player instanceof FakePlayer)
                && l.getBlockEntity(p) instanceof RitePedestalBlockEntity be)
            be.setOwner(player.getUUID());
    }

    public InteractionResult use(
            BlockState s,
            Level l,
            BlockPos p,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (player instanceof ServerPlayer server
                && l.getBlockEntity(p) instanceof RitePedestalBlockEntity be)
            be.interact(server, hand, player.isShiftKeyDown()
                    ? (player.getItemInHand(hand).is(net.minecraft.world.item.Items.STICK) ? 2 : 1) : 0);
        return InteractionResult.sidedSuccess(l.isClientSide);
    }

    public void neighborChanged(
            BlockState s, Level l, BlockPos p, Block b, BlockPos other, boolean moving) {
        if (l.getBlockEntity(p) instanceof RitePedestalBlockEntity be) be.environmentChanged();
    }

    public void onRemove(BlockState old, Level l, BlockPos p, BlockState next, boolean moving) {
        if (old.getBlock() != next.getBlock() && l instanceof ServerLevel server) {
            StructureIntegrityHub.of(server).release(p);
            if (l.getBlockEntity(p) instanceof RitePedestalBlockEntity be) be.dropOwnedContents();
        }
        super.onRemove(old, l, p, next, moving);
    }
}

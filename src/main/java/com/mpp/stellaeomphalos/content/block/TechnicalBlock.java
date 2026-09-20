package com.mpp.stellaeomphalos.content.block;

import com.mpp.stellaeomphalos.content.blockentity.rite.TechnicalBlockEntity;

import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

public final class TechnicalBlock extends Block implements EntityBlock {
    private final String kind;

    public TechnicalBlock(String kind) {
        super(
                Properties.of()
                        .strength(kind.equals("ward_block") ? -1 : 3, 3600000)
                        .noOcclusion()
                        .pushReaction(PushReaction.BLOCK)
                        .lightLevel(
                                s ->
                                        kind.equals("flare_light") || kind.equals("world_lamp")
                                                ? 15
                                                : 0));
        this.kind = kind;
    }

    @Override
    public BlockState getStateForPlacement(
            net.minecraft.world.item.context.BlockPlaceContext context) {
        if (kind.startsWith("bore_head_")
                && !context.getLevel()
                        .getBlockState(context.getClickedPos().above())
                        .is(
                                com.mpp.stellaeomphalos.content.world.WorldContent.BLOCKS
                                        .get("bore_core")
                                        .get())) return null;
        return defaultBlockState();
    }

    @Override
    public SoundType getSoundType(
            BlockState state,
            LevelReader level,
            BlockPos pos,
            net.minecraft.world.entity.Entity entity) {
        if ((kind.equals("frame_shell") || kind.equals("mimic_block"))
                && level.getBlockEntity(pos) instanceof TechnicalBlockEntity be)
            return be.hostState().getSoundType(level, pos, entity);
        return super.getSoundType(state, level, pos, entity);
    }

    @Override
    public float getDestroyProgress(
            BlockState state, Player player, BlockGetter world, BlockPos pos) {
        if (kind.equals("frame_shell")
                && world.getBlockEntity(pos) instanceof TechnicalBlockEntity be)
            return be.hostState().getDestroyProgress(player, world, pos);
        return super.getDestroyProgress(state, player, world, pos);
    }

    public String kind() {
        return kind;
    }

    public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
        return java.util.Set.of(
                                "frame_shell",
                                "mimic_block",
                                "gate_core",
                                "world_lamp",
                                "rite_link",
                                "observatory",
                                "bore_core",
                                "mineral_regenerator")
                        .contains(kind)
                ? new TechnicalBlockEntity(p, s)
                : null;
    }

    public RenderShape getRenderShape(BlockState s) {
        return java.util.Set.of("frame_shell", "ward_block", "flare_light", "gate_node")
                        .contains(kind)
                ? RenderShape.INVISIBLE
                : RenderShape.MODEL;
    }

    public VoxelShape getCollisionShape(
            BlockState s, BlockGetter level, BlockPos p, CollisionContext ctx) {
        if (kind.equals("gate_node") || kind.equals("flare_light")) return Shapes.empty();
        if (kind.equals("frame_shell")
                && level.getBlockEntity(p) instanceof TechnicalBlockEntity be)
            return be.hostState().getCollisionShape(level, p, ctx);
        return super.getCollisionShape(s, level, p, ctx);
    }

    public ItemStack getCloneItemStack(BlockGetter level, BlockPos p, BlockState s) {
        if ((kind.equals("frame_shell") || kind.equals("mimic_block"))
                && level.getBlockEntity(p) instanceof TechnicalBlockEntity be)
            return new ItemStack(be.hostState().getBlock());
        return super.getCloneItemStack(level, p, s);
    }

    public InteractionResult use(
            BlockState s,
            Level level,
            BlockPos p,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (level.getBlockEntity(p) instanceof TechnicalBlockEntity be)
            return be.interact(player, hand, hit);
        return InteractionResult.PASS;
    }

    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level l, BlockState s, BlockEntityType<T> type) {
        return l.isClientSide
                ? null
                : (world, p, state, be) -> {
                    if (be instanceof TechnicalBlockEntity technical) technical.serverTick();
                };
    }
}

package com.mpp.stellaeomphalos.content.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Part-6 §6.2.1.4 光斑（`glow_mote`）与易逝光源（`ephemeral_light`）。
 *
 * <p>两者都是 {@code EnumProperty<DyeColor>} 的 16 色不可见光源：全亮度、无碰撞、不可选中、可被替换、
 * 不被活塞推动、零掉落。区别是易逝光源由照明器/辉光粉生成并自行消失，光斑由照明杖放置并需要玩家主动移除。
 */
public class LightMoteBlock extends Block {

    public static final EnumProperty<DyeColor> COLOR =
            EnumProperty.create("color", DyeColor.class);

    private final boolean ephemeral;

    public LightMoteBlock(boolean ephemeral) {
        super(
                Properties.of()
                        .noCollission()
                        .noOcclusion()
                        .instabreak()
                        .lightLevel(s -> 15)
                        .pushReaction(PushReaction.BLOCK)
                        .noLootTable()
                        .replaceable()
                        .randomTicks()
                        .strength(-1.0F, 3600000.0F));
        this.ephemeral = ephemeral;
        registerDefaultState(stateDefinition.any().setValue(COLOR, DyeColor.WHITE));
    }

    public boolean ephemeral() {
        return ephemeral;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(COLOR);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public boolean canBeReplaced(BlockState state, net.minecraft.world.item.context.BlockPlaceContext context) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // 易逝光源自行消失；光斑需要玩家主动移除。
        if (ephemeral) level.removeBlock(pos, false);
    }

    @Override
    public void onPlace(
            BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
        super.onPlace(state, level, pos, oldState, moving);
        if (ephemeral && !level.isClientSide) level.scheduleTick(pos, this, 600);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (ephemeral) level.removeBlock(pos, false);
    }
}

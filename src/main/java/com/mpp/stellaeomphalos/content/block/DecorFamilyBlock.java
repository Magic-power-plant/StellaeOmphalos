package com.mpp.stellaeomphalos.content.block;

import com.mpp.stellaeomphalos.content.block.DecorFamily.DecorVariant;
import com.mpp.stellaeomphalos.content.item.DecorBlockItem;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 装饰建材方块（Part-6 §6.2.1.1 / §6.3.1 D-1 / §6.3.2）。
 *
 * <p>一个族 = 一个 Block + {@link DecorVariant} 枚举属性；柱体变体用 {@code top}/{@code bottom} 持久化连接状态，
 * 替代 1.12.2 的 {@code getActualState}。连接状态在放置、邻居变化与移除三处刷新，缺一会留下脏状态。
 */
public class DecorFamilyBlock extends Block implements SimpleWaterloggedBlock {

    public static final EnumProperty<DecorVariant> VARIANT =
            EnumProperty.create("variant", DecorVariant.class);
    public static final BooleanProperty TOP = BooleanProperty.create("top");
    public static final BooleanProperty BOTTOM = BooleanProperty.create("bottom");

    private static final VoxelShape PILLAR_SINGLE = Block.box(4, 0, 4, 12, 16, 12);
    private static final VoxelShape PILLAR_MIDDLE = Shapes.block();
    private static final VoxelShape PILLAR_TOP = Block.box(4, 8, 4, 12, 16, 12);
    private static final VoxelShape PILLAR_BOTTOM = Block.box(4, 0, 4, 12, 8, 12);

    private final DecorFamily family;

    public DecorFamilyBlock(DecorFamily family, Properties properties) {
        super(properties);
        this.family = family;
        registerDefaultState(
                stateDefinition
                        .any()
                        .setValue(VARIANT, DecorVariant.RAW)
                        .setValue(TOP, false)
                        .setValue(BOTTOM, false)
                        .setValue(BlockStateProperties.WATERLOGGED, false));
    }

    public DecorFamily family() {
        return family;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT, TOP, BOTTOM, BlockStateProperties.WATERLOGGED);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        var state =
                defaultBlockState()
                        .setValue(VARIANT, variantOf(context))
                        .setValue(
                                BlockStateProperties.WATERLOGGED,
                                context.getLevel()
                                        .getFluidState(context.getClickedPos())
                                        .getType()
                                        .isSame(Fluids.WATER));
        return state.getValue(VARIANT) == DecorVariant.PILLAR
                ? connect(state, context.getLevel(), context.getClickedPos())
                : state;
    }

    /** 放置用变体由物品决定；无匹配时退回 RAW。 */
    private DecorVariant variantOf(BlockPlaceContext context) {
        var stack = context.getItemInHand();
        if (stack.getItem() instanceof DecorBlockItem item && item.family() == family)
            return item.variant();
        return DecorVariant.RAW;
    }

    /** 依据上下邻居刷新柱体连接状态（§6.3.2 触发点：放置/邻居变化/移除）。 */
    public static BlockState connect(BlockState state, BlockGetter level, BlockPos pos) {
        if (state.getValue(VARIANT) != DecorVariant.PILLAR) return state;
        var block = state.getBlock();
        boolean up = level.getBlockState(pos.above()).is(block);
        boolean down = level.getBlockState(pos.below()).is(block);
        return state.setValue(TOP, up).setValue(BOTTOM, down);
    }

    @Override
    public BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighbor,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos) {
        if (direction.getAxis() == Direction.Axis.Y)
            return connect(state, level, pos);
        if (state.getValue(BlockStateProperties.WATERLOGGED))
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return state;
    }

    @Override
    public void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block neighborBlock,
            BlockPos neighborPos,
            boolean moving) {
        if (level.isClientSide || state.getValue(VARIANT) != DecorVariant.PILLAR) return;
        var next = connect(state, level, pos);
        if (next != state) level.setBlock(pos, next, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }

    @Override
    public void onRemove(
            BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        super.onRemove(state, level, pos, next, moving);
        if (level.isClientSide || state.getBlock() != next.getBlock()) return;
        for (var offset : new BlockPos[] {pos.above(), pos.below()}) {
            var neighbor = level.getBlockState(offset);
            if (neighbor.getBlock() != this || neighbor.getValue(VARIANT) != DecorVariant.PILLAR)
                continue;
            var refreshed = connect(neighbor, level, offset);
            if (refreshed != neighbor) level.setBlock(offset, refreshed, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(VARIANT) != DecorVariant.PILLAR) return Shapes.block();
        if (state.getValue(TOP)) return state.getValue(BOTTOM) ? PILLAR_MIDDLE : PILLAR_TOP;
        return state.getValue(BOTTOM) ? PILLAR_BOTTOM : PILLAR_SINGLE;
    }

    @Override
    public VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(BlockStateProperties.WATERLOGGED)
                ? Fluids.WATER.getSource(false)
                : super.getFluidState(state);
    }

    @Override
    public boolean propagatesSkylightDown(
            BlockState state, BlockGetter level, BlockPos pos) {
        return !state.getValue(BlockStateProperties.WATERLOGGED);
    }
}

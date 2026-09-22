package com.mpp.stellaeomphalos.content.block;

import com.mpp.stellaeomphalos.content.world.WorldContent;

import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 星泉泉头（Part-6 §6.2.1.2，决策 D-4）。
 *
 * <p>1.12.2 用 meta 表达的两个变体在 1.20.1 由 {@link EnumProperty}&lt;{@link BoreMode}&gt; 承载：
 * `LIQUID` 模式向下开挖倒锥井并按周期抽取区块流体，`VORTEX` 模式清空 7×7×7 并把非玩家生物拉入漩涡
 * 并施加时间冻结（§6.6.1）。泉头**只能**放在 `fountain` 主机正下方，放置点在下方时拒绝。
 *
 * <p>另保留一个实现扩展属性 {@link #TIER}：钻头档位决定 `fountain` 作业时使用的工具与磨损速度。
 * 这是 §6.2 总账之外的实现细节（总账只要求 `LIQUID` / `VORTEX` 两个变体），已在该册「订正」表登记。
 */
public final class BoreHeadBlock extends Block {

    /** 泉头模式（D-4 的两个变体）。 */
    public static final EnumProperty<BoreMode> MODE =
            EnumProperty.create("mode", BoreMode.class);

    /** 钻头档位（实现扩展，不在总账内）。 */
    public static final EnumProperty<Tier> TIER =
            EnumProperty.create("tier", Tier.class);

    /** 泉头碰撞箱：14/16 见方、2/16 高。 */
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 2, 15);

    public BoreHeadBlock() {
        super(Properties.of().strength(3.0F, 6.0F).noOcclusion());
        registerDefaultState(
                stateDefinition
                        .any()
                        .setValue(MODE, BoreMode.LIQUID)
                        .setValue(TIER, Tier.STONE));
    }

    /** 两模式。 */
    public enum BoreMode implements StringRepresentable {
        /** 液体模式：开挖倒锥井 + 抽取流体。 */
        LIQUID,
        /** 漩涡模式：清空 7×7×7 并把生物拉入时间冻结。 */
        VORTEX;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** 钻头档位：决定可挖方块等级与磨损速度。 */
    public enum Tier implements StringRepresentable {
        STONE,
        IRON,
        DIAMOND;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MODE, TIER);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // §6.6.1：只能在下方点击且上方为 `fountain` 时允许放置。
        if (!context.getClickedFace().equals(net.minecraft.core.Direction.DOWN)) return null;
        var above = WorldContent.BLOCKS.get("fountain");
        if (above == null) return null;
        if (!context.getLevel().getBlockState(context.getClickedPos().above()).is(above.get()))
            return null;
        return defaultBlockState();
    }

    @Override
    public VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** 由放置物品写入模式与档位。 */
    public static BlockState configure(BlockState state, BoreMode mode, Tier tier) {
        return state.setValue(MODE, mode).setValue(TIER, tier);
    }

    /** 该档位对应的原版工具（供 `fountain` 作业与磨损使用）。 */
    public static net.minecraft.world.item.Item toolFor(Tier tier) {
        return switch (tier) {
            case IRON -> net.minecraft.world.item.Items.IRON_PICKAXE;
            case DIAMOND -> net.minecraft.world.item.Items.DIAMOND_PICKAXE;
            case STONE -> net.minecraft.world.item.Items.STONE_PICKAXE;
        };
    }
}

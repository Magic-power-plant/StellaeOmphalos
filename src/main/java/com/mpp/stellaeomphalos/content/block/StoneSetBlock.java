package com.mpp.stellaeomphalos.content.block;

import net.minecraft.core.*;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public final class StoneSetBlock extends Block {
    public enum PillarPart implements StringRepresentable {
        NONE,
        BOTTOM,
        MIDDLE,
        TOP;

        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static final EnumProperty<PillarPart> PART =
            EnumProperty.create("pillar_part", PillarPart.class);
    private final boolean pillar;

    public StoneSetBlock(boolean pillar) {
        super(Properties.of().strength(2, 6));
        this.pillar = pillar;
        registerDefaultState(stateDefinition.any().setValue(PART, PillarPart.NONE));
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(PART);
    }

    private BlockState connect(BlockState state, LevelReader world, BlockPos p) {
        if (!pillar) return state;
        boolean up = world.getBlockState(p.above()).is(this),
                down = world.getBlockState(p.below()).is(this);
        return state.setValue(
                PART,
                up
                        ? (down ? PillarPart.MIDDLE : PillarPart.BOTTOM)
                        : (down ? PillarPart.TOP : PillarPart.NONE));
    }

    public BlockState getStateForPlacement(BlockPlaceContext c) {
        return connect(defaultBlockState(), c.getLevel(), c.getClickedPos());
    }

    public BlockState updateShape(
            BlockState s,
            Direction d,
            BlockState neighbor,
            LevelAccessor level,
            BlockPos p,
            BlockPos other) {
        return d.getAxis() == Direction.Axis.Y ? connect(s, level, p) : s;
    }
}

package com.mpp.stellaeomphalos.content.block;

import com.mpp.stellaeomphalos.content.world.WorldContent;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.*;

public final class CrystalClusterBlock extends Block {
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 4),
            ASPECT = IntegerProperty.create("aspect", 0, 2);
    private final boolean gem;

    public CrystalClusterBlock(boolean gem) {
        super(
                Properties.of()
                        .strength(1.5F)
                        .noOcclusion()
                        .randomTicks()
                        .lightLevel(s -> s.getValue(STAGE) + 3));
        this.gem = gem;
        registerDefaultState(stateDefinition.any().setValue(STAGE, 0).setValue(ASPECT, 0));
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(STAGE, ASPECT);
    }

    public VoxelShape getShape(BlockState s, BlockGetter world, BlockPos p, CollisionContext c) {
        int stage = s.getValue(STAGE);
        return Block.box(5 - stage, 0, 5 - stage, 11 + stage, 4 + stage * 3, 11 + stage);
    }

    public boolean canSurvive(BlockState s, LevelReader level, BlockPos p) {
        return level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP);
    }

    public void randomTick(BlockState s, ServerLevel level, BlockPos p, RandomSource random) {
        int stage = s.getValue(STAGE);
        if (stage >= 4) return;
        boolean fast = level.getBlockState(p.below()).is(WorldContent.STAR_METAL_ORE.get());
        if (random.nextInt(fast ? 2 : 6) != 0) return;
        var next = s.setValue(STAGE, stage + 1);
        if (gem) next = next.setValue(ASPECT, level.canSeeSky(p) ? (level.isNight() ? 2 : 1) : 0);
        level.setBlock(p, next, 3);
    }
}

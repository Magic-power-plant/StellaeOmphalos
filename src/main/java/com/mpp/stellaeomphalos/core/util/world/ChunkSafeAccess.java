package com.mpp.stellaeomphalos.core.util.world;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class ChunkSafeAccess {
    private ChunkSafeAccess() {}
    public static Optional<BlockState> state(Level level, BlockPos pos) {
        return level.hasChunkAt(pos) && !level.isOutsideBuildHeight(pos) ? Optional.of(level.getBlockState(pos)) : Optional.empty();
    }
    public static <T extends BlockEntity> Optional<T> entity(Level level, BlockPos pos, Class<T> type) {
        if (!level.hasChunkAt(pos)) return Optional.empty();
        var entity = level.getBlockEntity(pos);
        return type.isInstance(entity) ? Optional.of(type.cast(entity)) : Optional.empty();
    }
}

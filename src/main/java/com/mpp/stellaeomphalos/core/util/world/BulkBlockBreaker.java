package com.mpp.stellaeomphalos.core.util.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class BulkBlockBreaker {
    private BulkBlockBreaker() {}
    public static boolean breakOne(ServerLevel level, BlockPos pos, ServerPlayer actor) {
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Block break off server thread");
        if (actor.serverLevel() != level || !level.hasChunkAt(pos) || !level.mayInteract(actor, pos)) return false;
        var state = level.getBlockState(pos);
        if (state.isAir() || state.getDestroySpeed(level, pos) < 0) return false;
        // ServerPlayerGameMode already invokes Forge protection events, harvest rules and tool wear.
        return actor.gameMode.destroyBlock(pos);
    }
}

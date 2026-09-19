package com.mpp.stellaeomphalos.core.platform.api;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/** Version 1 capability for optional domain debug data; invoked on the server thread after authorization. */
@FunctionalInterface
public interface NetworkDiagnosticSource {
    /** Returns a copied report, or empty when this source has no data at the already-loaded position. */
    Optional<CompoundTag> inspect(ServerPlayer player, BlockPos position);
}

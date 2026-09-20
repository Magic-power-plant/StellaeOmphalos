package com.mpp.stellaeomphalos.constellation.domain;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * One invocation context: the level, the pedestal origin, the owning player (nullable — ownerless
 * ritual sites are legal) and the back-mirror count of the driving structure.
 */
public record DomainContext(ServerLevel level, BlockPos origin, @Nullable UUID owningPlayer, int mirrorCount) { }

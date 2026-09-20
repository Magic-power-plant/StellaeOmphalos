package com.mpp.stellaeomphalos.constellation.domain;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** A resolved effect origin; relay link blocks redirect it via {@code DomainEffect.resolveOrigin}. */
public record DomainOrigin(ServerLevel level, BlockPos pos) { }

package com.mpp.stellaeomphalos.ritual.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

import java.util.UUID;

/** Capabilities presented to an effect, independent of its owning machine. */
public interface RiteEffectContext {
    ServerLevel level();

    BlockPos origin();

    ResourceLocation riteId();

    ResourceLocation sign();

    float intensity();

    double radius();

    UUID owner();

    int positionBudget();

    int amplifierCount();

    AffectedRegion requestRegion();

    RandomSource random();
}

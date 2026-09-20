package com.mpp.stellaeomphalos.player.mantle;

/** Effect capability: simulation has no random, NBT, entity, cooldown or world side effects. */
public interface MantleActions {
    DamageIntercept perform(MantleAction action, boolean simulate);

    MantleState snapshot();
}

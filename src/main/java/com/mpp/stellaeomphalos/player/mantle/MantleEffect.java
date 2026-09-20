package com.mpp.stellaeomphalos.player.mantle;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/** Main-thread session owns its private state; commit is unreachable during simulation. */
public abstract class MantleEffect implements MantleActions {
    private final ResourceLocation id;
    protected final CompoundTag state;

    protected MantleEffect(MantleState initial) {
        id = initial.effect();
        state = initial.data();
    }

    public final DamageIntercept perform(MantleAction action, boolean simulate) {
        var result = preview(action);
        if (!simulate) commit(action, result);
        return result;
    }

    protected abstract DamageIntercept preview(MantleAction action);

    protected abstract void commit(MantleAction action, DamageIntercept result);

    @Override
    public final MantleState snapshot() {
        return new MantleState(id, state);
    }
}

package com.mpp.stellaeomphalos.lumen.capability;

import net.minecraft.world.level.Level;

/** A light-receiving node. */
public interface LumenSink extends LumenNode {
    /**
     * @param amount   LU offered
     * @param simulate when true only the acceptance is computed, state stays untouched
     * @return actually accepted LU (0..amount)
     */
    long acceptLumen(Level level, long amount, boolean simulate);
    long lumenCapacity();
    long lumenStored();
}

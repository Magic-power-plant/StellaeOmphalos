package com.mpp.stellaeomphalos.lumen.transport;

import java.util.function.Supplier;

/** Version-checked value cache: any topology epoch bump invalidates the stored value. Pure, JUnit-safe. */
public final class EpochCache<V> {
    private long epoch = Long.MIN_VALUE;
    private V value;
    /** Returns the cached value for this epoch; recomputes when the epoch moved. A null recompute is never cached. */
    public V get(long epoch, Supplier<V> recompute) {
        if (this.epoch != epoch) {
            V recomputed = recompute.get();
            if (recomputed == null) return null;
            value = recomputed;
            this.epoch = epoch;
        }
        return value;
    }
    public void invalidate() { epoch = Long.MIN_VALUE; value = null; }
}

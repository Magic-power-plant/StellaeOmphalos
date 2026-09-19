package com.mpp.stellaeomphalos.core.util.net;

import com.mpp.stellaeomphalos.core.util.world.DimensionPos;
import java.util.HashSet;
import java.util.Set;

/** Explicit instance ownership prevents cross-world and cross-side particle suppression. */
public final class FrameGate {
    private final Set<DimensionPos> positions = new HashSet<>();
    private long tick = Long.MIN_VALUE;
    public boolean acquire(DimensionPos position, long now) {
        if (now != tick) { positions.clear(); tick = now; }
        return positions.add(position);
    }
    public void clear() { positions.clear(); tick = Long.MIN_VALUE; }
}

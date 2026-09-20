package com.mpp.stellaeomphalos.lumen.capability;

import com.mpp.stellaeomphalos.lumen.transport.LumenSourceData;
import net.minecraft.world.level.Level;

/** A light-producing node; per-tick output is measured in LU. */
public interface LumenSource extends LumenNode {
    /** @return LU available this tick; must be >= 0. */
    long provideLumen(Level level, long gameTick);
    /** Lightweight world-independent source data (serializable, no Level reference). */
    LumenSourceData data();
}

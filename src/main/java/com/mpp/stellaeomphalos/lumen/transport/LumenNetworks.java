package com.mpp.stellaeomphalos.lumen.transport;

import com.mpp.stellaeomphalos.OmphalosConfig;
import com.mpp.stellaeomphalos.lumen.capability.LumenDelivery;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Facade: the lumen network of a dimension. */
public final class LumenNetworks {
    private LumenNetworks() {}

    public static LumenNetwork of(ServerLevel level) { return LumenTopology.network(level); }

    /** Convenience: resolve from a source position using the configured per-tick routing budget. */
    public static List<LumenDelivery> resolve(ServerLevel level, BlockPos from) {
        return of(level).resolve(from, OmphalosConfig.COMMON.integer("performance.lumenRoutingStepsPerTick"));
    }
}

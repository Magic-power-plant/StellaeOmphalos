package com.mpp.stellaeomphalos.lumen.capability;

import net.minecraft.core.BlockPos;

/** One resolved sink delivery: effective LU after per-hop loss, and the hop count taken. */
public record LumenDelivery(BlockPos sink, long amount, int hops) {
    public LumenDelivery {
        if (amount < 0 || hops < 0) throw new IllegalArgumentException("Negative delivery component");
        sink = sink.immutable();
    }
}

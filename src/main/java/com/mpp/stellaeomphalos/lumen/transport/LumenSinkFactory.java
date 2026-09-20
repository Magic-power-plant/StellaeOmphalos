package com.mpp.stellaeomphalos.lumen.transport;

import com.mpp.stellaeomphalos.lumen.capability.LumenSink;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Creates a lumen sink view over a non-block carrier (entity, item, ...). */
@FunctionalInterface
public interface LumenSinkFactory {
    LumenSink create(Level level, BlockPos pos, Object carrier);
}

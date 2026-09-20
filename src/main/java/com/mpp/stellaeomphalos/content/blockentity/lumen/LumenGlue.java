package com.mpp.stellaeomphalos.content.blockentity.lumen;

import com.mpp.stellaeomphalos.lumen.capability.LumenHandler;
import com.mpp.stellaeomphalos.lumen.capability.LumenIO;
import com.mpp.stellaeomphalos.lumen.capability.LumenNode;
import com.mpp.stellaeomphalos.lumen.transport.LumenTopology;
import java.util.function.LongSupplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.LazyOptional;

/** Shared capability/topology glue for the lumen block entity family (composition over inheritance). */
final class LumenGlue {
    private final LumenNode node;
    private final LumenHandler handler;
    private LazyOptional<LumenHandler> capability;
    private boolean rebuild = true;

    LumenGlue(LumenNode node, LongSupplier stored, LongSupplier capacity) {
        this.node = node;
        handler = new LumenHandler() {
            @Override public LumenNode node() { return node; }
            @Override public LumenIO io() { return node.io(); }
            @Override public long stored() { return stored.getAsLong(); }
            @Override public long capacity() { return capacity.getAsLong(); }
        };
        capability = LazyOptional.of(() -> handler);
    }

    void join(BlockEntity owner) {
        if (owner.getLevel() instanceof ServerLevel level) LumenTopology.enqueueJoin(level, node.pos());
    }
    void leave(BlockEntity owner) {
        if (owner.getLevel() instanceof ServerLevel level) LumenTopology.enqueueLeave(level, node.pos());
    }
    boolean neighborChanged(Level level) {
        if (level instanceof ServerLevel server) LumenTopology.enqueueRescan(server, node.pos());
        rebuild = true;
        return true;
    }
    boolean needsRebuild() { return rebuild; }
    void markDirty() { rebuild = true; }
    void markClean() { rebuild = false; }
    LazyOptional<LumenHandler> capability() { return capability; }
    void invalidate() { capability.invalidate(); }
    void revive() { capability = LazyOptional.of(() -> handler); }
}

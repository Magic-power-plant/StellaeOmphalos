package com.mpp.stellaeomphalos.structure.match;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/** Owns observer identity and terminal lifecycle; matching remains a separate capability. */
public abstract class AbstractStructureWatch implements AutoCloseable {
    protected final BlockPos origin;
    protected final ResourceLocation blueprintId;
    private boolean closed;

    protected AbstractStructureWatch(BlockPos origin, ResourceLocation id) {
        this.origin = origin.immutable();
        blueprintId = id;
    }

    public final BlockPos origin() {
        return origin;
    }

    public final ResourceLocation blueprintId() {
        return blueprintId;
    }

    public final boolean closed() {
        return closed;
    }

    public abstract StructureState state();

    protected abstract void detach();

    public final void close() {
        if (!closed) {
            closed = true;
            detach();
        }
    }
}

package com.mpp.stellaeomphalos.constellation.domain;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

/**
 * One persistent slot of a {@link DomainPositionCache}. Identity is the position; subclasses carry
 * extra payload. NBT keys are PascalCase ("Pos" plus subclass keys).
 */
public abstract class DomainPositionEntry {
    private final BlockPos pos;

    protected DomainPositionEntry(BlockPos pos) { this.pos = pos.immutable(); }

    public BlockPos pos() { return pos; }

    /** Appends payload after the position; subclasses must call super first. */
    public void write(CompoundTag tag) { tag.putLong("Pos", pos.asLong()); }

    /** Reads subclass payload; the position itself is supplied by the cache factory. */
    public void read(CompoundTag tag) { }
}

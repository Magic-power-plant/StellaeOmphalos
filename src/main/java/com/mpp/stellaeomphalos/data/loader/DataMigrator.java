package com.mpp.stellaeomphalos.data.loader;

import net.minecraft.nbt.CompoundTag;

/** A deterministic single-version transformation of a copied payload. */
public interface DataMigrator {
    int fromVersion();
    default int toVersion() { return fromVersion() + 1; }
    CompoundTag migrate(CompoundTag payload);
}

package com.mpp.stellaeomphalos.data.loader;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Lifecycle base for server-owned dimension state. Subclasses only own domain fields; loading,
 * schema handling and the read-only failure mode stay in one place.
 */
public abstract class DimensionArchive extends SavedData {
    private final ArchiveKey key;
    private boolean readOnly;
    private boolean loaded;

    protected DimensionArchive(ArchiveKey key) { this.key = key; }

    public final ArchiveKey key() { return key; }
    public final boolean readOnly() { return readOnly; }
    public final boolean loaded() { return loaded; }

    public final void restore(CompoundTag tag) {
        int schema = tag.contains("schema") ? tag.getInt("schema") : 1;
        if (schema > key.schemaVersion()) {
            readOnly = true;
            return;
        }
        try {
            readSchema(schema, tag);
        } catch (RuntimeException invalid) {
            readOnly = true;
        }
    }

    protected void readSchema(int schema, CompoundTag tag) { readData(tag); }
    protected abstract void readData(CompoundTag tag);
    protected abstract void writeData(CompoundTag tag);

    public void onLoaded(ServerLevel level) { loaded = true; }
    public void tick(ServerLevel level) {}

    protected final void markArchiveDirty() {
        if (!readOnly) setDirty();
    }

    @Override
    public final CompoundTag save(CompoundTag tag) {
        tag.putInt("schema", key.schemaVersion());
        if (!readOnly) writeData(tag);
        return tag;
    }
}

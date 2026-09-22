package com.mpp.stellaeomphalos.data.loader;

import com.mojang.serialization.Codec;
import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.saveddata.SavedData;

/** Owns persistence metadata and migration lifecycle. Subclasses define immutable domain state. */
public abstract class VersionedSavedData<T> extends SavedData {
    private final MigrationChain migrations;
    private final Codec<T> codec;
    private final String writerVersion;
    private long migratedAt;
    private T state;
    private CompoundTag preserved;
    protected VersionedSavedData(MigrationChain migrations, Codec<T> codec, String writerVersion, T initial) {
        this.migrations = migrations; this.codec = codec; this.writerVersion = writerVersion;
        state = Objects.requireNonNull(initial);
    }
    public final T state() { return state; }
    protected final void update(T replacement) {
        if (preserved != null) return;
        state = Objects.requireNonNull(replacement);
        setDirty();
    }
    public final void restore(CompoundTag input, long tick) {
        var upgraded = migrations.apply(input, tick);
        var decoded = codec.parse(NbtOps.INSTANCE, upgraded.getCompound("payload"));
        T next = decoded.result().orElseThrow(() -> new IllegalArgumentException(decoded.error().orElseThrow().message()));
        state = next;
        migratedAt = upgraded.getLong("migratedAt");
        if (input.getInt("dataVersion") != migrations.currentVersion()) {
            setDirty();
            MigrationReports.record(migrations.domain(), "Migrated version " + input.getInt("dataVersion")
                    + " to " + migrations.currentVersion());
        }
    }
    /** Runtime loading preserves unknown/corrupt bytes instead of silently replacing the save. */
    public final void restoreOrPreserve(CompoundTag input, long tick) {
        try { restore(input, tick); }
        catch (RuntimeException invalid) {
            preserved = input.copy();
            setDirty(false);
            com.mojang.logging.LogUtils.getLogger().warn("Read-only {} save: {}", migrations.domain(), invalid.getMessage());
            MigrationReports.record(migrations.domain(), "Read-only save preserved: " + invalid.getMessage());
        }
    }
    public final boolean readOnly() { return preserved != null; }
    @Override public final CompoundTag save(CompoundTag output) {
        if (preserved != null) return preserved.copy();
        var encoded = codec.encodeStart(NbtOps.INSTANCE, state).result().orElseThrow(() -> new IllegalStateException("Invalid saved state"));
        if (!(encoded instanceof CompoundTag)) throw new IllegalStateException("Saved payload must be a compound");
        output.putInt("dataVersion", migrations.currentVersion());
        output.putString("domain", migrations.domain());
        output.putString("createdBy", writerVersion);
        output.putLong("migratedAt", migratedAt);
        output.put("payload", encoded);
        return output;
    }
}

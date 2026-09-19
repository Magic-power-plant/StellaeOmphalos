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
    protected VersionedSavedData(MigrationChain migrations, Codec<T> codec, String writerVersion, T initial) {
        this.migrations = migrations; this.codec = codec; this.writerVersion = writerVersion;
        state = Objects.requireNonNull(initial);
    }
    public final T state() { return state; }
    protected final void update(T replacement) {
        state = Objects.requireNonNull(replacement);
        setDirty();
    }
    public final void restore(CompoundTag input, long tick) {
        var upgraded = migrations.apply(input, tick);
        var decoded = codec.parse(NbtOps.INSTANCE, upgraded.getCompound("payload"));
        T next = decoded.result().orElseThrow(() -> new IllegalArgumentException(decoded.error().orElseThrow().message()));
        state = next;
        migratedAt = upgraded.getLong("migratedAt");
        if (input.getInt("dataVersion") != migrations.currentVersion()) setDirty();
    }
    @Override public final CompoundTag save(CompoundTag output) {
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

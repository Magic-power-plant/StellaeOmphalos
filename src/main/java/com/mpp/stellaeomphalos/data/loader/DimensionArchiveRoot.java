package com.mpp.stellaeomphalos.data.loader;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

/** Single access point for dimension archives. Every lookup uses DimensionDataStorage. */
public final class DimensionArchiveRoot {
    private static final Map<ServerLevel, Map<ArchiveKey, DimensionArchive>> ACTIVE = new IdentityHashMap<>();

    private DimensionArchiveRoot() {}

    public static <A extends DimensionArchive> A get(ServerLevel level, ArchiveKey key,
                                                     Supplier<A> factory,
                                                     Function<CompoundTag, A> loader) {
        @SuppressWarnings("unchecked")
        A result = (A) level.getDataStorage().computeIfAbsent(tag -> {
            A archive = loader.apply(tag);
            archive.restore(tag);
            archive.onLoaded(level);
            return archive;
        }, () -> {
            A archive = factory.get();
            archive.onLoaded(level);
            return archive;
        }, key.storageName());
        ACTIVE.computeIfAbsent(level, ignored -> new java.util.LinkedHashMap<>()).put(key, result);
        return result;
    }

    public static void tickAll(ServerLevel level) {
        var archives = ACTIVE.get(level);
        if (archives == null) return;
        for (var archive : java.util.List.copyOf(archives.values())) archive.tick(level);
    }

    public static void flushAll(ServerLevel level) {
        var archives = ACTIVE.get(level);
        if (archives == null) return;
        archives.values().forEach(archive -> archive.setDirty());
    }

    public static void unload(ServerLevel level) { ACTIVE.remove(level); }
}

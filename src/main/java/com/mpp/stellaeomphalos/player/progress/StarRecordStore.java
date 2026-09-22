package com.mpp.stellaeomphalos.player.progress;

import com.mojang.logging.LogUtils;

import net.minecraft.nbt.*;
import net.minecraft.world.level.saveddata.SavedData;

import java.io.File;
import java.util.*;

/** SavedData owns the save lifecycle; failed writes deliberately leave it dirty. */
public final class StarRecordStore extends SavedData {
    public static final String NAME = "stellaeomphalos_star_records";
    private final Map<UUID, StarRecord> records = new LinkedHashMap<>();
    private final Map<String, Tag> preserved = new LinkedHashMap<>();
    private final StarRecordIO io;
    private final java.util.function.Consumer<String> notices;
    private final CompoundTag futureArchive;
    private final StarRecord unavailable = new FakeStarRecord();
    private int failures;
    private long seedCounter;
    private long poolSeed;
    private final Set<net.minecraft.resources.ResourceLocation> publicClaims =
            new LinkedHashSet<>();

    public StarRecordStore(
            CompoundTag tag, StarRecordIO io, java.util.function.Consumer<String> notices) {
        this.io = io;
        this.notices = notices;
        futureArchive = tag.getInt("DataVersion") > 1 ? tag.copy() : null;
        if (futureArchive != null) {
            LogUtils.getLogger().warn("Future star archive version {}; opened read-only", tag.getInt("DataVersion"));
            notices.accept("future_version");
            return;
        }
        seedCounter = tag.getLong("SeedCounter");
        poolSeed =
                tag.contains("ShardPoolSeed")
                        ? tag.getLong("ShardPoolSeed")
                        : UUID.randomUUID().getMostSignificantBits();
        publicClaims.addAll(StarRecord.ids(tag, "PublicClaims"));
        var players = tag.getCompound("Players");
        for (var key : players.getAllKeys()) {
            if (key.startsWith("Corrupt_")) {
                preserved.put(key, players.get(key).copy());
                continue;
            }
            try {
                if (!players.contains(key, Tag.TAG_COMPOUND))
                    throw new IllegalArgumentException("Player record is not a compound");
                records.put(UUID.fromString(key), StarRecord.load(players.getCompound(key)));
            } catch (StarRecord.FutureRecordException future) {
                preserved.put(key, players.get(key).copy());
                LogUtils.getLogger().warn("Future star record {} retained", key);
            } catch (RuntimeException invalid) {
                LogUtils.getLogger()
                        .warn("Invalid player star record {}; preserving evidence", key, invalid);
                preserved.put("Corrupt_" + key, players.get(key).copy());
                try {
                    records.put(UUID.fromString(key), new StarRecord());
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        setDirty();
    }

    public StarRecord record(UUID id) {
        if (futureArchive != null) return unavailable;
        if (preserved.containsKey(id.toString())) return new FakeStarRecord();
        return records.computeIfAbsent(
                id,
                ignored -> {
                    setDirty();
                    return new StarRecord();
                });
    }

    public boolean contains(UUID id) {
        return futureArchive != null || records.containsKey(id) || preserved.containsKey(id.toString());
    }

    public long poolSeed() {
        return poolSeed;
    }

    public boolean claimed(net.minecraft.resources.ResourceLocation id) {
        return publicClaims.contains(id);
    }

    public void claim(net.minecraft.resources.ResourceLocation id) {
        if (futureArchive != null) return;
        if (publicClaims.add(id)) setDirty();
    }

    public void renewPool(Collection<net.minecraft.resources.ResourceLocation> eligible) {
        if (futureArchive != null) return;
        publicClaims.removeAll(eligible);
        setDirty();
    }

    public long nextSeed() {
        if (futureArchive != null) return seedCounter;
        setDirty();
        return ++seedCounter;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        if (futureArchive != null) return futureArchive.copy();
        var players = new CompoundTag();
        records.forEach((id, record) -> players.put(id.toString(), record.save()));
        preserved.forEach((key, value) -> players.put(key, value.copy()));
        tag.putInt("DataVersion", 1);
        tag.putLong("SeedCounter", seedCounter);
        tag.putLong("ShardPoolSeed", poolSeed);
        tag.put("PublicClaims", StarRecord.list(publicClaims));
        tag.put("Players", players);
        return tag;
    }

    @Override
    public void save(File file) {
        if (futureArchive != null || !isDirty()) return;
        if (io.write(file.toPath(), save(new CompoundTag()))) {
            failures = 0;
            setDirty(false);
        } else if (++failures == 3) notices.accept("save_failed");
    }

    public void unload(UUID id) {
        /* SavedData retains records; no detached mutable player cache. */
    }
}

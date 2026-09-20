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
    private int failures;
    private long seedCounter;
    private long poolSeed;
    private final Set<net.minecraft.resources.ResourceLocation> publicClaims =
            new LinkedHashSet<>();

    public StarRecordStore(
            CompoundTag tag, StarRecordIO io, java.util.function.Consumer<String> notices) {
        if (tag.getInt("DataVersion") > 1)
            throw new IllegalArgumentException("Future star archive version; file preserved");
        this.io = io;
        this.notices = notices;
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
        if (preserved.containsKey(id.toString())) return new FakeStarRecord();
        return records.computeIfAbsent(
                id,
                ignored -> {
                    setDirty();
                    return new StarRecord();
                });
    }

    public boolean contains(UUID id) {
        return records.containsKey(id) || preserved.containsKey(id.toString());
    }

    public long poolSeed() {
        return poolSeed;
    }

    public boolean claimed(net.minecraft.resources.ResourceLocation id) {
        return publicClaims.contains(id);
    }

    public void claim(net.minecraft.resources.ResourceLocation id) {
        if (publicClaims.add(id)) setDirty();
    }

    public void renewPool(Collection<net.minecraft.resources.ResourceLocation> eligible) {
        publicClaims.removeAll(eligible);
        setDirty();
    }

    public long nextSeed() {
        setDirty();
        return ++seedCounter;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
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
        if (!isDirty()) return;
        if (io.write(file.toPath(), save(new CompoundTag()))) {
            failures = 0;
            setDirty(false);
        } else if (++failures == 3) notices.accept("save_failed");
    }

    public void unload(UUID id) {
        /* SavedData retains records; no detached mutable player cache. */
    }
}

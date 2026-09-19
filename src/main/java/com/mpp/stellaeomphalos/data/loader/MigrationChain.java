package com.mpp.stellaeomphalos.data.loader;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/** Migrates a copy transactionally. Input remains untouched on gaps, future versions or failure. */
public final class MigrationChain {
    private final String domain;
    private final int currentVersion;
    private final Map<Integer, DataMigrator> steps = new TreeMap<>();
    public MigrationChain(String domain, int currentVersion, List<DataMigrator> migrations) {
        if (domain.isBlank() || currentVersion < 1) throw new IllegalArgumentException("Invalid migration domain/version");
        this.domain = domain;
        this.currentVersion = currentVersion;
        for (var step : migrations) {
            if (step.fromVersion() < 0 || step.toVersion() != step.fromVersion() + 1
                    || step.toVersion() > currentVersion || steps.putIfAbsent(step.fromVersion(), step) != null)
                throw new IllegalArgumentException("Invalid migration step " + step.fromVersion());
        }
    }
    public CompoundTag apply(CompoundTag source, long serverTick) {
        if (!source.getString("domain").equals(domain)) throw new IllegalArgumentException("Migration domain mismatch");
        if (!source.contains("payload", Tag.TAG_COMPOUND)) throw new IllegalArgumentException("Missing payload");
        int version = source.getInt("dataVersion");
        if (version < 0 || version > currentVersion) throw new IllegalArgumentException("Unsupported data version " + version);
        var result = source.copy();
        var payload = result.getCompound("payload");
        while (version < currentVersion) {
            var step = steps.get(version);
            if (step == null) throw new IllegalStateException("Migration gap at " + domain + ":" + version);
            payload = java.util.Objects.requireNonNull(step.migrate(payload.copy()), "Migrator returned null");
            version++;
        }
        result.put("payload", payload);
        result.putInt("dataVersion", version);
        if (source.getInt("dataVersion") != version) result.putLong("migratedAt", serverTick);
        return result;
    }
    public int currentVersion() { return currentVersion; }
    public String domain() { return domain; }
}

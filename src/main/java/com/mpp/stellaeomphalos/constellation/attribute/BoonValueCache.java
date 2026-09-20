package com.mpp.stellaeomphalos.constellation.attribute;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.DoubleSupplier;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;

public final class BoonValueCache {

    public record Key(UUID player, ResourceLocation attribute, @Nullable BoonModifier.Mode mode) {}

    private final Map<Key, Double> values = new HashMap<>();
    private final Set<UUID> tracked = new HashSet<>();
    private long epoch;

    public synchronized double getOrCompute(Key key, DoubleSupplier supplier) {
        var cached = values.get(key);
        if (cached != null) return cached;
        double value = supplier.getAsDouble();
        values.put(key, value);
        tracked.add(key.player());
        return value;
    }

    public synchronized void invalidate(UUID player) {
        values.keySet().removeIf(key -> key.player().equals(player));
        tracked.remove(player);
        epoch++;
    }

    public synchronized void invalidateAll() {
        values.clear();
        tracked.clear();
        epoch++;
    }

    public synchronized long epoch() {
        return epoch;
    }

    public synchronized Set<UUID> trackedPlayers() {
        return Set.copyOf(tracked);
    }

    public synchronized int size() {
        return values.size();
    }
}

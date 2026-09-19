package com.mpp.stellaeomphalos.network;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Main-thread-owned token buckets, measured using a monotonic tick counter. */
public final class NetworkThrottle {
    public record Decision(boolean accepted, boolean warn, long dropped) {}
    private record Key(UUID player, int packet) {}
    private static final class Bucket {
        double tokens; long tick; long warnAt = Long.MIN_VALUE; long dropped;
        Bucket(int capacity, long tick) { tokens = capacity; this.tick = tick; }
    }
    private final Map<Key, Bucket> buckets = new HashMap<>();
    public Decision acquire(UUID player, int packet, long tick, int capacity, double refillPerTick) {
        if (capacity < 1 || !Double.isFinite(refillPerTick) || refillPerTick <= 0) throw new IllegalArgumentException("Invalid bucket");
        var bucket = buckets.computeIfAbsent(new Key(player, packet), ignored -> new Bucket(capacity, tick));
        long elapsed = Math.max(0, tick - bucket.tick);
        bucket.tokens = Math.min(capacity, bucket.tokens + elapsed * refillPerTick);
        bucket.tick = tick;
        if (bucket.tokens >= 1) { bucket.tokens--; return new Decision(true, false, bucket.dropped); }
        bucket.dropped++;
        boolean warn = bucket.warnAt == Long.MIN_VALUE || tick - bucket.warnAt >= 1200;
        if (warn) bucket.warnAt = tick;
        return new Decision(false, warn, bucket.dropped);
    }
    public void remove(UUID player) { buckets.keySet().removeIf(key -> key.player.equals(player)); }
    public void clear() { buckets.clear(); }
    public int bucketCount() { return buckets.size(); }
}

package com.mpp.stellaeomphalos.constellation.sign;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Per-player sky session numbers plus the seed-request rate limiter.
 * The session id increments on every login/dimension switch; stale replies are dropped by the peer.
 * Rate limit: at most {@link #MAX_REQUESTS} seed requests per dimension per session,
 * at least {@link #MIN_INTERVAL_TICKS} ticks apart.
 */
public final class SkySeedSession {
    public static final int MAX_REQUESTS = 3;
    public static final int MIN_INTERVAL_TICKS = 40;

    private record Window(ResourceKey<Level> dim, int session) { }
    private static final class Counter { int count; long lastTick = Long.MIN_VALUE; }

    private final Map<UUID, Integer> sessions = new HashMap<>();
    private final Map<UUID, Map<Window, Counter>> requests = new HashMap<>();

    public synchronized int current(UUID player) { return sessions.getOrDefault(player, 0); }

    /** Starts a new session for the player; invalidates all outstanding rate-limit windows. */
    public synchronized int bump(UUID player) {
        int next = current(player) + 1;
        sessions.put(player, next);
        requests.remove(player);
        return next;
    }

    /** @return true when the request is within budget and should be answered. */
    public synchronized boolean tryAcquireRequest(UUID player, ResourceKey<Level> dim, long tick) {
        var window = new Window(dim, current(player));
        var counter = requests.computeIfAbsent(player, ignored -> new HashMap<>())
                .computeIfAbsent(window, ignored -> new Counter());
        if (counter.count >= MAX_REQUESTS) return false;
        if (counter.count > 0 && tick - counter.lastTick < MIN_INTERVAL_TICKS) return false;
        counter.count++;
        counter.lastTick = tick;
        return true;
    }

    public synchronized void remove(UUID player) {
        sessions.remove(player);
        requests.remove(player);
    }

    public synchronized void clear() {
        sessions.clear();
        requests.clear();
    }
}

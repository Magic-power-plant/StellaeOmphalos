package com.mpp.stellaeomphalos.network;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-thread protocol budget with global, packet-family and rolling violation limits.
 * The state is deliberately keyed by the authenticated sender supplied by Forge.
 */
public final class NetworkBudget {
    public enum Action { ACCEPT, DROP, ISOLATE, DISCONNECT }

    public record Policy(int cost, int familyCapacity, int familyRefill,
                         int rollingCapacity, boolean expensive) {
        public Policy {
            if (cost < 1 || familyCapacity < 1 || familyRefill < 1 || rollingCapacity < 1)
                throw new IllegalArgumentException("Invalid network policy");
        }
    }

    public record Decision(Action action, long violations, long dropped, long isolatedUntil) {
        public boolean accepted() { return action == Action.ACCEPT; }
    }

    private static final int GLOBAL_CAPACITY = 64;
    private static final int ROLLING_TICKS = 20;
    private static final int ISOLATION_TICKS = 1200;
    private static final class Bucket {
        double tokens;
        long tick;
        Bucket(double tokens, long tick) { this.tokens = tokens; this.tick = tick; }
        boolean tryTake(int capacity, int refill, int cost, long now) {
            long elapsed = Math.max(0, now - tick);
            tokens = Math.min(capacity, tokens + elapsed * (double) refill);
            tick = now;
            if (tokens < cost) return false;
            tokens -= cost;
            return true;
        }
    }
    private static final class State {
        final Bucket global = new Bucket(GLOBAL_CAPACITY, 0);
        final Map<Integer, Bucket> families = new HashMap<>();
        long windowStart;
        int rolling;
        long violations;
        long dropped;
        long isolatedUntil;
        boolean disconnected;
    }

    private final Map<UUID, State> states = new HashMap<>();

    public Decision acquire(UUID player, int family, Policy policy, long tick) {
        var state = states.computeIfAbsent(player, ignored -> new State());
        if (state.disconnected) return decision(state, Action.DISCONNECT);
        if (tick < state.isolatedUntil) {
            state.dropped++;
            state.violations++;
            state.disconnected = true;
            return decision(state, Action.DISCONNECT);
        }
        if (tick - state.windowStart >= ROLLING_TICKS) {
            state.windowStart = tick;
            state.rolling = 0;
        }
        var bucket = state.families.computeIfAbsent(family, ignored -> new Bucket(policy.familyCapacity(), tick));
        boolean accepted = state.global.tryTake(GLOBAL_CAPACITY, GLOBAL_CAPACITY, policy.cost(), tick)
                && bucket.tryTake(policy.familyCapacity(), policy.familyRefill(), policy.cost(), tick)
                && state.rolling + policy.cost() <= policy.rollingCapacity();
        if (accepted) {
            state.rolling += policy.cost();
            return decision(state, Action.ACCEPT);
        }
        state.violations++;
        state.dropped++;
        if (state.violations >= 10) {
            if (state.isolatedUntil > tick) {
                state.disconnected = true;
                return decision(state, Action.DISCONNECT);
            }
            state.isolatedUntil = tick + ISOLATION_TICKS;
            return decision(state, Action.ISOLATE);
        }
        return decision(state, Action.DROP);
    }

    public void remove(UUID player) { states.remove(player); }
    public void clear() { states.clear(); }
    public int playerCount() { return states.size(); }
    public long violations(UUID player) { return states.getOrDefault(player, new State()).violations; }

    private static Decision decision(State state, Action action) {
        return new Decision(action, state.violations, state.dropped, state.isolatedUntil);
    }
}

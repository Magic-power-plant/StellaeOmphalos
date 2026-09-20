package com.mpp.stellaeomphalos.ritual.effect;

import java.util.*;

/** Weighted, aging queue. An invocation reserves its full position quota before running. */
public final class RiteEffectDispatcher {
    private record Work(Object key, int cost, int priority, long sequence, Runnable action) {}

    private final Map<Object, Work> pending = new LinkedHashMap<>();
    private long sequence;
    private int spent;

    public void submit(Object key, int cost, int priority, Runnable action) {
        if (cost < 1 || cost > 65536) throw new IllegalArgumentException("Effect cost");
        pending.putIfAbsent(key, new Work(key, cost, priority, sequence++, action));
    }

    public int tick(int budget) {
        spent = 0;
        var entries = new ArrayList<>(pending.values());
        entries.sort(Comparator.<Work>comparingLong(w -> w.sequence() - w.priority() * 4L));
        for (var work : entries) {
            if (work.cost() > budget - spent) continue;
            pending.remove(work.key());
            spent += work.cost();
            work.action().run();
        }
        return spent;
    }

    public void cancel(Object key) {
        pending.remove(key);
    }

    public int queued() {
        return pending.size();
    }

    public int spent() {
        return spent;
    }

    public void clear() {
        pending.clear();
    }
}

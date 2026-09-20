package com.mpp.stellaeomphalos.lumen.transport;

import java.util.LinkedHashSet;
import java.util.function.LongConsumer;

/** Insertion-ordered deduplicating work queue over packed position keys; drains at most `budget` items per call. Pure, JUnit-safe. */
public final class LumenWorkQueue {
    private final LinkedHashSet<Long> pending = new LinkedHashSet<>();
    public void offer(long key) { pending.add(key); }
    /** Drains up to `budget` items as a snapshot first; keys re-offered by an action run no earlier than the next call. */
    public int process(int budget, LongConsumer action) {
        if (budget < 1) return 0;
        var batch = new java.util.ArrayList<Long>(Math.min(budget, pending.size()));
        var iterator = pending.iterator();
        while (iterator.hasNext() && batch.size() < budget) {
            batch.add(iterator.next());
            iterator.remove();
        }
        for (long key : batch) action.accept(key);
        return batch.size();
    }
    public int size() { return pending.size(); }
    public boolean isEmpty() { return pending.isEmpty(); }
    public void clear() { pending.clear(); }
}

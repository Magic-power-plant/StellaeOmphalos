package com.mpp.stellaeomphalos.core.util.collections;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/** Snapshot iteration permits callbacks to mutate the list; clear notifies each removed entry once. */
public final class ExpiryList<T> implements Iterable<T> {
    private static final class Entry<T> {
        final T value; final long expiry;
        Entry(T value, long expiry) { this.value = value; this.expiry = expiry; }
    }
    private final List<Entry<T>> entries = new ArrayList<>();
    private final Consumer<T> expired;
    private long tick;
    public ExpiryList(Consumer<T> expired) { this.expired = java.util.Objects.requireNonNull(expired); }
    public void add(T value, long lifetime) {
        if (lifetime < 1) throw new IllegalArgumentException("Nonpositive lifetime");
        entries.add(new Entry<>(java.util.Objects.requireNonNull(value), Math.addExact(tick, lifetime)));
    }
    public void advance() {
        tick++;
        for (var entry : List.copyOf(entries)) if (entry.expiry <= tick && entries.remove(entry)) expired.accept(entry.value);
    }
    public void clear() {
        var removed = List.copyOf(entries); entries.clear();
        RuntimeException failure = null;
        for (var entry : removed) {
            try { expired.accept(entry.value); } catch (RuntimeException exception) {
                if (failure == null) failure = exception; else failure.addSuppressed(exception);
            }
        }
        if (failure != null) throw failure;
    }
    public int size() { return entries.size(); }
    @Override public Iterator<T> iterator() { return entries.stream().map(entry -> entry.value).toList().iterator(); }
}

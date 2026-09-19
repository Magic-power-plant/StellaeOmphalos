package com.mpp.stellaeomphalos.core.util.collections;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

/** Main-thread confined countdown map. Expiry callbacks run once while the value is still visible. */
public final class TickExpiryMap<K, V> {
    private record Entry<V>(V value, long expires) {}
    private final Map<K, Entry<V>> entries = new LinkedHashMap<>();
    private final BiConsumer<K, V> expired;
    private long tick;
    public TickExpiryMap(BiConsumer<K, V> expired) { this.expired = java.util.Objects.requireNonNull(expired); }
    public void put(K key, V value, long lifetime) {
        if (lifetime < 1) throw new IllegalArgumentException("Nonpositive lifetime");
        entries.put(key, new Entry<>(java.util.Objects.requireNonNull(value), Math.addExact(tick, lifetime)));
    }
    public Optional<V> get(K key) { return Optional.ofNullable(entries.get(key)).map(Entry::value); }
    public void remove(K key) { entries.remove(key); }
    public void advance() {
        tick++;
        var snapshot = entries.entrySet().stream().map(entry -> Map.entry(entry.getKey(), entry.getValue())).toList();
        for (var entry : snapshot) {
            if (entry.getValue().expires <= tick && entries.get(entry.getKey()) == entry.getValue()) {
                try { expired.accept(entry.getKey(), entry.getValue().value); }
                finally { entries.remove(entry.getKey(), entry.getValue()); }
            }
        }
    }
    public int size() { return entries.size(); }
    public void clear() { entries.clear(); }
}

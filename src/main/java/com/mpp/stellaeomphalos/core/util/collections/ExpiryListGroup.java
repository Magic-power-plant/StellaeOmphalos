package com.mpp.stellaeomphalos.core.util.collections;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public final class ExpiryListGroup<K, V> {
    private final Map<K, ExpiryList<V>> groups = new LinkedHashMap<>();
    private final BiConsumer<K, V> expired;
    public ExpiryListGroup(BiConsumer<K, V> expired) { this.expired = java.util.Objects.requireNonNull(expired); }
    public void add(K key, V value, long lifetime) {
        groups.computeIfAbsent(key, ignored -> new ExpiryList<>(entry -> expired.accept(key, entry))).add(value, lifetime);
    }
    public void advance() {
        for (var entry : groups.entrySet().stream().map(entry -> Map.entry(entry.getKey(), entry.getValue())).toList()) {
            entry.getValue().advance();
            if (entry.getValue().size() == 0) groups.remove(entry.getKey(), entry.getValue());
        }
    }
    public void remove(K key) { var removed = groups.remove(key); if (removed != null) removed.clear(); }
    public int groupCount() { return groups.size(); }
}

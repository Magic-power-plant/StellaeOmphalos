package com.mpp.stellaeomphalos.core.util.collections;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.random.RandomGenerator;

public final class CappedWeightedPool<T> {
    private final int capacity;
    private final Map<T, Integer> weights = new LinkedHashMap<>();
    public CappedWeightedPool(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("Nonpositive capacity");
        this.capacity = capacity;
    }
    public boolean offer(WeightedEntry<T> entry) {
        if (weights.containsKey(entry.value()) || weights.size() >= capacity) return false;
        weights.put(entry.value(), entry.weight());
        return true;
    }
    public Optional<T> sample(RandomGenerator random) {
        long total = weights.values().stream().mapToLong(Integer::longValue).sum();
        if (total == 0) return Optional.empty();
        long chosen = random.nextLong(total);
        for (var entry : weights.entrySet()) {
            chosen -= entry.getValue();
            if (chosen < 0) return Optional.of(entry.getKey());
        }
        throw new IllegalStateException("Invalid weight sum");
    }
}

package com.mpp.stellaeomphalos.core.util.collections;

public record WeightedEntry<T>(int weight, T value) {
    public WeightedEntry {
        if (weight < 0) throw new IllegalArgumentException("Negative weight");
        java.util.Objects.requireNonNull(value);
    }
}

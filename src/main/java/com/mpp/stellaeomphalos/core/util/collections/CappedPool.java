package com.mpp.stellaeomphalos.core.util.collections;

import java.util.Optional;
import java.util.random.RandomGenerator;

public final class CappedPool<T> {
    private final OrderedUniqueList<T> values = new OrderedUniqueList<>();
    private final int capacity;
    public CappedPool(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("Nonpositive capacity");
        this.capacity = capacity;
    }
    public boolean offer(T value) { return values.size() < capacity && values.add(value); }
    public Optional<T> getRandom(RandomGenerator random) {
        return values.size() == 0 ? Optional.empty() : Optional.of(values.get(random.nextInt(values.size())));
    }
    public Optional<T> getRandomIfSpaceFilled(RandomGenerator random, double emptinessFactor) {
        if (!Double.isFinite(emptinessFactor) || emptinessFactor < 0 || emptinessFactor > 1) throw new IllegalArgumentException("Factor outside [0,1]");
        double chance = 1 - emptinessFactor * (1 - values.size() / (double) capacity);
        return random.nextDouble() < chance ? getRandom(random) : Optional.empty();
    }
    public int size() { return values.size(); }
}

package com.mpp.stellaeomphalos.core.util.collections;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class OrderedUniqueList<T> {
    private final List<T> order = new ArrayList<>();
    private final Set<T> membership = new HashSet<>();
    public boolean add(T value) {
        if (!membership.add(java.util.Objects.requireNonNull(value))) return false;
        order.add(value);
        return true;
    }
    public boolean remove(T value) {
        if (!membership.remove(value)) return false;
        order.remove(value);
        return true;
    }
    public T get(int index) { return order.get(index); }
    public List<T> snapshot() { return List.copyOf(order); }
    public int size() { return order.size(); }
    public void clear() { order.clear(); membership.clear(); }
}

package com.mpp.stellaeomphalos.data.codec;

import net.minecraft.nbt.*;

/**
 * Order-independent multiset containment: duplicate requirements consume distinct list elements.
 */
public final class NbtSubset {
    private NbtSubset() {}

    public static boolean contains(Tag actual, Tag expected) {
        if (expected == null) return true;
        if (actual == null) return false;
        if (expected instanceof CompoundTag subset && actual instanceof CompoundTag whole) {
            for (String key : subset.getAllKeys())
                if (!contains(whole.get(key), subset.get(key))) return false;
            return true;
        }
        if (expected instanceof ListTag subset && actual instanceof ListTag whole) {
            int[] assigned = new int[whole.size()];
            java.util.Arrays.fill(assigned, -1);
            for (int i = 0; i < subset.size(); i++)
                if (!assign(i, subset, whole, assigned, new boolean[whole.size()])) return false;
            return true;
        }
        return expected.equals(actual);
    }

    private static boolean assign(
            int index, ListTag subset, ListTag whole, int[] assigned, boolean[] seen) {
        for (int j = 0; j < whole.size(); j++)
            if (!seen[j] && contains(whole.get(j), subset.get(index))) {
                seen[j] = true;
                if (assigned[j] < 0 || assign(assigned[j], subset, whole, assigned, seen)) {
                    assigned[j] = index;
                    return true;
                }
            }
        return false;
    }
}

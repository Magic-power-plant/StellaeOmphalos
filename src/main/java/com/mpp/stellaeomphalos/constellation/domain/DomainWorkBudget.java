package com.mpp.stellaeomphalos.constellation.domain;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

import java.util.*;
import java.util.function.Predicate;

/** A scoped quota caps entity collection and block candidate checks in one scheduled effect. */
public final class DomainWorkBudget implements AutoCloseable {
    private static final ThreadLocal<DomainWorkBudget> ACTIVE = new ThreadLocal<>();
    private final DomainWorkBudget previous;
    private int remaining;

    private DomainWorkBudget(int quota) {
        remaining = quota;
        previous = ACTIVE.get();
        ACTIVE.set(this);
    }

    public static DomainWorkBudget begin(int quota) {
        if (quota < 0) throw new IllegalArgumentException("Quota");
        return new DomainWorkBudget(quota);
    }

    public static boolean take() {
        var b = ACTIVE.get();
        if (b == null) return true;
        if (b.remaining == 0) return false;
        b.remaining--;
        return true;
    }

    public static int available() {
        var b = ACTIVE.get();
        return b == null ? 256 : b.remaining;
    }

    public static <T extends Entity> List<T> entities(
            ServerLevel level, Class<T> type, AABB box, Predicate<T> predicate, int max) {
        int cap = Math.min(max, available());
        if (cap == 0) return List.of();
        var result = new ArrayList<T>();
        level.getEntities(EntityTypeTest.forClass(type), box, predicate, result, cap);
        var b = ACTIVE.get();
        if (b != null) b.remaining -= result.size();
        return result;
    }

    public static <T extends Entity> List<T> entities(ServerLevel level, Class<T> type, AABB box) {
        return entities(level, type, box, e -> true, 64);
    }

    public void close() {
        if (previous == null) ACTIVE.remove();
        else ACTIVE.set(previous);
    }
}

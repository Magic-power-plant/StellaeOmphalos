package com.mpp.stellaeomphalos.constellation.domain;

import com.mpp.stellaeomphalos.constellation.sign.MajorSign;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nullable;

/**
 * Uniform entity collector: the unit AABB at the center inflated by the effect size, an {@code
 * enabled} switch, a secondary {@code searchFilter}, and a hard {@code maxTargets} cap (new in this
 * project). All effect entity scans must go through this class.
 */
public abstract class DomainEntityScan<T extends Entity> extends DomainEffect {
    private final Class<T> type;
    private final int maxTargets;
    private boolean enabled = true;
    private Predicate<T> searchFilter = entity -> true;

    protected DomainEntityScan(@Nullable MajorSign owner, Class<T> type, int maxTargets) {
        super(owner);
        if (maxTargets < 1) throw new IllegalArgumentException("Nonpositive maxTargets");
        this.type = type;
        this.maxTargets = maxTargets;
    }

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setSearchFilter(Predicate<T> filter) {
        this.searchFilter = java.util.Objects.requireNonNull(filter);
    }

    public int maxTargets() {
        return maxTargets;
    }

    /** Empty when disabled; otherwise at most {@code maxTargets} entities passing the filter. */
    public List<T> collect(ServerLevel level, BlockPos center, double size) {
        if (!enabled) return List.of();
        var box = new AABB(center).inflate(size);
        return DomainWorkBudget.entities(level, type, box, searchFilter, maxTargets);
    }

    /** Hard cap, pure for unit tests. */
    public static <X> List<X> cap(List<X> input, int maxTargets) {
        return input.size() <= maxTargets ? input : input.subList(0, maxTargets);
    }
}

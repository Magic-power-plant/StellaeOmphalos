package com.mpp.stellaeomphalos.core.platform.api;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Event;

/** Version 1 movement query. Dispatched on the entity's logical thread on both sides. */
public final class WaterMovementQueryEvent extends Event {
    private final LivingEntity entity;
    private float slowdown;
    /** Creates a query with vanilla's calculated slowdown. */
    public WaterMovementQueryEvent(LivingEntity entity, float slowdown) { this.entity = entity; this.slowdown = slowdown; }
    /** Entity whose movement is being queried; never retain beyond this callback. */
    public LivingEntity entity() { return entity; }
    /** Returns the effective water drag multiplier. */
    public float slowdown() { return slowdown; }
    /** Sets a finite drag multiplier in [0,1]. */
    public void setSlowdown(float value) {
        if (!Float.isFinite(value)) throw new IllegalArgumentException("Nonfinite water slowdown");
        slowdown = Math.max(0, Math.min(1, value));
    }
}

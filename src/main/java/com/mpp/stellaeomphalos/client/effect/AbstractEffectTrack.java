package com.mpp.stellaeomphalos.client.effect;

/** Owns effect age and terminal state; subclasses cannot revive an expired track. */
public abstract class AbstractEffectTrack implements EffectTrack {
    protected int age;
    private final int lifetime, priority;
    private final boolean mandatory;
    private final EffectLane lane;
    private boolean expired;

    protected AbstractEffectTrack(EffectLane lane, int lifetime, int priority, boolean mandatory) {
        if (lifetime < 1) throw new IllegalArgumentException("Lifetime must be positive");
        this.lane = lane;
        this.lifetime = lifetime;
        this.priority = priority;
        this.mandatory = mandatory;
    }

    @Override
    public final EffectLane lane() {
        return lane;
    }

    @Override
    public final boolean isExpired() {
        return expired;
    }

    @Override
    public final boolean isMandatory() {
        return mandatory;
    }

    @Override
    public final int priority() {
        return priority;
    }

    @Override
    public final void expire() {
        expired = true;
    }

    @Override
    public final void tick() {
        if (!expired) {
            advance();
            if (++age >= lifetime) expire();
        }
    }

    protected void advance() {}

    public final int age() {
        return age;
    }

    public final float remaining(float partial) {
        return Math.max(0, 1 - (age + partial) / lifetime);
    }
}

package com.mpp.stellaeomphalos.client.view;

public abstract class AbstractViewSequence implements CameraController {
    private final int duration, priority;
    protected int age;
    private boolean stopped;

    protected AbstractViewSequence(int duration, int priority) {
        if (duration < 1 || duration > 2400)
            throw new IllegalArgumentException("Invalid camera duration");
        this.duration = duration;
        this.priority = priority;
    }

    public final int priority() {
        return priority;
    }

    public final boolean finished() {
        return stopped || age >= duration;
    }

    public final void tick() {
        if (!finished()) age++;
    }

    public final void stop() {
        stopped = true;
    }
}

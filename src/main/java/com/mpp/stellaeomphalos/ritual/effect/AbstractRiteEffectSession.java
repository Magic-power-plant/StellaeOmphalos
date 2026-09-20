package com.mpp.stellaeomphalos.ritual.effect;

/** Owns effect attachment, suspend/resume and termination. All transitions are idempotent. */
public abstract class AbstractRiteEffectSession {
    public enum State {
        NEW,
        RUNNING,
        SUSPENDED,
        DETACHED
    }

    private State state = State.NEW;
    private long lastTick;

    public final State state() {
        return state;
    }

    public final void tick(long now) {
        if (state == State.DETACHED) return;
        if (state == State.NEW) onStart();
        else if (state == State.SUSPENDED) onResume();
        state = State.RUNNING;
        onTick(lastTick == 0 ? 1 : (int) Math.min(Integer.MAX_VALUE, Math.max(1, now - lastTick)));
        lastTick = now;
    }

    public final void suspend() {
        if (state == State.RUNNING) {
            state = State.SUSPENDED;
            onSuspend();
        }
    }

    public final void detach(RiteEffect.EndReason reason) {
        if (state != State.DETACHED) {
            suspend();
            state = State.DETACHED;
            onDetach(reason);
        }
    }

    protected abstract void onStart();

    protected abstract void onResume();

    protected abstract void onTick(int elapsed);

    protected abstract void onSuspend();

    protected abstract void onDetach(RiteEffect.EndReason reason);
}

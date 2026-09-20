package com.mpp.stellaeomphalos.ritual.rite;

import net.minecraft.nbt.CompoundTag;

/** Persistent lifecycle state belongs to this abstract entity, never to an interface default. */
public abstract class AbstractRiteInstance {
    protected RiteState state = RiteState.IDLE, resumeState = RiteState.RUNNING;
    protected int progress,
            cycles,
            consumedLumen,
            interrupts,
            warmup,
            failures,
            interruptedTicks,
            cooldown;
    protected boolean warmupPaid, started;
    protected long revision;

    public final RiteState state() {
        return state;
    }

    public final int progress() {
        return progress;
    }

    public final int cycles() {
        return cycles;
    }

    public final int consumedLumen() {
        return consumedLumen;
    }

    public final int interrupts() {
        return interrupts;
    }

    public final long revision() {
        return revision;
    }

    protected final void transition(RiteState next, RiteHost host) {
        if (state == next) return;
        var old = state;
        state = next;
        revision++;
        host.stateChanged(old, next);
    }

    protected void saveLifecycle(CompoundTag n) {
        n.putString("State", state.name());
        n.putString("ResumeState", resumeState.name());
        n.putInt("Progress", progress);
        n.putInt("Cycles", cycles);
        n.putInt("LumenUsed", consumedLumen);
        n.putInt("Interrupts", interrupts);
        n.putInt("Warmup", warmup);
        n.putInt("Failures", failures);
        n.putInt("InterruptedTicks", interruptedTicks);
        n.putInt("Cooldown", cooldown);
        n.putBoolean("WarmupPaid", warmupPaid);
        n.putBoolean("Started", started);
        n.putLong("Revision", revision);
    }

    protected void readLifecycle(CompoundTag n) {
        progress = Math.max(0, n.getInt("Progress"));
        cycles = Math.max(0, n.getInt("Cycles"));
        consumedLumen = Math.max(0, n.getInt("LumenUsed"));
        interrupts = Math.max(0, n.getInt("Interrupts"));
        warmup = Math.max(0, n.getInt("Warmup"));
        failures = Math.max(0, n.getInt("Failures"));
        interruptedTicks = Math.max(0, n.getInt("InterruptedTicks"));
        cooldown = Math.max(0, n.getInt("Cooldown"));
        warmupPaid = n.getBoolean("WarmupPaid");
        started = n.getBoolean("Started");
        revision = n.getLong("Revision");
        try {
            resumeState = RiteState.valueOf(n.getString("ResumeState"));
        } catch (IllegalArgumentException ex) {
            resumeState = RiteState.RUNNING;
        }
        try {
            state = RiteState.valueOf(n.getString("State"));
        } catch (IllegalArgumentException ex) {
            state = RiteState.SCANNING;
        }
        if (state != RiteState.LOCKED) state = RiteState.SCANNING;
    }
}

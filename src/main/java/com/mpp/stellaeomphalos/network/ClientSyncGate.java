package com.mpp.stellaeomphalos.network;

/** A single connection's main-thread-owned readiness state. */
public final class ClientSyncGate {
    public enum State { CLOSED, OPENING, OPEN }
    private State state = State.CLOSED;
    private long deadline;
    private ProtocolVersion remote;
    public void begin(ProtocolVersion version, long now) {
        if (version.major() != ProtocolVersion.CURRENT.major()) throw new IllegalArgumentException("Incompatible protocol");
        reset(); remote = version; deadline = now + 3000000000L; state = State.OPENING;
    }
    public boolean ready(long now) {
        expire(now);
        if (state != State.OPENING) return false;
        state = State.OPEN;
        return true;
    }
    public boolean allows(int minimumMinor, long now) {
        expire(now);
        return state == State.OPEN && remote.minor() >= minimumMinor;
    }
    public void expire(long now) { if (state == State.OPENING && now >= deadline) reset(); }
    public State state() { return state; }
    public void reset() { state = State.CLOSED; remote = null; deadline = 0; }
}

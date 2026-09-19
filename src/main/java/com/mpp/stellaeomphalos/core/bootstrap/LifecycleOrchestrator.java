package com.mpp.stellaeomphalos.core.bootstrap;

/** Mod loading state is process-wide; world session state is owned separately. */
public final class LifecycleOrchestrator {
    public enum Phase { CONSTRUCTING, REGISTRIES_READY, LOADED }
    private static volatile Phase phase = Phase.CONSTRUCTING;
    private LifecycleOrchestrator() {}
    public static Phase phase() { return phase; }
    public static synchronized void registriesReady() {
        if (phase != Phase.CONSTRUCTING) throw new IllegalStateException("Registries already initialized");
        phase = Phase.REGISTRIES_READY;
    }
    public static synchronized void loadComplete() {
        if (phase != Phase.REGISTRIES_READY) throw new IllegalStateException("Registries not initialized");
        phase = Phase.LOADED;
    }
}

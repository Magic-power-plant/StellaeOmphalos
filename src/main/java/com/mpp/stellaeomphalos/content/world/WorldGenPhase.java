package com.mpp.stellaeomphalos.content.world;

public enum WorldGenPhase {
    ORES,
    SURFACE,
    STRUCTURES,
    SPRING,
    POST;

    public int bit() {
        return 1 << ordinal();
    }

    public static int currentMask() {
        return (1 << values().length) - 1;
    }
}

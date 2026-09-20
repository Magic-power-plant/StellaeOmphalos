package com.mpp.stellaeomphalos.structure.match;

public enum StructureState {
    INDETERMINATE,
    FORMED,
    DEGRADED,
    BROKEN,
    LOCKED;

    public boolean canProduce() {
        return this == FORMED || this == DEGRADED;
    }
}

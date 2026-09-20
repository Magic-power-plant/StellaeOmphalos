package com.mpp.stellaeomphalos.knowledge.research;

public enum GateLevel {
    HIDDEN,
    SILHOUETTE,
    READABLE,
    ACTIVE;

    public boolean readable() {
        return compareTo(READABLE) >= 0;
    }
}

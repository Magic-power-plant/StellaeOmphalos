package com.mpp.stellaeomphalos.lumen.capability;

/** Capability facade attached to block entities (and later items/entities). */
public interface LumenHandler {
    LumenNode node();
    LumenIO io();
    /** Only meaningful for RELAY/PRISM/SINK: externally visible stored LU. */
    default long stored() { return 0L; }
    default long capacity() { return 0L; }
}

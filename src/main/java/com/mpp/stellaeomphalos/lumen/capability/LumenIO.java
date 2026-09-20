package com.mpp.stellaeomphalos.lumen.capability;

/** Lumen transfer side. Sources only emit, sinks only accept, relays/prisms transit. */
public enum LumenIO {
    SOURCE, SINK, RELAY, PRISM, NONE;

    /** Transit nodes form implicit edges with every in-range network node; sources/sinks only edge to transit nodes. */
    public boolean isTransit() { return this == RELAY || this == PRISM; }
}

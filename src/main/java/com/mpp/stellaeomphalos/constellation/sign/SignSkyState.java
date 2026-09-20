package com.mpp.stellaeomphalos.constellation.sign;

import java.util.List;
import java.util.Optional;

/** Read-only snapshot of one dimension's active sky state for the current day. */
public interface SignSkyState {
    List<Sign> activeSigns();
    float distribution(Sign sign);
    /** The omen in effect right now; empty means none (there is no NONE value). */
    Optional<CelestialOmen> omen();
    long day();
}

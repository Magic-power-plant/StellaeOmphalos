package com.mpp.stellaeomphalos.constellation.sign;

import java.util.Set;

/** A trait sign: no domain effect of its own; modifies other effects and is bound to moon phases. */
public interface TraitSign extends Sign {
    Set<MoonPhase> showupMoonPhases(long worldSeed);
}

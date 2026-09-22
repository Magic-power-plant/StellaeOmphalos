package com.mpp.stellaeomphalos.constellation.domain;

import com.mpp.stellaeomphalos.core.bootstrap.RegistrationGuard;
import com.mpp.stellaeomphalos.core.registry.ModEffects;
import com.mpp.stellaeomphalos.constellation.effect.BleedingEffect;

/** Registry declarations are loaded at assembly time, independently of pure domain tables. */
public final class DomainContent {
    public static final RegistrationGuard<BleedingEffect> BLEEDING = ModEffects.ENTRIES.declare("bleeding", BleedingEffect::new);
    private DomainContent() {}
    public static void initialize() { /* Loads declarations within the registration window. */ }
}

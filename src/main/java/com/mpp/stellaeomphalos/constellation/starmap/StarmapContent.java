package com.mpp.stellaeomphalos.constellation.starmap;

import com.mpp.stellaeomphalos.core.bootstrap.RegistrationGuard;
import com.mpp.stellaeomphalos.core.registry.ModEffects;

/** Registry declarations of the starmap module; class-loaded during the registration window. */
public final class StarmapContent {
    public static final RegistrationGuard<CheatDeathEffect> DEATH_PROTECTION =
            ModEffects.ENTRIES.declare("death_protection", CheatDeathEffect::new);

    private StarmapContent() {}

    public static void initialize() {
        // Forces class load so the DeferredRegister declarations above land in the window.
    }
}

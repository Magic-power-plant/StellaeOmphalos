package com.mpp.stellaeomphalos.player.boon.root;

import java.util.UUID;

/** Fan-out for the root behavior hooks' session cleanup (logout / server stop). */
public final class RootBehaviorCleanup {

    private RootBehaviorCleanup() {}

    public static void onLogout(UUID player) {
        RootExpSupport.clear(player);
        VerdanceRootBoon.clear(player);
        AegisRootBoon.clear(player);
        SoarRootBoon.clear(player);
    }

    public static void onServerStop() {
        RootExpSupport.clearAll();
        VerdanceRootBoon.clearAll();
        AegisRootBoon.clearAll();
        SoarRootBoon.clearAll();
    }
}

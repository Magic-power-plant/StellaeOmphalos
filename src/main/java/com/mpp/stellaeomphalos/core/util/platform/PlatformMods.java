package com.mpp.stellaeomphalos.core.util.platform;

import com.mpp.stellaeomphalos.core.platform.CompatRegistry;
import java.util.Set;

public final class PlatformMods {
    private PlatformMods() {}
    public static boolean available(String modId, Set<String> blacklist) { return !blacklist.contains(modId) && CompatRegistry.isLoaded(modId); }
}

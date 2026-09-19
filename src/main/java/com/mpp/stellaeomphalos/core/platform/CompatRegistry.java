package com.mpp.stellaeomphalos.core.platform;

import java.util.Set;
import java.util.stream.Collectors;
import net.minecraftforge.fml.ModList;

public final class CompatRegistry {
    private static Set<String> loaded = Set.of();
    private static boolean discovered;
    private CompatRegistry() {}
    public static void discover() {
        if (discovered) throw new IllegalStateException("Compatibility already initialized");
        loaded = ModList.get().getMods().stream().map(mod -> mod.getModId()).collect(Collectors.toUnmodifiableSet());
        discovered = true;
    }
    public static boolean isLoaded(String id) {
        if (!discovered) throw new IllegalStateException("Compatibility queried before common setup");
        return loaded.contains(id);
    }
}

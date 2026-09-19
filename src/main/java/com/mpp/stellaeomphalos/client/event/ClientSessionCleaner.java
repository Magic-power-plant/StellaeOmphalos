package com.mpp.stellaeomphalos.client.event;

import com.mojang.logging.LogUtils;
import java.util.LinkedHashMap;
import java.util.Map;

/** Every connection-local cache registers one explicit cleanup action. */
public final class ClientSessionCleaner {
    private static final Map<String, Runnable> CLEANERS = new LinkedHashMap<>();
    private ClientSessionCleaner() {}
    public static void register(String name, Runnable cleaner) {
        if (CLEANERS.putIfAbsent(name, cleaner) != null) throw new IllegalArgumentException("Duplicate cache " + name);
    }
    public static void clear() {
        CLEANERS.forEach((name, cleaner) -> {
            try { cleaner.run(); } catch (RuntimeException exception) { LogUtils.getLogger().error("Failed to clear client cache {}", name, exception); }
        });
    }
}

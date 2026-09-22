package com.mpp.stellaeomphalos.data.loader;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Bounded handoff from persistence loading to the server-session notification service. */
public final class MigrationReports {
    private static final Map<String, List<String>> PENDING = new LinkedHashMap<>();
    private MigrationReports() {}
    public static synchronized void record(String domain, String message) {
        if (PENDING.size() >= 128 && !PENDING.containsKey(domain))
            PENDING.remove(PENDING.keySet().iterator().next());
        PENDING.put(domain, List.of(message));
    }
    public static synchronized Map<String, List<String>> drain() {
        var copy = Map.copyOf(PENDING);
        PENDING.clear();
        return copy;
    }
}

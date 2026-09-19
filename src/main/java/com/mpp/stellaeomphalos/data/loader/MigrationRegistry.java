package com.mpp.stellaeomphalos.data.loader;

import java.util.LinkedHashMap;
import java.util.Map;

public final class MigrationRegistry {
    private final Map<String, MigrationChain> domains = new LinkedHashMap<>();
    public void register(MigrationChain chain) {
        if (domains.putIfAbsent(chain.domain(), chain) != null) throw new IllegalArgumentException("Duplicate migration domain");
    }
    public MigrationChain forDomain(String domain) {
        var chain = domains.get(domain);
        if (chain == null) throw new IllegalArgumentException("Unknown migration domain " + domain);
        return chain;
    }
}

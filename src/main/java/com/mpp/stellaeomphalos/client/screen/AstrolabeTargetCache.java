package com.mpp.stellaeomphalos.client.screen;

import com.mpp.stellaeomphalos.network.toClient.AstrolabeFixPayload;

import net.minecraft.resources.ResourceLocation;

import java.util.*;

/** One result and one request clock per catalogued target, cleared on session/dimension change. */
public final class AstrolabeTargetCache {
    private static final Map<ResourceLocation, AstrolabeFixPayload> RESULTS = new HashMap<>();
    private static final Map<ResourceLocation, Long> REQUESTED = new HashMap<>();

    private AstrolabeTargetCache() {}

    public static void accept(AstrolabeFixPayload fix) {
        if (RESULTS.size() < 32 || RESULTS.containsKey(fix.targetId()))
            RESULTS.put(fix.targetId(), fix);
    }

    public static AstrolabeFixPayload get(ResourceLocation id) {
        return RESULTS.get(id);
    }

    public static boolean request(ResourceLocation id) {
        long now = net.minecraft.Util.getMillis();
        if (now - REQUESTED.getOrDefault(id, -10000L) < 10000) return false;
        if (!com.mpp.stellaeomphalos.client.OmphalosClient.sendDependent(
                new com.mpp.stellaeomphalos.network.toServer.AstrolabeQueryPayload(id)))
            return false;
        REQUESTED.put(id, now);
        return true;
    }

    public static void clear() {
        RESULTS.clear();
        REQUESTED.clear();
    }
}

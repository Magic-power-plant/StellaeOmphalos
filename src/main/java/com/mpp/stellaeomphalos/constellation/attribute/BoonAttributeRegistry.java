package com.mpp.stellaeomphalos.constellation.attribute;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;

public final class BoonAttributeRegistry {

    private static final Map<ResourceLocation, BoonAttribute> TYPES = new LinkedHashMap<>();

    private BoonAttributeRegistry() {}

    public static synchronized <T extends BoonAttribute> T register(T attribute) {
        var previous = TYPES.putIfAbsent(attribute.id(), attribute);
        if (previous != null) throw new IllegalStateException("duplicate boon attribute " + attribute.id());
        return attribute;
    }

    @Nullable
    public static synchronized BoonAttribute get(ResourceLocation id) {
        return TYPES.get(id);
    }

    public static synchronized BoonAttribute require(ResourceLocation id) {
        var attribute = TYPES.get(id);
        if (attribute == null) throw new IllegalArgumentException("unknown boon attribute " + id);
        return attribute;
    }

    public static synchronized boolean isRegistered(ResourceLocation id) {
        return TYPES.containsKey(id);
    }

    public static synchronized Collection<BoonAttribute> all() {
        return Collections.unmodifiableCollection(TYPES.values());
    }

    public static synchronized int size() {
        return TYPES.size();
    }
}

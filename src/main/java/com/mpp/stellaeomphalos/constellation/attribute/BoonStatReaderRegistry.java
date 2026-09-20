package com.mpp.stellaeomphalos.constellation.attribute;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

public final class BoonStatReaderRegistry {

    private static final Map<ResourceLocation, BoonStatReader> READERS = new LinkedHashMap<>();

    private BoonStatReaderRegistry() {}

    public static synchronized void register(BoonStatReader reader) {
        var id = reader.attribute().id();
        if (!BoonAttributeRegistry.isRegistered(id))
            throw new IllegalArgumentException("reader for unknown boon attribute " + id);
        if (READERS.putIfAbsent(id, reader) != null)
            throw new IllegalStateException("reader already registered for " + id);
    }

    public static synchronized BoonStatReader readerFor(BoonAttribute attribute) {
        var reader = READERS.get(attribute.id());
        return reader != null ? reader : new FlatStatReader(attribute);
    }

    public static synchronized boolean hasReader(BoonAttribute attribute) {
        return READERS.containsKey(attribute.id());
    }
}

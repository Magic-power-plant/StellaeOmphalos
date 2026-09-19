package com.mpp.stellaeomphalos.network;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/** A capability registry allows domain modules to install handlers without framework imports of gameplay. */
public final class PayloadHandlers<C> {
    private final Map<Class<?>, BiConsumer<C, OmphalosPayload>> handlers = new HashMap<>();
    public <T extends OmphalosPayload> void register(Class<T> type, BiConsumer<C, T> handler) {
        if (handlers.putIfAbsent(type, (context, payload) -> handler.accept(context, type.cast(payload))) != null)
            throw new IllegalArgumentException("Duplicate payload handler " + type.getName());
    }
    public boolean dispatch(C context, OmphalosPayload payload) {
        var handler = handlers.get(payload.getClass());
        if (handler == null) return false;
        handler.accept(context, payload); return true;
    }
}

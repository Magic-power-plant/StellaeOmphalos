package com.mpp.stellaeomphalos.ritual.effect;

import com.mpp.stellaeomphalos.constellation.domain.DomainEffectRegistry;

import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.function.Supplier;

/** Explicit extension factories override neither existing effects nor lifecycle ownership. */
public final class RiteEffectRegistry {
    private static final Map<ResourceLocation, Supplier<? extends RiteEffect>> EXTENSIONS =
            new LinkedHashMap<>();

    private RiteEffectRegistry() {}

    public static void register(ResourceLocation id, Supplier<? extends RiteEffect> factory) {
        if (EXTENSIONS.putIfAbsent(id, factory) != null)
            throw new IllegalArgumentException("Duplicate rite effect " + id);
    }

    public static Optional<RiteEffect> create(ResourceLocation id) {
        var factory = EXTENSIONS.get(id);
        return factory != null
                ? Optional.of(factory.get())
                : DomainEffectRegistry.bySign(id) != null
                        ? Optional.of(new DomainRiteEffect(id))
                        : Optional.empty();
    }
}

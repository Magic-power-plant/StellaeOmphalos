package com.mpp.stellaeomphalos.core.registry;

import com.mpp.stellaeomphalos.Omphalos;
import com.mpp.stellaeomphalos.core.bootstrap.LifecycleOrchestrator;
import com.mpp.stellaeomphalos.core.bootstrap.RegistrationGuard;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;

/** A family owns declaration and attachment of one registry. */
public final class RegistryFamily<T> {
    private final DeferredRegister<T> register;
    private static final ThreadLocal<Boolean> CONSTRUCTING_ENTRY = ThreadLocal.withInitial(() -> false);
    private boolean attached;
    public RegistryFamily(ResourceKey<? extends Registry<T>> key) {
        register = DeferredRegister.create(key, Omphalos.MODID);
    }
    public <V extends T> RegistrationGuard<V> declare(String name, Supplier<V> factory) {
        if (!name.matches("[a-z][a-z0-9_]*")) throw new IllegalArgumentException("Invalid registry name: " + name);
        var reference = register.register(name, () -> {
            boolean previous = CONSTRUCTING_ENTRY.get(); CONSTRUCTING_ENTRY.set(true);
            try { return factory.get(); } finally { CONSTRUCTING_ENTRY.set(previous); }
        });
        return new RegistrationGuard<>(reference.getId(), reference,
                () -> LifecycleOrchestrator.phase() != LifecycleOrchestrator.Phase.CONSTRUCTING
                        || CONSTRUCTING_ENTRY.get() && reference.isPresent());
    }
    public void attach(IEventBus bus) {
        if (attached) throw new IllegalStateException("Duplicate registry attachment");
        attached = true;
        register.register(bus);
    }
    public Set<ResourceLocation> ids() {
        return register.getEntries().stream().map(entry -> entry.getId()).collect(Collectors.toUnmodifiableSet());
    }
    public ResourceLocation key() { return register.getRegistryKey().location(); }
}

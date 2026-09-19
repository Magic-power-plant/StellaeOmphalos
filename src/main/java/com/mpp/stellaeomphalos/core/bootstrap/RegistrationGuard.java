package com.mpp.stellaeomphalos.core.bootstrap;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;

/** Guards lazy references without reflecting into Forge or forcing early resolution. */
public final class RegistrationGuard<T> implements Supplier<T> {
    private final ResourceLocation id;
    private final Supplier<T> supplier;
    private final BooleanSupplier ready;
    public RegistrationGuard(ResourceLocation id, Supplier<T> supplier, BooleanSupplier ready) {
        this.id = Objects.requireNonNull(id);
        this.supplier = Objects.requireNonNull(supplier);
        this.ready = Objects.requireNonNull(ready);
    }
    public ResourceLocation id() { return id; }
    @Override public T get() {
        if (!ready.getAsBoolean()) throw new IllegalStateException("Early registry access: " + id);
        return Objects.requireNonNull(supplier.get(), "Missing registration: " + id);
    }
}

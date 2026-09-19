package com.mpp.stellaeomphalos.data.loader;

import com.mojang.serialization.Codec;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/** A codec and its semantic validator define a reloadable, immutable table. */
public record DataTable<T>(ResourceLocation id, Codec<T> codec, Validator<T> validator) {
    @FunctionalInterface public interface Validator<T> {
        void validate(Map<ResourceLocation, T> entries, DataLoadReport report);
    }
    public DataTable {
        Objects.requireNonNull(id); Objects.requireNonNull(codec); Objects.requireNonNull(validator);
    }
    public String directory() { return id.getPath(); }
}

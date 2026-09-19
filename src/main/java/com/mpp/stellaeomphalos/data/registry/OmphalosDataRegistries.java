package com.mpp.stellaeomphalos.data.registry;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.registries.DataPackRegistryEvent;

/** Strict engine registries are opt-in. Recoverable policy tables use DataTableLoader. */
public final class OmphalosDataRegistries {
    private record Definition<T>(ResourceKey<Registry<T>> key, Codec<T> codec, Codec<T> networkCodec) {
        void attach(DataPackRegistryEvent.NewRegistry event) { event.dataPackRegistry(key, codec, networkCodec); }
    }
    private static final List<Definition<?>> DEFINITIONS = new ArrayList<>();
    private static boolean frozen;
    private OmphalosDataRegistries() {}
    public static <T> void declare(ResourceKey<Registry<T>> key, Codec<T> codec, Codec<T> networkCodec) {
        if (frozen || DEFINITIONS.stream().anyMatch(definition -> definition.key.equals(key)))
            throw new IllegalStateException("Duplicate or late datapack registry " + key);
        DEFINITIONS.add(new Definition<>(key, codec, networkCodec));
    }
    public static void register(DataPackRegistryEvent.NewRegistry event) {
        frozen = true;
        DEFINITIONS.forEach(definition -> definition.attach(event));
    }
}

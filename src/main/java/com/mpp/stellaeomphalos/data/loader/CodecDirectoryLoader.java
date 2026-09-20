package com.mpp.stellaeomphalos.data.loader;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.*;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.*;
import java.util.function.Consumer;

/** Lower-layer loader publishes typed immutable snapshots without importing gameplay domains. */
public final class CodecDirectoryLoader<T> extends SimpleJsonResourceReloadListener {
    private final Codec<T> codec;
    private final Consumer<Map<ResourceLocation, T>> publish;

    public CodecDirectoryLoader(
            String directory, Codec<T> codec, Consumer<Map<ResourceLocation, T>> publish) {
        super(new Gson(), directory);
        this.codec = codec;
        this.publish = publish;
    }

    protected void apply(
            Map<ResourceLocation, JsonElement> values,
            ResourceManager manager,
            ProfilerFiller profiler) {
        var next = new TreeMap<ResourceLocation, T>();
        values.forEach(
                (id, json) -> {
                    try {
                        var result = codec.parse(JsonOps.INSTANCE, json);
                        result.result().ifPresent(v -> next.put(id, v));
                        result.error()
                                .ifPresent(
                                        e ->
                                                LogUtils.getLogger()
                                                        .error("Rejected {}: {}", id, e.message()));
                    } catch (RuntimeException e) {
                        LogUtils.getLogger().error("Rejected {}", id, e);
                    }
                });
        publish.accept(Map.copyOf(next));
    }
}

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
    private final String directory;
    private boolean preparationFailed;
    private final Consumer<Map<ResourceLocation, T>> publish;

    public CodecDirectoryLoader(
            String directory, Codec<T> codec, Consumer<Map<ResourceLocation, T>> publish) {
        super(new Gson(), directory);
        this.directory = directory;
        this.codec = codec;
        this.publish = publish;
    }

    @Override protected Map<ResourceLocation, JsonElement> prepare(ResourceManager manager, ProfilerFiller profiler) {
        preparationFailed = false;
        var values = new TreeMap<ResourceLocation, JsonElement>();
        for (var resource : manager.listResources(directory, id -> id.getPath().endsWith(".json")).entrySet()) {
            String path = resource.getKey().getPath();
            var id = new ResourceLocation(resource.getKey().getNamespace(), path.substring(directory.length() + 1, path.length() - 5));
            if (id.getPath().substring(id.getPath().lastIndexOf('/') + 1).startsWith("_")) continue;
            try (var reader = resource.getValue().openAsReader()) {
                values.put(id, com.mpp.stellaeomphalos.data.codec.BoundedJson.parse(reader));
            } catch (Exception invalid) {
                preparationFailed = true;
                LogUtils.getLogger().error("Rejected JSON resource {}", resource.getKey(), invalid);
            }
        }
        return values;
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
        if (!preparationFailed && next.size() == values.size()) publish.accept(Map.copyOf(next));
        else LogUtils.getLogger().warn("Keeping previous dataset: {} of {} entries rejected",
                values.size() - next.size(), values.size());
    }
}

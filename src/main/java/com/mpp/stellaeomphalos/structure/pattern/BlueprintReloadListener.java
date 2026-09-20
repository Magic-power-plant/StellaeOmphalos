package com.mpp.stellaeomphalos.structure.pattern;

import com.google.gson.*;
import com.mojang.logging.LogUtils;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.*;

public final class BlueprintReloadListener extends SimpleJsonResourceReloadListener {
    public BlueprintReloadListener() {
        super(new Gson(), "stellaeomphalos/blueprint");
    }

    @Override
    protected void apply(
            Map<ResourceLocation, JsonElement> files,
            ResourceManager manager,
            ProfilerFiller profiler) {
        var loaded = new HashMap<ResourceLocation, BuildBlueprint>();
        var active = new HashSet<ResourceLocation>();
        for (var id : files.keySet())
            try {
                resolve(id, files, loaded, active);
            } catch (RuntimeException ex) {
                LogUtils.getLogger().error("Rejecting blueprint {}: {}", id, ex.getMessage());
            }
        BlueprintRegistry.publish(loaded);
    }

    private JsonObject palette(
            ResourceLocation id,
            Map<ResourceLocation, JsonElement> files,
            Set<ResourceLocation> active) {
        if (!active.add(id) || active.size() > 16)
            throw new IllegalArgumentException("Palette include cycle");
        var value = files.get(id);
        if (value == null) throw new IllegalArgumentException("Missing blueprint " + id);
        var object = value.getAsJsonObject();
        var result =
                object.has("include")
                        ? palette(
                                new ResourceLocation(object.get("include").getAsString()),
                                files,
                                active)
                        : new JsonObject();
        if (object.has("palette"))
            object.getAsJsonObject("palette")
                    .entrySet()
                    .forEach(e -> result.add(e.getKey(), e.getValue().deepCopy()));
        active.remove(id);
        return result;
    }

    private BuildBlueprint resolve(
            ResourceLocation id,
            Map<ResourceLocation, JsonElement> files,
            Map<ResourceLocation, BuildBlueprint> loaded,
            Set<ResourceLocation> active) {
        if (loaded.containsKey(id)) return loaded.get(id);
        if (!files.containsKey(id)) throw new IllegalArgumentException("Missing include " + id);
        if (active.size() >= 16 || !active.add(id))
            throw new IllegalArgumentException("Cyclic/deep include " + id);
        try {
            var definition = files.get(id).getAsJsonObject().deepCopy();
            definition.add("palette", palette(id, files, new HashSet<>()));
            var result =
                    BlueprintJson.decode(
                            id, definition, child -> resolve(child, files, loaded, active));
            loaded.put(id, result);
            return result;
        } finally {
            active.remove(id);
        }
    }
}

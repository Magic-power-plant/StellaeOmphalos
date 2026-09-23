package com.mpp.stellaeomphalos.data.registry;

import com.google.gson.JsonObject;
import com.mpp.stellaeomphalos.core.platform.RecipeMutationApi;

import net.minecraft.resources.ResourceLocation;

import java.util.*;

/** Reload snapshots replay through the established 《星坛与制作系统》 recipe mutation contract. */
public final class WorldRecipeTables {
    private static final Map<String, Map<ResourceLocation, JsonObject>> TABLES = new HashMap<>();

    private WorldRecipeTables() {}

    public static void publish(String table, Map<ResourceLocation, JsonObject> entries) {
        var copy = new LinkedHashMap<ResourceLocation, JsonObject>();
        entries.forEach(
                (k, v) ->
                        copy.put(
                                new ResourceLocation(
                                        k.getNamespace(), "world_" + table + "/" + k.getPath()),
                                v.deepCopy()));
        TABLES.put(table, Map.copyOf(copy));
    }

    public static void replay(RecipeMutationApi api) {
        TABLES.values().forEach(table -> table.forEach((id, json) -> api.add(id, json.deepCopy())));
    }
}

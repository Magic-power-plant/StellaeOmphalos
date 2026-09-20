package com.mpp.stellaeomphalos.data.registry;

import com.google.gson.*;
import com.mpp.stellaeomphalos.core.platform.*;
import com.mpp.stellaeomphalos.data.codec.RecipeJson;

import net.minecraft.server.MinecraftServer;

import java.util.*;

/** Portable JSON mutation scripts are also the invocation surface for optional script adapters. */
public final class RecipeScriptBridge {
    private RecipeScriptBridge() {}

    public static void replay(MinecraftServer server, RecipeMutationApi api) {
        var files =
                server.getResourceManager()
                        .listResources("recipe_mutations", p -> p.getPath().endsWith(".json"));
        files.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(
                        entry -> {
                            try (var reader = entry.getValue().openAsReader()) {
                                var json = JsonParser.parseReader(reader).getAsJsonObject();
                                if (!RecipeJson.flag(json, "enabled", true)) return;
                                var operations = json.getAsJsonArray("operations");
                                if (operations.size() > 4096)
                                    throw new JsonParseException("Too many recipe mutations");
                                for (var value : operations) apply(api, value.getAsJsonObject());
                            } catch (Exception e) {
                                throw new IllegalArgumentException(
                                        "Recipe script " + entry.getKey() + ": " + e.getMessage(),
                                        e);
                            }
                        });
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new RecipeScriptEvent(api));
    }

    public static void apply(RecipeMutationApi api, JsonObject operation) {
        String action = RecipeJson.text(operation, "action", "");
        switch (action) {
            case "add" ->
                    api.add(
                            RecipeJson.id(operation.get("id").getAsString()),
                            operation.getAsJsonObject("recipe"));
            case "replace" ->
                    api.replace(
                            RecipeJson.id(operation.get("id").getAsString()),
                            operation.getAsJsonObject("recipe"));
            case "remove" -> api.remove(RecipeJson.id(operation.get("id").getAsString()));
            case "scale" ->
                    api.scale(
                            RecipeJson.id(operation.get("id").getAsString()),
                            operation.get("field").getAsString(),
                            operation.get("factor").getAsDouble());
            case "disable" -> api.disable(RecipeJson.id(operation.get("family").getAsString()));
            case "reset" -> {
                if (operation.has("family"))
                    api.resetToBaseline(RecipeJson.id(operation.get("family").getAsString()));
                else api.resetToBaseline();
            }
            default -> throw new JsonParseException("Unknown recipe mutation " + action);
        }
    }
}

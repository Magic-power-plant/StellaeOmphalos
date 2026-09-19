package com.mpp.stellaeomphalos.data.loader;

import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.Omphalos;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.MissingMappingsEvent;

/** Registry mappings must be available before worlds/datapacks load, so they are packaged resources. */
public final class RegistryRenameMap {
    public record Rename(String domain, ResourceLocation from, ResourceLocation to) {
        public static final Codec<Rename> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("domain").forGetter(Rename::domain),
                ResourceLocation.CODEC.fieldOf("from").forGetter(Rename::from),
                ResourceLocation.CODEC.fieldOf("to").forGetter(Rename::to)).apply(instance, Rename::new));
    }
    private static final Map<String, Map<ResourceLocation, ResourceLocation>> RENAMES = new LinkedHashMap<>();
    private RegistryRenameMap() {}
    public static void attach() {
        String path = "/data/" + Omphalos.MODID + "/migration/registry_renames.json";
        try (var stream = RegistryRenameMap.class.getResourceAsStream(path)) {
            if (stream == null) throw new IllegalStateException("Missing rename map " + path);
            var json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            List<Rename> entries = Rename.CODEC.listOf().parse(JsonOps.INSTANCE, json).getOrThrow(false, message -> {
                throw new IllegalArgumentException(message);
            });
            for (var entry : entries) {
                if (!entry.from().getNamespace().equals(Omphalos.MODID)) throw new IllegalArgumentException("Foreign rename source");
                if (RENAMES.computeIfAbsent(entry.domain(), ignored -> new LinkedHashMap<>()).putIfAbsent(entry.from(), entry.to()) != null)
                    throw new IllegalArgumentException("Duplicate rename source");
            }
            RENAMES.forEach((domain, entriesById) -> entriesById.keySet().forEach(id -> resolve(domain, id)));
        } catch (Exception exception) { throw new IllegalStateException("Cannot load registry rename map", exception); }
        MinecraftForge.EVENT_BUS.addListener(RegistryRenameMap::missing);
    }
    public static ResourceLocation resolve(String domain, ResourceLocation id) {
        var visited = new HashSet<ResourceLocation>();
        var entries = RENAMES.getOrDefault(domain, Map.of());
        while (entries.containsKey(id)) {
            if (!visited.add(id)) throw new IllegalArgumentException("Cyclic registry renames");
            id = entries.get(id);
        }
        return id;
    }
    private static void missing(MissingMappingsEvent event) {
        remap(event, ForgeRegistries.BLOCKS, "block");
        remap(event, ForgeRegistries.ITEMS, "item");
        remap(event, ForgeRegistries.BLOCK_ENTITY_TYPES, "block_entity");
        remap(event, ForgeRegistries.ENTITY_TYPES, "entity");
        remap(event, ForgeRegistries.FLUIDS, "fluid");
    }
    private static <T> void remap(MissingMappingsEvent event, IForgeRegistry<T> registry, String domain) {
        for (var mapping : event.getMappings(registry.getRegistryKey(), Omphalos.MODID)) {
            var target = resolve(domain, mapping.getKey());
            if (!target.equals(mapping.getKey()) && registry.containsKey(target)) mapping.remap(registry.getValue(target));
            else {
                LogUtils.getLogger().error("Unresolved {} registry mapping {}", domain, mapping.getKey());
                mapping.warn();
            }
        }
    }
}

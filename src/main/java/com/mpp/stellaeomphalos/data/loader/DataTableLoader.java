package com.mpp.stellaeomphalos.data.loader;

import com.mpp.stellaeomphalos.data.codec.BoundedJson;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/** Parses each resource independently so malformed JSON cannot abort the server reload. */
public final class DataTableLoader extends SimplePreparableReloadListener<DataTableLoader.Prepared> {
    public record Prepared(Map<ResourceLocation, Map<ResourceLocation, ?>> tables, DataLoadReport report) {}
    private final DataTableRegistry registry;
    public DataTableLoader(DataTableRegistry registry) { this.registry = registry; }

    @Override protected Prepared prepare(ResourceManager resources, ProfilerFiller profiler) {
        var result = new LinkedHashMap<ResourceLocation, Map<ResourceLocation, ?>>();
        var report = new DataLoadReport();
        try {
            for (var table : registry.tables()) loadTable(resources, table, result, report);
        } catch (RuntimeException exception) { report.error("datapack", "$", exception.toString()); }
        return new Prepared(result, report);
    }
    private static <T> void loadTable(ResourceManager resources, DataTable<T> table,
            Map<ResourceLocation, Map<ResourceLocation, ?>> result, DataLoadReport report) {
        var entries = new LinkedHashMap<ResourceLocation, T>();
        resources.listResources(table.directory(), id -> id.getPath().endsWith(".json")).forEach((file, resource) -> {
            try (Reader reader = resource.openAsReader()) {
                String path = file.getPath().substring(table.directory().length() + 1, file.getPath().length() - 5);
                if (path.startsWith("_")) return;
                var value = decode(table, file.toString(), reader, report);
                value.ifPresent(entry -> entries.put(new ResourceLocation(file.getNamespace(), path), entry));
            } catch (Exception exception) { report.error(file.toString(), "$", exception.toString()); }
        });
        try { table.validator().validate(Map.copyOf(entries), report); }
        catch (RuntimeException exception) { report.error(table.id().toString(), "$", exception.toString()); }
        result.put(table.id(), Map.copyOf(entries));
    }
    public static <T> java.util.Optional<T> decode(DataTable<T> table, String file, Reader source, DataLoadReport report) {
        try {
            var parsed = table.codec().parse(JsonOps.INSTANCE, BoundedJson.parse(source));
            parsed.error().ifPresent(error -> {
                var path = java.util.regex.Pattern.compile("\\$\\.[a-z_]+(?:\\[[0-9]+\\])?").matcher(error.message());
                String pointer = path.find() ? path.group() : error.message().contains("schema_version") ? "$.schema_version" : "$";
                report.error(file, pointer, error.message());
            });
            return parsed.result();
        } catch (RuntimeException exception) {
            report.error(file, "$", exception.getMessage() == null ? exception.toString() : exception.getMessage());
            return java.util.Optional.empty();
        }
    }
    @Override protected void apply(Prepared prepared, ResourceManager resources, ProfilerFiller profiler) {
        try {
            registry.publish(prepared.tables(), prepared.report());
            for (var issue : prepared.report().issues()) {
                LogUtils.getLogger().warn("Data {} {} {}: {}", issue.severity(), issue.file(), issue.pointer(), issue.message());
            }
        } catch (RuntimeException exception) { LogUtils.getLogger().error("Unable to publish data tables", exception); }
    }
}

package com.mpp.stellaeomphalos.content.world;

import com.google.common.hash.Hashing;
import com.google.gson.*;
import com.mpp.stellaeomphalos.data.loader.FoundationDataProvider;

import net.minecraft.data.*;
import net.minecraft.nbt.*;
import net.minecraftforge.data.event.GatherDataEvent;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Authored catalogue and SNBT are inputs; all distributed static assets are generated. */
public final class WorldDataProvider implements DataProvider {
    private final PackOutput output;
    private final Path inputs;
    private final JsonObject catalogue;

    public WorldDataProvider(PackOutput output) {
        this.output = output;
        inputs =
                Path.of(System.getProperty("stellaeomphalos.projectRoot", "."))
                        .resolve("src/main/part4");
        try (var reader =
                Files.newBufferedReader(
                        inputs.resolve("catalogue.json"),
                        java.nio.charset.StandardCharsets.UTF_8)) {
            catalogue = JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException e) {
            throw new IllegalStateException("Missing authored Part-4 data", e);
        }
        catalogue
                .getAsJsonObject("sounds")
                .entrySet()
                .forEach(
                        e ->
                                FoundationDataProvider.sound(
                                        e.getKey(), e.getValue().getAsJsonObject()));
        catalogue
                .getAsJsonObject("languages")
                .entrySet()
                .forEach(
                        e -> {
                            var a = e.getValue().getAsJsonArray();
                            FoundationDataProvider.language(
                                    e.getKey(), a.get(0).getAsString(), a.get(1).getAsString());
                        });
    }

    public static void gather(GatherDataEvent e) {
        e.getGenerator()
                .addProvider(
                        e.includeClient() || e.includeServer(),
                        new WorldDataProvider(e.getGenerator().getPackOutput()));
    }

    public CompletableFuture<?> run(CachedOutput cache) {
        var futures = new ArrayList<CompletableFuture<?>>();
        catalogue
                .getAsJsonObject("files")
                .entrySet()
                .forEach(
                        e ->
                                futures.add(
                                        DataProvider.saveStable(
                                                cache,
                                                e.getValue(),
                                                output.getOutputFolder().resolve(e.getKey()))));
        try (var templates = Files.list(inputs.resolve("templates"))) {
            for (var source : templates.sorted().toList()) {
                var tag =
                        TagParser.parseTag(
                                Files.readString(source, java.nio.charset.StandardCharsets.UTF_8));
                var bytes = new ByteArrayOutputStream();
                NbtIo.writeCompressed(tag, bytes);
                byte[] data = bytes.toByteArray();
                cache.writeIfNeeded(
                        output.getOutputFolder()
                                .resolve(
                                        "data/stellaeomphalos/structures/"
                                                + source.getFileName()
                                                        .toString()
                                                        .replace(".snbt", ".nbt")),
                        data,
                        Hashing.sha1().hashBytes(data));
            }
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
        try (var assets = Files.walk(inputs.resolve("assets"))) {
            for (var source : assets.filter(Files::isRegularFile).sorted().toList()) {
                byte[] data = Files.readAllBytes(source);
                cache.writeIfNeeded(
                        output.getOutputFolder().resolve(inputs.relativize(source)),
                        data,
                        Hashing.sha1().hashBytes(data));
            }
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    public String getName() {
        return "Stellae Omphalos Part-4 world and rites";
    }
}

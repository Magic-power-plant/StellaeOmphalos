package com.mpp.stellaeomphalos.client.view;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.*;
import java.util.*;

/**
 * Session-scoped texture ownership, bounded captures, and hash-isolated world/server disk paths.
 */
public final class ViewCaptureCache {
    private record Key(ResourceLocation dimension, BlockPos position) {}

    private static final Map<Key, ResourceLocation> TEXTURES = new LinkedHashMap<>();
    private static final Set<Key> MISSING = new HashSet<>();

    private ViewCaptureCache() {}

    private static Path path(ResourceLocation dimension, BlockPos pos) {
        var mc = Minecraft.getInstance();
        String identity;
        if (mc.getCurrentServer() != null) identity = mc.getCurrentServer().ip;
        else if (mc.getSingleplayerServer() != null)
            identity =
                    mc.getSingleplayerServer()
                            .getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                            .toAbsolutePath()
                            .toString();
        else identity = "unconnected";
        String partition =
                UUID.nameUUIDFromBytes(identity.getBytes(java.nio.charset.StandardCharsets.UTF_8))
                        .toString();
        String dim =
                UUID.nameUUIDFromBytes(
                                dimension
                                        .toString()
                                        .getBytes(java.nio.charset.StandardCharsets.UTF_8))
                        .toString();
        return mc.gameDirectory
                .toPath()
                .resolve("stellaeomphalos/viewcapture")
                .resolve(partition)
                .resolve(dim)
                .resolve(pos.getX() + "_" + pos.getY() + "_" + pos.getZ() + ".png");
    }

    public static void capture(ResourceLocation dimension, BlockPos pos) {
        if (!com.mpp.stellaeomphalos.OmphalosConfig.CLIENT.flag("view.captureTargets")) return;
        var key = new Key(dimension, pos.immutable());
        var mc = Minecraft.getInstance();
        try (var full = Screenshot.takeScreenshot(mc.getMainRenderTarget());
                var crop = new NativeImage(160, 90, false)) {
            for (int y = 0; y < 90; y++)
                for (int x = 0; x < 160; x++)
                    crop.setPixelRGBA(
                            x,
                            y,
                            full.getPixelRGBA(
                                    Math.min(
                                            full.getWidth() - 1,
                                            full.getWidth() / 5
                                                    + x * (full.getWidth() * 3 / 5) / 160),
                                    Math.min(
                                            full.getHeight() - 1,
                                            full.getHeight() / 5
                                                    + y * (full.getHeight() * 3 / 5) / 90)));
            var file = path(dimension, pos);
            Files.createDirectories(file.getParent());
            crop.writeToFile(file);
            var old = TEXTURES.remove(key);
            if (old != null) mc.getTextureManager().release(old);
            MISSING.remove(key);
        } catch (java.io.IOException error) {
            com.mojang.logging.LogUtils.getLogger().warn("Unable to save gateway view", error);
        }
    }

    public static ResourceLocation texture(ResourceLocation dimension, BlockPos pos) {
        var key = new Key(dimension, pos.immutable());
        if (TEXTURES.containsKey(key)) return TEXTURES.get(key);
        if (MISSING.contains(key) || TEXTURES.size() + MISSING.size() >= 64) return null;
        var file = path(dimension, pos);
        if (!Files.isRegularFile(file)) {
            MISSING.add(key);
            return null;
        }
        try (var input = Files.newInputStream(file)) {
            var image = NativeImage.read(input);
            if (image.getWidth() != 160 || image.getHeight() != 90) {
                image.close();
                MISSING.add(key);
                return null;
            }
            var texture = new DynamicTexture(image);
            var id =
                    new ResourceLocation(
                            "stellaeomphalos",
                            "viewcapture/"
                                    + UUID.nameUUIDFromBytes(
                                            file.toString()
                                                    .getBytes(
                                                            java.nio.charset.StandardCharsets
                                                                    .UTF_8)));
            Minecraft.getInstance().getTextureManager().register(id, texture);
            TEXTURES.put(key, id);
            return id;
        } catch (java.io.IOException failure) {
            MISSING.add(key);
            return null;
        }
    }

    public static void clear() {
        var manager = Minecraft.getInstance().getTextureManager();
        for (var id : TEXTURES.values()) manager.release(id);
        TEXTURES.clear();
        MISSING.clear();
    }

    public static int size() {
        return TEXTURES.size() + MISSING.size();
    }
}

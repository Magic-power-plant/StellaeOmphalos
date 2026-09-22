package com.mpp.stellaeomphalos.client.image;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/** Runtime sampling happens only in tick; render-time lookups are bounded cache reads. */
public final class PaletteTable {
    private static final Map<ResourceLocation, Integer> DEFAULTS = new HashMap<>(),
            CACHE = new HashMap<>();
    private static final Set<ResourceLocation> ATTEMPTED = new HashSet<>();

    private PaletteTable() {}

    public static int colorOf(ItemStack stack) {
        var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        return CACHE.getOrDefault(id, DEFAULTS.getOrDefault(id, 0xffb7dcff));
    }

    public static void reload(ResourceManager resources) {
        clear();
        DEFAULTS.clear();
        try (var stream =
                        resources.open(new ResourceLocation("stellaeomphalos:item_palette.json"));
                var reader =
                        new java.io.InputStreamReader(
                                stream, java.nio.charset.StandardCharsets.UTF_8)) {
            for (var entry :
                    com.google.gson.JsonParser.parseReader(reader).getAsJsonObject().entrySet())
                DEFAULTS.put(
                        new ResourceLocation(entry.getKey()),
                        (int) Long.parseLong(entry.getValue().getAsString(), 16));
        } catch (java.io.IOException | IllegalArgumentException error) {
            com.mojang.logging.LogUtils.getLogger().warn("Unable to read item palette", error);
        }
    }

    public static void warm(ItemStack stack) {
        if (stack.isEmpty()
                || !com.mpp.stellaeomphalos.OmphalosConfig.CLIENT.flag("palette.runtimeExtraction"))
            return;
        var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (ATTEMPTED.size() >= 512 || !ATTEMPTED.add(id)) return;
        var mc = Minecraft.getInstance();
        var sprite =
                mc.getItemRenderer()
                        .getModel(stack, mc.level, mc.player, 0)
                        .getParticleIcon()
                        .contents()
                        .name();
        var png =
                new ResourceLocation(
                        sprite.getNamespace(), "textures/" + sprite.getPath() + ".png");
        try (var stream = mc.getResourceManager().open(png);
                var image = NativeImage.read(stream)) {
            int h = Math.min(image.getWidth(), image.getHeight());
            int[] pixels = new int[image.getWidth() * h];
            for (int y = 0; y < h; y++)
                for (int x = 0; x < image.getWidth(); x++) {
                    int abgr = image.getPixelRGBA(x, y);
                    pixels[y * image.getWidth() + x] =
                            (abgr & 0xff00ff00) | (abgr & 255) << 16 | (abgr >> 16 & 255);
                }
            CACHE.put(id, PaletteExtractor.dominantColor(pixels));
        } catch (java.io.IOException ignored) {
        }
    }

    public static void clear() {
        CACHE.clear();
        ATTEMPTED.clear();
    }
}

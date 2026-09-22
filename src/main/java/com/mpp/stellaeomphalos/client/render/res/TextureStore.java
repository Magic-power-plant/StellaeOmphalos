package com.mpp.stellaeomphalos.client.render.res;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.HashMap;
import java.util.Map;

/** TextureManager owns GPU textures. This cache owns only resource descriptors. */
public final class TextureStore {
    private record Texture(ResourceLocation location, boolean isReady) implements RenderTexture {
        public float u0() {
            return 0;
        }

        public float v0() {
            return 0;
        }

        public float u1() {
            return 1;
        }

        public float v1() {
            return 1;
        }
    }

    private static final Map<ResourceLocation, RenderTexture> CACHE = new HashMap<>();

    private TextureStore() {}

    public static RenderTexture resolve(ResourceManager resources, ResourceLocation id) {
        return CACHE.computeIfAbsent(
                id, key -> new Texture(key, resources.getResource(key).isPresent()));
    }

    public static void clear() {
        CACHE.clear();
    }

    public static int size() {
        return CACHE.size();
    }
}

package com.mpp.stellaeomphalos.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RegisterShadersEvent;

import java.io.IOException;

/** State is declared here, never changed from a renderer body. All paths use NEW_ENTITY. */
public abstract class OmphalosRenderTypes extends RenderType {
    public static final ResourceLocation TEXTURE =
            new ResourceLocation("stellaeomphalos", "textures/effect/white.png");

    public enum Kind {
        BEAM_ADDITIVE,
        SKY_ADDITIVE,
        SOFT_PARTICLE,
        GLOW_LAYER,
        GHOST_BLOCK
    }

    private static final ResourceLocation BLOCK_ATLAS =
            net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS;
    public static final RenderType SOLID = entitySolid(TEXTURE);
    private static final Kind[] KINDS = Kind.values();
    private static final ShaderInstance[] SHADERS = new ShaderInstance[KINDS.length];
    private static final RenderType[] TYPES = new RenderType[KINDS.length];
    private static final RenderType EMISSIVE = entityTranslucentEmissive(TEXTURE),
            TRANSLUCENT = entityTranslucent(TEXTURE);

    static {
        for (var kind : KINDS) {
            int i = kind.ordinal();
            boolean soft = kind == Kind.SOFT_PARTICLE || kind == Kind.GHOST_BLOCK;
            TYPES[i] =
                    create(
                            "stellaeomphalos:" + kind.name().toLowerCase(java.util.Locale.ROOT),
                            DefaultVertexFormat.NEW_ENTITY,
                            VertexFormat.Mode.QUADS,
                            262144,
                            false,
                            true,
                            CompositeState.builder()
                                    .setShaderState(new ShaderStateShard(() -> SHADERS[i]))
                                    .setTextureState(
                                            new TextureStateShard(
                                                    kind == Kind.GHOST_BLOCK
                                                            ? BLOCK_ATLAS
                                                            : TEXTURE,
                                                    false,
                                                    false))
                                    .setTransparencyState(
                                            soft ? TRANSLUCENT_TRANSPARENCY : ADDITIVE_TRANSPARENCY)
                                    .setCullState(NO_CULL)
                                    .setLightmapState(LIGHTMAP)
                                    .setOverlayState(OVERLAY)
                                    .setWriteMaskState(COLOR_WRITE)
                                    .createCompositeState(false));
        }
    }

    private OmphalosRenderTypes(
            String name,
            VertexFormat f,
            VertexFormat.Mode m,
            int size,
            boolean a,
            boolean b,
            Runnable start,
            Runnable end) {
        super(name, f, m, size, a, b, start, end);
    }

    public static void register(RegisterShadersEvent event) throws IOException {
        ghostAtlas();
        for (var kind : KINDS) {
            int i = kind.ordinal();
            event.registerShader(
                    new ShaderInstance(
                            event.getResourceProvider(),
                            new ResourceLocation(
                                    "stellaeomphalos",
                                    "rendertype_" + kind.name().toLowerCase(java.util.Locale.ROOT)),
                            DefaultVertexFormat.NEW_ENTITY),
                    shader -> {
                        SHADERS[i] = shader;
                        if (kind == Kind.GHOST_BLOCK) ghostAtlas();
                    });
        }
    }

    public static RenderType ghostAtlas() {
        return type(Kind.GHOST_BLOCK);
    }

    public static int loadedShaderCount() {
        int count = 0;
        for (var shader : SHADERS) if (shader != null) count++;
        return count;
    }

    public static RenderType type(Kind kind) {
        if (!ShaderCompat.shadersInUse() && SHADERS[kind.ordinal()] != null)
            return TYPES[kind.ordinal()];
        if (kind == Kind.GHOST_BLOCK) return entityTranslucent(BLOCK_ATLAS);
        return kind == Kind.SOFT_PARTICLE ? TRANSLUCENT : EMISSIVE;
    }

    public static void finish(
            net.minecraft.client.renderer.MultiBufferSource.BufferSource buffers) {
        for (var type : TYPES) buffers.endBatch(type);
        buffers.endBatch(EMISSIVE);
        buffers.endBatch(TRANSLUCENT);
    }
}

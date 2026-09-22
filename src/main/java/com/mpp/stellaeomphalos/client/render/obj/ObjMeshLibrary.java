package com.mpp.stellaeomphalos.client.render.obj;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mpp.stellaeomphalos.OmphalosConfig;
import com.mpp.stellaeomphalos.client.render.OmphalosRenderTypes;
import com.mpp.stellaeomphalos.client.render.ShaderCompat;
import com.mpp.stellaeomphalos.client.render.util.WorldDraw;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.ArrayList;

/**
 * Owns bounded immutable crystal geometry and its GL lifetime. Called only in the world flush pass.
 */
public final class ObjMeshLibrary {
    private static final ResourceLocation CRYSTAL =
            new ResourceLocation("stellaeomphalos:models/obj/focus_crystal.obj");
    private static final Mesh[] MESHES = new Mesh[8];
    private static final BufferBuilder BUILDER = new BufferBuilder(2048);
    private static float[] positions = new float[0];
    private static int closed, uploaded;

    private ObjMeshLibrary() {}

    private static final class Mesh implements AutoCloseable {
        private final VertexBuffer buffer;
        private final RenderType type;
        private final int color;
        private boolean disposed;

        Mesh(RenderType type, int color) {
            this.type = type;
            this.color = color;
            BUILDER.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);
            for (int i = 0; i < positions.length; i += 3) {
                int corner = (i / 3) % 4;
                BUILDER.vertex(positions[i], positions[i + 1], positions[i + 2])
                        .color(color >> 16 & 255, color >> 8 & 255, color & 255, color >>> 24)
                        .uv(corner == 0 || corner == 3 ? 0 : 1, corner < 2 ? 0 : 1)
                        .overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(LightTexture.FULL_BRIGHT)
                        .normal(0, 1, 0)
                        .endVertex();
            }
            buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
            try {
                buffer.bind();
                buffer.upload(BUILDER.end());
                uploaded++;
            } catch (RuntimeException failure) {
                buffer.close();
                throw failure;
            } finally {
                VertexBuffer.unbind();
            }
        }

        public void close() {
            if (!disposed) {
                disposed = true;
                buffer.close();
                closed++;
            }
        }
    }

    public static void reload(ResourceManager resources) {
        closeAll();
        positions = new float[0];
        try (var stream = resources.open(CRYSTAL);
                var reader =
                        new java.io.BufferedReader(
                                new java.io.InputStreamReader(
                                        stream, java.nio.charset.StandardCharsets.UTF_8))) {
            var vertices = new ArrayList<float[]>();
            var output = new ArrayList<Float>();
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] fields = line.split("\\s+");
                if (fields[0].equals("v")) {
                    if (fields.length != 4 || vertices.size() >= 4096)
                        throw new java.io.IOException("Invalid OBJ vertex");
                    var point =
                            new float[] {
                                Float.parseFloat(fields[1]),
                                Float.parseFloat(fields[2]),
                                Float.parseFloat(fields[3])
                            };
                    for (float value : point)
                        if (!Float.isFinite(value) || Math.abs(value) > 16)
                            throw new java.io.IOException("Invalid OBJ coordinate");
                    vertices.add(point);
                } else if (fields[0].equals("f")) {
                    if (fields.length != 5 || output.size() > 49152)
                        throw new java.io.IOException("Crystal OBJ requires bounded quad faces");
                    for (int i = 1; i < 5; i++) {
                        int index = Integer.parseInt(fields[i]) - 1;
                        if (index < 0 || index >= vertices.size())
                            throw new java.io.IOException("Invalid OBJ index");
                        for (float v : vertices.get(index)) output.add(v);
                    }
                } else throw new java.io.IOException("Unsupported crystal OBJ directive");
            }
            if (output.isEmpty()) throw new java.io.IOException("Empty crystal mesh");
            positions = new float[output.size()];
            for (int i = 0; i < positions.length; i++) positions[i] = output.get(i);
        } catch (java.io.IOException | NumberFormatException failure) {
            com.mojang.logging.LogUtils.getLogger()
                    .error("Unable to load authored crystal mesh", failure);
        }
    }

    /**
     * Returns false when the portable batched path should be used (shader packs or disabled cache).
     */
    public static boolean drawCrystal(WorldDraw draw, int color) {
        if (positions.length == 0
                || ShaderCompat.shadersInUse()
                || !OmphalosConfig.CLIENT.flag("render.staticMeshes")) return false;
        RenderSystem.assertOnRenderThread();
        var type = OmphalosRenderTypes.type(OmphalosRenderTypes.Kind.GLOW_LAYER);
        Mesh mesh = null;
        int free = -1;
        for (int i = 0; i < MESHES.length; i++) {
            var candidate = MESHES[i];
            if (candidate != null && candidate.color == color && candidate.type == type) {
                mesh = candidate;
                break;
            }
            if (candidate == null) free = i;
        }
        if (mesh == null) {
            if (free < 0) return false;
            mesh = new Mesh(type, color);
            MESHES[free] = mesh;
        }
        // Flush pending instance geometry before binding this immutable mesh. The RenderType owns
        // all state.
        if (draw.buffers
                instanceof net.minecraft.client.renderer.MultiBufferSource.BufferSource source)
            source.endBatch();
        type.setupRenderState();
        try {
            mesh.buffer.bind();
            var shader = RenderSystem.getShader();
            if (shader == null) return false;
            mesh.buffer.drawWithShader(
                    draw.pose.last().pose(), RenderSystem.getProjectionMatrix(), shader);
            draw.vertices += positions.length / 3;
        } finally {
            VertexBuffer.unbind();
            type.clearRenderState();
        }
        return true;
    }

    public static void closeAll() {
        for (int i = 0; i < MESHES.length; i++) {
            if (MESHES[i] != null) MESHES[i].close();
            MESHES[i] = null;
        }
    }

    public static int liveCount() {
        int count = 0;
        for (var mesh : MESHES) if (mesh != null) count++;
        return count;
    }

    public static int closedCount() {
        return closed;
    }

    public static int uploadedCount() {
        return uploaded;
    }
}

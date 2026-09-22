package com.mpp.stellaeomphalos.client.render.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mpp.stellaeomphalos.client.render.OmphalosRenderTypes;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Reused per-pass context. Coordinates are world coordinates, pose is translated once by caller.
 */
public final class WorldDraw {
    public PoseStack pose;
    public MultiBufferSource buffers;
    public double cameraX, cameraY, cameraZ;
    public float partial;
    public long millis;
    public int vertices;
    public int light = LightTexture.FULL_BRIGHT;

    private static final net.minecraft.resources.ResourceLocation WHITE =
            new net.minecraft.resources.ResourceLocation("stellaeomphalos:effect/white");
    private static float whiteU0, whiteV0, whiteU1 = 1, whiteV1 = 1;
    private boolean atlasWhite;
    private float regionU0, regionV0, regionU1 = 1, regionV1 = 1;

    public VertexConsumer spriteBuffer(
            net.minecraft.client.renderer.texture.TextureAtlasSprite sprite) {
        atlasWhite = true;
        regionU0 = sprite.getU0();
        regionV0 = sprite.getV0();
        regionU1 = sprite.getU1();
        regionV1 = sprite.getV1();
        return buffers.getBuffer(OmphalosRenderTypes.ghostAtlas());
    }

    public static void refreshAtlas() {
        var sprite =
                net.minecraft.client.Minecraft.getInstance()
                        .getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS)
                        .apply(WHITE);
        whiteU0 = sprite.getU0();
        whiteV0 = sprite.getV0();
        whiteU1 = sprite.getU1();
        whiteV1 = sprite.getV1();
    }

    public VertexConsumer atlasBuffer() {
        atlasWhite = false;
        return buffers.getBuffer(OmphalosRenderTypes.ghostAtlas());
    }

    public VertexConsumer solid() {
        atlasWhite = false;
        return buffers.getBuffer(OmphalosRenderTypes.SOLID);
    }

    private final org.joml.Quaternionf rotation = new org.joml.Quaternionf();

    public org.joml.Quaternionf rotate(float x, float y, float z) {
        return rotation.rotationZYX(z, y, x);
    }

    public VertexConsumer buffer(OmphalosRenderTypes.Kind kind) {
        atlasWhite = kind == OmphalosRenderTypes.Kind.GHOST_BLOCK;
        if (atlasWhite) {
            regionU0 = whiteU0;
            regionV0 = whiteV0;
            regionU1 = whiteU1;
            regionV1 = whiteV1;
        }
        return buffers.getBuffer(OmphalosRenderTypes.type(kind));
    }

    public void vertex(
            VertexConsumer v, double x, double y, double z, int color, float u, float w) {
        vertex(v, x, y, z, color, u, w, 0, 1, 0);
    }

    public void vertex(
            VertexConsumer v,
            double x,
            double y,
            double z,
            int color,
            float u,
            float w,
            float nx,
            float ny,
            float nz) {
        v.vertex(pose.last().pose(), (float) x, (float) y, (float) z)
                .color(color >> 16 & 255, color >> 8 & 255, color & 255, color >>> 24)
                .uv(
                        atlasWhite ? regionU0 + u * (regionU1 - regionU0) : u,
                        atlasWhite ? regionV0 + w * (regionV1 - regionV0) : w)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(pose.last().normal(), nx, ny, nz)
                .endVertex();
        vertices++;
    }

    public void quad(
            VertexConsumer v,
            double x,
            double y,
            double z,
            double ax,
            double ay,
            double az,
            double bx,
            double by,
            double bz,
            int color) {
        double nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
        double length = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (length < 1e-9) return;
        float xNormal = (float) (nx / length),
                yNormal = (float) (ny / length),
                zNormal = (float) (nz / length);
        vertex(v, x - ax - bx, y - ay - by, z - az - bz, color, 0, 0, xNormal, yNormal, zNormal);
        vertex(v, x + ax - bx, y + ay - by, z + az - bz, color, 1, 0, xNormal, yNormal, zNormal);
        vertex(v, x + ax + bx, y + ay + by, z + az + bz, color, 1, 1, xNormal, yNormal, zNormal);
        vertex(v, x - ax + bx, y - ay + by, z - az + bz, color, 0, 1, xNormal, yNormal, zNormal);
    }

    public void beam(
            VertexConsumer v,
            double x,
            double y,
            double z,
            double ex,
            double ey,
            double ez,
            double width,
            int color) {
        double dx = ex - x, dy = ey - y, dz = ez - z, len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-6 || vertices > 299980) return;
        dx /= len;
        dy /= len;
        dz /= len;
        double nx = -dz, ny = 0, nz = dx, norm = Math.hypot(nx, nz);
        if (norm < 1e-6) {
            nx = 1;
            nz = 0;
            norm = 1;
        }
        nx /= norm;
        nz /= norm;
        double tx = dy * nz - dz * ny, ty = dz * nx - dx * nz, tz = dx * ny - dy * nx;
        for (int i = 0; i < 3; i++) {
            double angle = i * Math.PI / 3,
                    a = Math.cos(angle) * width,
                    b = Math.sin(angle) * width;
            double ox = nx * a + tx * b, oy = ny * a + ty * b, oz = nz * a + tz * b;
            vertex(v, x - ox, y - oy, z - oz, color, 0, 0);
            vertex(v, x + ox, y + oy, z + oz, color, 1, 0);
            vertex(v, ex + ox, ey + oy, ez + oz, color, 1, 1);
            vertex(v, ex - ox, ey - oy, ez - oz, color, 0, 1);
        }
    }

    public void box(
            VertexConsumer v,
            double x,
            double y,
            double z,
            double sx,
            double sy,
            double sz,
            int c) {
        quad(v, x, y - sy, z, sx, 0, 0, 0, 0, sz, c);
        quad(v, x, y + sy, z, -sx, 0, 0, 0, 0, sz, c);
        quad(v, x - sx, y, z, 0, -sy, 0, 0, 0, sz, c);
        quad(v, x + sx, y, z, 0, sy, 0, 0, 0, sz, c);
        quad(v, x, y, z - sz, -sx, 0, 0, 0, sy, 0, c);
        quad(v, x, y, z + sz, sx, 0, 0, 0, sy, 0, c);
    }
}

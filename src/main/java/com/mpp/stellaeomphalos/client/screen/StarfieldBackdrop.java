package com.mpp.stellaeomphalos.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.RandomSource;

/**
 * Animated deep-space backdrop modeled after {@code docs/星空背景.html}: breathing nebula
 * glow, a diagonal milky-way band, twinkling parallax stars, occasional shooting stars and a
 * corner vignette. Rendered in physical pixels so stars stay 1-2 device px regardless of the
 * Minecraft GUI scale.
 */
public final class StarfieldBackdrop {
    private static final class Star {
        float x, y, size, opacity, speed, phase, drift;
        int rgb;
    }

    private static final class Meteor {
        float x, y, vx, vy, len, decay;
        float life = 1;
    }

    private static final java.util.List<Star> stars = new java.util.ArrayList<>();
    private static final java.util.List<Meteor> meteors = new java.util.ArrayList<>();
    private static final RandomSource random = RandomSource.create(0x57A1L);
    private static int lastW = -1, lastH = -1;
    private static float px, py;
    private static long lastFrame, nextMeteorAt;

    private StarfieldBackdrop() {}

    private static int argb(float alpha, int rgb) {
        int a = Math.min(255, Math.max(0, (int) (alpha * 255)));
        return a << 24 | rgb;
    }

    /** Smooth elliptical radial gradient: opaque-ish center fading to transparent at the rim. */
    private static void radial(
            GuiGraphics g, float cx, float cy, float rx, float ry, int rgb, float alpha) {
        if (alpha <= 0.004f) return;
        g.flush();
        float r = ((rgb >> 16) & 0xff) / 255f,
                gr = ((rgb >> 8) & 0xff) / 255f,
                b = (rgb & 0xff) / 255f,
                a = Math.min(1, alpha);
        var mat = g.pose().last().pose();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        var tess = Tesselator.getInstance();
        var buf = tess.getBuilder();
        buf.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        buf.vertex(mat, cx, cy, 0).color(r, gr, b, a).endVertex();
        int n = 48;
        for (int i = 0; i <= n; i++) {
            double ang = i * Math.PI * 2 / n;
            buf.vertex(mat, cx + (float) Math.cos(ang) * rx, cy + (float) Math.sin(ang) * ry, 0)
                    .color(r, gr, b, 0)
                    .endVertex();
        }
        tess.end();
    }

    /** Small filled circle via scanlines, used for star glow where squares would show. */
    private static void disc(GuiGraphics g, int cx, int cy, int radius, int rgb, float alpha) {
        int color = argb(alpha, rgb);
        for (int dy = -radius; dy <= radius; dy++) {
            int half = (int) Math.sqrt(radius * radius - dy * dy);
            g.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
        }
    }

    private static void line(GuiGraphics g, int x, int y, int ex, int ey, int color) {
        int n = Math.max(Math.abs(ex - x), Math.abs(ey - y));
        for (int i = 0; i <= n; i++) {
            int lx = x + (ex - x) * i / Math.max(1, n), ly = y + (ey - y) * i / Math.max(1, n);
            g.fill(lx, ly, lx + 1, ly + 1, color);
        }
    }

    private static void rebuild(int w, int h) {
        stars.clear();
        random.setSeed(0x57A1L);
        int count = Math.min(900, Math.max(60, w * h / 8000));
        for (int i = 0; i < count; i++) {
            var s = new Star();
            s.x = random.nextFloat() * w;
            s.y = random.nextFloat() * h;
            s.size = (float) Math.pow(random.nextFloat(), 1.7) * 1.6f + 0.45f;
            if (random.nextFloat() < 0.02f) s.size += 0.9f;
            s.opacity = random.nextFloat() * 0.75f + 0.25f;
            s.speed = random.nextFloat() * 0.02f + 0.005f;
            s.phase = random.nextFloat() * ((float) Math.PI * 2);
            s.drift = (random.nextFloat() * 0.016f + 0.002f) * (s.size > 1.4f ? 1.5f : 1);
            float r = random.nextFloat();
            s.rgb = r < 0.74f ? 0xe8eaf6 : r < 0.86f ? 0x4fc3f7 : r < 0.94f ? 0x80deea : 0xffd54f;
            stars.add(s);
        }
    }

    private static void spawnMeteor(int w, int h) {
        var m = new Meteor();
        boolean fromLeft = random.nextBoolean();
        float angle = (random.nextFloat() * 0.5f + 0.35f) * (float) Math.PI;
        float speed = 6 + random.nextFloat() * 7;
        m.x = fromLeft ? -60 : w + 60;
        m.y = random.nextFloat() * h * 0.55f;
        m.vx = (fromLeft ? 1 : -1) * (float) Math.cos(angle * 0.6f) * speed;
        m.vy = Math.abs((float) Math.sin(angle * 0.6f)) * speed;
        m.decay = 0.008f + random.nextFloat() * 0.008f;
        m.len = 90 + random.nextFloat() * 130;
        meteors.add(m);
    }

    public static void render(GuiGraphics g, int width, int height, int mx, int my) {
        double gs = Math.max(1, Minecraft.getInstance().getWindow().getGuiScale());
        g.pose().pushPose();
        g.pose().scale((float) (1 / gs), (float) (1 / gs), 1);
        renderPhysical(
                g,
                (int) Math.round(width * gs),
                (int) Math.round(height * gs),
                (int) Math.round(mx * gs),
                (int) Math.round(my * gs));
        g.pose().popPose();
    }

    private static void renderPhysical(GuiGraphics g, int width, int height, int mx, int my) {
        long now = net.minecraft.Util.getMillis();
        float t = now / 1000f;
        float frame = lastFrame == 0 ? 1 : Math.min(4, (now - lastFrame) / 16.7f);
        lastFrame = now;
        if (width != lastW || height != lastH) {
            rebuild(width, height);
            lastW = width;
            lastH = height;
            px = mx;
            py = my;
            meteors.clear();
            nextMeteorAt = now + 2500 + random.nextInt(5000);
        }

        // Deep space base
        g.fill(0, 0, width, height, 0xff070b14);

        // Nebula blobs, breathing over 26s
        float breathe = 0.5f - 0.5f * (float) Math.cos(t * Math.PI * 2 / 26);
        float nebOpacity = 0.72f + 0.28f * breathe, nebScale = 1 + 0.06f * breathe;
        radial(g, width * .18f, height * .12f, width * .30f * nebScale, height * .225f * nebScale,
                0x4fc3f7, .16f * nebOpacity);
        radial(g, width * .84f, height * .22f, width * .275f * nebScale, height * .25f * nebScale,
                0xb388ff, .15f * nebOpacity);
        radial(g, width * .70f, height * .88f, width * .35f * nebScale, height * .275f * nebScale,
                0x80deea, .10f * nebOpacity);
        radial(g, width * .08f, height * .82f, width * .225f * nebScale, height * .20f * nebScale,
                0xffd54f, .06f * nebOpacity);
        // Diagonal darkening toward the lower right
        radial(g, width * 1.05f, height * 1.08f, width * .6f, height * .55f, 0x070b14, .55f);

        // Milky-way band
        g.pose().pushPose();
        g.pose().translate(width * 0.5f, height * 0.45f, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(-24f));
        int half = (int) (width * 0.9f), bandH = (int) (height * 0.30f);
        g.fillGradient(-half, -bandH, half, 0, 0x00b388ff, 0x0eb388ff);
        g.fillGradient(-half, 0, half, bandH, 0x104fc3f7, 0x004fc3f7);
        g.fillGradient(-half, -bandH / 6, half, bandH / 6, 0x0ce8eaf6, 0x0ce8eaf6);
        g.pose().popPose();

        // Stars: twinkle, mouse parallax, slow downward drift
        px += (mx - px) * 0.06f * frame;
        py += (my - py) * 0.06f * frame;
        float dx = px - width / 2f, dy = py - height / 2f;
        for (var s : stars) {
            float op = s.opacity * (0.6f + (float) Math.sin(t * s.speed * 100 + s.phase) * 0.4f);
            if (op <= 0.02f) continue;
            s.y += s.drift * frame;
            if (s.y > height + 3) {
                s.y = -3;
                s.x = random.nextFloat() * width;
            }
            float x = s.x + dx * 0.012f * s.size, y = s.y + dy * 0.012f * s.size;
            int ix = (int) x, iy = (int) y;
            if (s.size > 1.15f) {
                int gr = Math.max(2, (int) (s.size * 2.2f));
                float prev = 0;
                for (int layer = 3; layer >= 1; layer--) {
                    float f = layer / 3f, w = (1 - f) * (1 - f);
                    disc(g, ix, iy, Math.max(1, (int) (gr * f)), s.rgb, op * 0.25f * (w - prev));
                    prev = w;
                }
            }
            int r = s.size < 0.9f ? 0 : 1;
            g.fill(ix - r, iy - r, ix + r + 1, iy + r + 1, argb(op, s.rgb));
            if (s.size > 1.75f) {
                int fl = (int) (s.size * 4), c = argb(op * 0.18f, s.rgb);
                g.fill(ix - fl, iy, ix + fl + 1, iy + 1, c);
                g.fill(ix, iy - fl, ix + 1, iy + fl + 1, c);
            }
        }

        // Shooting stars
        if (now > nextMeteorAt) {
            spawnMeteor(width, height);
            nextMeteorAt = now + 4000 + random.nextInt(9000);
        }
        for (int i = meteors.size() - 1; i >= 0; i--) {
            var m = meteors.get(i);
            m.x += m.vx * frame;
            m.y += m.vy * frame;
            m.life -= m.decay * frame;
            if (m.life <= 0 || m.y > height + 80 || m.x < -200 || m.x > width + 200) {
                meteors.remove(i);
                continue;
            }
            float mag = (float) Math.sqrt(m.vx * m.vx + m.vy * m.vy);
            float tx = m.x - m.vx / mag * m.len, ty = m.y - m.vy / mag * m.len;
            for (int seg = 0; seg < 8; seg++) {
                float f0 = seg / 8f, f1 = (seg + 1) / 8f;
                line(
                        g,
                        (int) (m.x + (tx - m.x) * f0),
                        (int) (m.y + (ty - m.y) * f0),
                        (int) (m.x + (tx - m.x) * f1),
                        (int) (m.y + (ty - m.y) * f1),
                        argb(0.9f * (1 - f0) * m.life, f0 < 0.35f ? 0xe8eaf6 : 0x80deea));
            }
            radial(g, m.x, m.y, 8, 8, 0xffffff, 0.6f * m.life);
        }

        // Vignette: layered edge strips, darkest at the corners
        int depth = Math.min(width, height) / 3;
        for (int i = 0; i < 48; i++) {
            float f = (i + 1) / 48f;
            int color = argb(0.75f * (1 - f) * (1 - f), 0x03050a);
            int o = depth * i / 48, n = depth * (i + 1) / 48;
            g.fill(0, o, width, n, color);
            g.fill(0, height - n, width, height - o, color);
            g.fill(o, 0, n, height, color);
            g.fill(width - n, 0, width - o, height, color);
        }
    }
}

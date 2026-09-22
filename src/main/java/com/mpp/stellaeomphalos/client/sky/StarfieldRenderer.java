package com.mpp.stellaeomphalos.client.sky;

import com.mpp.stellaeomphalos.OmphalosConfig;
import com.mpp.stellaeomphalos.client.codex.ClientKnowledgeCache;
import com.mpp.stellaeomphalos.client.render.OmphalosRenderTypes.Kind;
import com.mpp.stellaeomphalos.client.render.util.WorldDraw;
import com.mpp.stellaeomphalos.client.sign.SignSkyMirror;

import net.minecraft.client.Minecraft;

/** Default overlay preserves vanilla sun, moon, fog and cloud rendering. */
public final class StarfieldRenderer {
    private static StarfieldModel model;
    private static long seed;
    private static int layers;
    private static com.mpp.stellaeomphalos.constellation.sign.Sign[] signs =
            new com.mpp.stellaeomphalos.constellation.sign.Sign[0];
    private static com.mpp.stellaeomphalos.constellation.sign.SignSkyAnchor[] anchors =
            new com.mpp.stellaeomphalos.constellation.sign.SignSkyAnchor[0];

    private StarfieldRenderer() {}

    public static void tick() {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        var current = SignSkyMirror.skySeed(mc.level.dimension());
        if (current.isEmpty()) {
            SignSkyMirror.requestSeed(mc.level.dimension());
            return;
        }
        int nextLayers = OmphalosConfig.CLIENT.integer("sky.starLayers");
        if (model == null || seed != current.getAsLong() || layers != nextLayers) {
            seed = current.getAsLong();
            layers = nextLayers;
            model = new StarfieldModel(seed, layers);
        }
        var list = new java.util.ArrayList<com.mpp.stellaeomphalos.constellation.sign.Sign>();
        var locations =
                new java.util.ArrayList<com.mpp.stellaeomphalos.constellation.sign.SignSkyAnchor>();
        var layout = SignSkyMirror.layout(mc.level.dimension());
        for (var active : SignSkyMirror.activeSigns(mc.level.dimension())) {
            var sign=com.mpp.stellaeomphalos.client.sign.SignDefinitionMirror.byNumber(active.numericId());
            if(sign!=null&&ClientKnowledgeCache.record().knownSigns().contains(sign.id())&&layout.containsKey(sign.id())) {
                list.add(sign);locations.add(layout.get(sign.id()));
            }
        }
        signs = list.toArray(com.mpp.stellaeomphalos.constellation.sign.Sign[]::new);
        anchors =
                locations.toArray(com.mpp.stellaeomphalos.constellation.sign.SignSkyAnchor[]::new);
    }

    public static void render(WorldDraw d) {
        var mc = Minecraft.getInstance();
        if (mc.level == null
                || !OmphalosConfig.CLIENT.flag("render.skySignOverlay")
                || OmphalosConfig.CLIENT.snapshot().get("sky.overlay")
                        == OmphalosConfig.SkyMode.NONE) return;
        float brightness =
                mc.level.getStarBrightness(d.partial) * (1 - mc.level.getRainLevel(d.partial));
        if (brightness <= .01F) return;
        var v = d.buffer(Kind.SKY_ADDITIVE);
        boolean foreign =
                OmphalosConfig.CLIENT.flag("sky.respectForeign")
                        && (!mc.level
                                                .dimensionType()
                                                .effectsLocation()
                                                .getNamespace()
                                                .equals("minecraft")
                                        && !mc.level
                                                .dimensionType()
                                                .effectsLocation()
                                                .getNamespace()
                                                .equals("stellaeomphalos")
                                || com.mpp.stellaeomphalos.client.render.ShaderCompat
                                        .shadersInUse());
        if (model != null && !foreign)
            for (int i = 0; i < model.points.length; i += 3) {
                int alpha =
                        (int)
                                (brightness
                                        * (100
                                                + 70
                                                        * Math.sin(
                                                                d.millis
                                                                        / (700.0
                                                                                + (i / 300)
                                                                                        * 190))));
                double x = model.points[i], y = model.points[i + 1], z = model.points[i + 2];
                d.quad(v, x, y, z, .08, 0, 0, 0, .08, 0, (alpha << 24) | 0xb8caff);
            }
        for (int i = 0; i < signs.length; i++) {
            var sign = signs[i];
            var a = anchors[i];
            int color = ((int) (brightness * 220) << 24) | (sign.renderColor() & 0xffffff);
            for (int j = 0; j < sign.stars().size(); j++) {
                var p = sign.stars().get(j);
                double x = a.baseX() + a.incUx() * p.x() + a.incVx() * p.y(),
                        y = a.baseY() + a.incUy() * p.x() + a.incVy() * p.y(),
                        z = a.baseZ() + a.incUz() * p.x() + a.incVz() * p.y();
                d.quad(v, x, y, z, .18, 0, 0, 0, .18, 0, color);
            }
            for (int j = 0; j < sign.lines().size(); j++) {
                var line = sign.lines().get(j);
                var p = line.a();
                var q = line.b();
                d.beam(
                        v,
                        a.baseX() + a.incUx() * p.x() + a.incVx() * p.y(),
                        a.baseY() + a.incUy() * p.x() + a.incVy() * p.y(),
                        a.baseZ() + a.incUz() * p.x() + a.incVz() * p.y(),
                        a.baseX() + a.incUx() * q.x() + a.incVx() * q.y(),
                        a.baseY() + a.incUy() * q.x() + a.incVy() * q.y(),
                        a.baseZ() + a.incUz() * q.x() + a.incVz() * q.y(),
                        .035,
                        color);
            }
        }
    }

    public static void clear() {
        model = null;
        signs = new com.mpp.stellaeomphalos.constellation.sign.Sign[0];
        anchors = new com.mpp.stellaeomphalos.constellation.sign.SignSkyAnchor[0];
    }
}

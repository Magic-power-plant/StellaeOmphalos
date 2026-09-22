package com.mpp.stellaeomphalos.client.render;

import com.mpp.stellaeomphalos.client.render.util.WorldDraw;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

/** Cached baked quad geometry; tint and alpha are vertex data, no global color state. */
public final class PhantomBatch {
    private static final java.util.Map<
                    BlockState, java.util.List<net.minecraft.client.renderer.block.model.BakedQuad>>
            CACHE = new java.util.HashMap<>();
    private static final RandomSource RANDOM = RandomSource.create(42);
    private static final Direction[] DIRECTIONS = Direction.values();

    private static int epoch;

    public static int epoch() {
        return epoch;
    }

    private PhantomBatch() {}

    public static void prepare(BlockState state) {
        if (CACHE.containsKey(state) || CACHE.size() >= 512) return;
        var quads = new java.util.ArrayList<net.minecraft.client.renderer.block.model.BakedQuad>();
        var model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
        RANDOM.setSeed(42);
        quads.addAll(model.getQuads(state, null, RANDOM));
        for (var side : DIRECTIONS) {
            RANDOM.setSeed(42);
            quads.addAll(model.getQuads(state, side, RANDOM));
        }
        CACHE.put(state, java.util.List.copyOf(quads));
    }

    public static void draw(BlockState state, WorldDraw d, int color) {
        var quads = CACHE.get(state);
        if (quads == null) return;
        var v = d.atlasBuffer();
        for (int q = 0; q < quads.size(); q++) {
            int[] data = quads.get(q).getVertices();
            int stride = data.length / 4;
            for (int i = 0; i < 4; i++) {
                int p = i * stride;
                d.vertex(
                        v,
                        Float.intBitsToFloat(data[p]) - .5,
                        Float.intBitsToFloat(data[p + 1]),
                        Float.intBitsToFloat(data[p + 2]) - .5,
                        color,
                        Float.intBitsToFloat(data[p + 4]),
                        Float.intBitsToFloat(data[p + 5]));
            }
        }
    }

    public static void clear() {
        CACHE.clear();
        epoch++;
    }
}

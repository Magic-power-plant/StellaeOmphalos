package com.mpp.stellaeomphalos.client.render;

import com.mpp.stellaeomphalos.client.render.ber.OmphalosBlockEntityRenderer;
import com.mpp.stellaeomphalos.client.render.util.WorldDraw;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

/** Fixed storage, deduplicated by entity identity. Entries cannot survive a frame. */
public final class DeferredEffectQueue {
    private static final class Entry {
        OmphalosBlockEntityRenderer renderer;
        BlockEntity be;
        int light, overlay;
        float partial;
    }

    private static final Entry[] ENTRIES = new Entry[256];
    private static int count, lastFlushed;
    private static Frustum frustum;

    static {
        for (int i = 0; i < ENTRIES.length; i++) ENTRIES[i] = new Entry();
    }

    private DeferredEffectQueue() {}

    public static void begin(Frustum next) {
        clear();
        frustum = next;
    }

    public static boolean visible(AABB box) {
        return frustum == null || frustum.isVisible(box);
    }

    public static void enqueue(
            OmphalosBlockEntityRenderer<?> renderer,
            BlockEntity be,
            float partial,
            int light,
            int overlay) {
        for (int i = 0; i < count; i++) if (ENTRIES[i].be == be) return;
        if (count == ENTRIES.length) return;
        var e = ENTRIES[count++];
        e.renderer = renderer;
        e.be = be;
        e.partial = partial;
        e.light = light;
        e.overlay = overlay;
    }

    @SuppressWarnings("unchecked")
    public static void flush(WorldDraw d) {
        lastFlushed = count;
        try {
            for (int i = 0; i < count; i++) {
                var e = ENTRIES[i];
                if (!e.be.isRemoved()
                        && e.be.getLevel() == net.minecraft.client.Minecraft.getInstance().level) {
                    d.partial = e.partial;
                    e.renderer.draw(e.be, d, e.light, e.overlay);
                }
            }
        } finally {
            clear();
        }
    }

    public static void clear() {
        for (int i = 0; i < count; i++) {
            ENTRIES[i].be = null;
            ENTRIES[i].renderer = null;
        }
        count = 0;
        frustum = null;
    }

    public static int lastFlushed() {
        return lastFlushed;
    }

    public static int size() {
        return count;
    }
}

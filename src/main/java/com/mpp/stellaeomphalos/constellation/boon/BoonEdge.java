package com.mpp.stellaeomphalos.constellation.boon;

import net.minecraft.resources.ResourceLocation;

/** Undirected render edge between two node ids; the canonical constructor normalizes endpoint order. */
public record BoonEdge(ResourceLocation a, ResourceLocation b) {
    public BoonEdge {
        if (a == null || b == null) throw new IllegalArgumentException("null edge endpoint");
        if (a.equals(b)) throw new IllegalArgumentException("Self connection at " + a);
        if (a.toString().compareTo(b.toString()) > 0) {
            var swap = a;
            a = b;
            b = swap;
        }
    }

    public boolean touches(ResourceLocation id) {
        return a.equals(id) || b.equals(id);
    }

    public ResourceLocation opposite(ResourceLocation id) {
        if (a.equals(id)) return b;
        if (b.equals(id)) return a;
        throw new IllegalArgumentException(id + " is not part of " + this);
    }
}

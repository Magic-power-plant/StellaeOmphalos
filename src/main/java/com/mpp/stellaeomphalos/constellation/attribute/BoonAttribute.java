package com.mpp.stellaeomphalos.constellation.attribute;

import net.minecraft.resources.ResourceLocation;

public class BoonAttribute {

    private final ResourceLocation id;
    private final double defaultValue;
    private final BoonAttributeClamp clamp;
    private final boolean onlyMultiplicative;

    public BoonAttribute(ResourceLocation id, double defaultValue, BoonAttributeClamp clamp, boolean onlyMultiplicative) {
        if (id == null) throw new IllegalArgumentException("null id");
        if (!Double.isFinite(defaultValue)) throw new IllegalArgumentException("non-finite default for " + id);
        this.id = id;
        this.defaultValue = defaultValue;
        this.clamp = clamp == null ? BoonAttributeClamp.UNBOUNDED : clamp;
        this.onlyMultiplicative = onlyMultiplicative;
    }

    public ResourceLocation id() {
        return id;
    }

    public double defaultValue() {
        return defaultValue;
    }

    public BoonAttributeClamp clamp() {
        return clamp;
    }

    public boolean isOnlyMultiplicative() {
        return onlyMultiplicative;
    }
}

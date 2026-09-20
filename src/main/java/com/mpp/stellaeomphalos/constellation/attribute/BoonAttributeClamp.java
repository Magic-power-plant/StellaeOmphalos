package com.mpp.stellaeomphalos.constellation.attribute;

public final class BoonAttributeClamp {

    public static final BoonAttributeClamp UNBOUNDED = new BoonAttributeClamp(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

    private final double lower;
    private final double upper;

    public BoonAttributeClamp(double lower, double upper) {
        if (!(lower <= upper)) throw new IllegalArgumentException("lower > upper: " + lower + " > " + upper);
        this.lower = lower;
        this.upper = upper;
    }

    public static BoonAttributeClamp of(double lower, double upper) {
        return new BoonAttributeClamp(lower, upper);
    }

    public double clamp(double value) {
        return Math.min(Math.max(value, lower), upper);
    }

    public double lower() {
        return lower;
    }

    public double max() {
        return upper;
    }

    public boolean bounded() {
        return lower > Double.NEGATIVE_INFINITY || upper < Double.POSITIVE_INFINITY;
    }
}

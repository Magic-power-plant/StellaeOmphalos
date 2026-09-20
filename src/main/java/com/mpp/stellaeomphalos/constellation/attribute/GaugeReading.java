package com.mpp.stellaeomphalos.constellation.attribute;

public record GaugeReading(String nameKey, double value, String suffix, Breakdown breakdown) {
    public record Breakdown(
            double baseline,
            double addition,
            double addedMultiply,
            double stackingMultiply,
            double computed,
            String note) {}

    public static GaugeReading evaluate(
            String key,
            double baseline,
            double addition,
            double addedMultiply,
            double stackingMultiply,
            double finalValue,
            String unit) {
        double computed = (baseline + addition) * (1 + addedMultiply) * stackingMultiply;
        String note =
                Math.abs(computed - finalValue) >= 1e-4
                        ? "stellaeomphalos.codex.gauge_adjusted"
                        : "";
        return new GaugeReading(
                key,
                finalValue,
                unit,
                new Breakdown(baseline, addition, addedMultiply, stackingMultiply, computed, note));
    }
}

package com.mpp.stellaeomphalos.lumen.transport;

import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/** World-independent pure-data light source; the only piece persisted in the network save. */
public record LumenSourceData(
        ResourceLocation providerId,
        long baseOutput,
        Optional<ResourceLocation> sign,
        boolean autoLink,
        boolean seesSky,
        boolean enhanced,
        double proximityFactor,
        double noiseFactor) {
    public LumenSourceData {
        java.util.Objects.requireNonNull(providerId);
        java.util.Objects.requireNonNull(sign);
        if (baseOutput < 0) throw new IllegalArgumentException("Negative base output");
    }
    public LumenSourceData withSeesSky(boolean value) {
        return new LumenSourceData(providerId, baseOutput, sign, autoLink, value, enhanced, proximityFactor, noiseFactor);
    }
    public LumenSourceData withProximity(double value) {
        return new LumenSourceData(providerId, baseOutput, sign, autoLink, seesSky, enhanced, value, noiseFactor);
    }
    public LumenSourceData withNoise(double value) {
        return new LumenSourceData(providerId, baseOutput, sign, autoLink, seesSky, enhanced, proximityFactor, value);
    }
    public LumenSourceData withSign(Optional<ResourceLocation> value) {
        return new LumenSourceData(providerId, baseOutput, value, autoLink, seesSky, enhanced, proximityFactor, noiseFactor);
    }
}

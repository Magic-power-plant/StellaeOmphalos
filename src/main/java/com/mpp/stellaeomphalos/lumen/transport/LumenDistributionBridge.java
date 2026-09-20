package com.mpp.stellaeomphalos.lumen.transport;

import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * Cross-layer bridge: the constellation module (higher layer) registers the authoritative sign
 * distribution provider at assembly time. Until then, signed sources produce nothing (0) while
 * unsigned sources use the neutral channel at full distribution (1).
 */
public final class LumenDistributionBridge {
    @FunctionalInterface
    public interface Provider {
        /** @return sign distribution in [0,1]; 0 means the sign is not in effect. */
        double distribution(Level level, ResourceLocation sign);
    }
    private static final Provider NONE = (level, sign) -> 0;
    private static volatile Provider provider = NONE;
    private LumenDistributionBridge() {}

    public static void registerProvider(Provider replacement) {
        provider = java.util.Objects.requireNonNull(replacement);
    }
    public static double distribution(Level level, ResourceLocation sign) {
        return provider.distribution(level, sign);
    }
    /** Neutral distribution for sources without an owning sign. */
    public static double neutral() { return 1; }
}

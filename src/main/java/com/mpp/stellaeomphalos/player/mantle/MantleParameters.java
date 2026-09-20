package com.mpp.stellaeomphalos.player.mantle;

import com.mpp.stellaeomphalos.OmphalosConfig;

public record MantleParameters(
        int rechargeTicks,
        int maxStacks,
        double healing,
        double reduction,
        double retortScale,
        int retortTicks,
        int cooldown,
        double chance) {
    public static MantleParameters current() {
        return new MantleParameters(
                OmphalosConfig.SERVER.integer("mantle.rechargeTicks"),
                OmphalosConfig.SERVER.integer("mantle.maxStacks"),
                OmphalosConfig.SERVER.decimal("mantle.healing"),
                OmphalosConfig.SERVER.decimal("mantle.fireReduction"),
                OmphalosConfig.SERVER.decimal("mantle.retortScale"),
                OmphalosConfig.SERVER.integer("mantle.retortTicks"),
                OmphalosConfig.SERVER.integer("mantle.stasisCooldown"),
                OmphalosConfig.SERVER.decimal("mantle.activationChance"));
    }
}

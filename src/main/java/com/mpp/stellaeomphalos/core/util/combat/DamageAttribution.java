package com.mpp.stellaeomphalos.core.util.combat;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

public final class DamageAttribution {
    private DamageAttribution() {}
    public static DamageSource withOwner(DamageSource source, Entity direct, Entity owner) {
        return source.getClass() == DamageSource.class ? new DamageSource(source.typeHolder(), direct, owner) : source;
    }
}

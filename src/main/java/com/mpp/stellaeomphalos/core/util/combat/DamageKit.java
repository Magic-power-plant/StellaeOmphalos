package com.mpp.stellaeomphalos.core.util.combat;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

public final class DamageKit {
    private DamageKit() {}
    public static boolean apply(Entity target, DamageSource source, float amount) {
        if (!Float.isFinite(amount) || amount < 0) throw new IllegalArgumentException("Invalid damage");
        return !target.level().isClientSide && amount > 0 && target.hurt(source, amount);
    }
}

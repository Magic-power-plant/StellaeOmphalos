package com.mpp.stellaeomphalos.constellation.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/** Corrupted verdance inflicts bounded periodic damage while its finite status is active. */
public final class BleedingEffect extends MobEffect {
    public BleedingEffect() { super(MobEffectCategory.HARMFUL, 0x992B3C); }
    @Override public boolean isDurationEffectTick(int duration, int amplifier) { return duration % 20 == 0; }
    @Override public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide) entity.hurt(entity.damageSources().magic(), 1 + Math.min(3, Math.max(0, amplifier)));
    }
}

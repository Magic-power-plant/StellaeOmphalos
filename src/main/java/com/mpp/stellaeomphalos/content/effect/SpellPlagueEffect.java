package com.mpp.stellaeomphalos.content.effect;

import com.mpp.stellaeomphalos.core.util.combat.DamageKit;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * 法术瘟疫（id {@code spell_plague}）：每 40 tick 造成小额魔法伤害，并附加一段短时虚弱。
 * 施加的中毒/虚弱等级被硬性钳制在放大等级 3 以内，避免叠加到无法反制的程度。
 */
public final class SpellPlagueEffect extends CustomIconEffect {

    /** 虚弱等级上限（放大等级语义，0 基）。 */
    private static final int MAX_WEAKNESS_AMPLIFIER = 3;

    /** 每次结算的魔法伤害。 */
    private static final float PLAGUE_DAMAGE = 1.0F;

    /** 虚弱持续时间（tick）。 */
    private static final int WEAKNESS_DURATION = 60;

    public SpellPlagueEffect() {
        super(MobEffectCategory.HARMFUL, 0x6B4FA8);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 40 == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) return;
        DamageKit.apply(entity, entity.damageSources().magic(), PLAGUE_DAMAGE);
        int weakness = Math.min(MAX_WEAKNESS_AMPLIFIER, Math.max(0, amplifier));
        entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, WEAKNESS_DURATION, weakness));
    }
}

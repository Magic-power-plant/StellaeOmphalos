package com.mpp.stellaeomphalos.content.effect;

import com.mpp.stellaeomphalos.core.util.combat.DamageKit;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * 流血（id {@code bleed}）：真正的周期性伤害，每 20 tick 造成 {@code 1 + amplifier} 点伤害。
 *
 * <p>伤害经 {@link DamageKit} 施加，并使用专属伤害类型 {@link #BLEED_DAMAGE}。构造
 * {@link DamageSource} 时直接实体与真源都传 {@code null}，因此伤害**不会归因到受害者自身**，
 * 也不会为流血者累积击杀/仇恨；死亡信息走 {@code stellaeomphalos.bleed} 文案。
 *
 * <p>数据驱动 JSON：{@code data/stellaeomphalos/damage_type/bleed.json}。
 */
public final class BleedEffect extends CustomIconEffect {

    /** 流血专属伤害类型键。 */
    public static final ResourceKey<DamageType> BLEED_DAMAGE =
            ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("stellaeomphalos", "bleed"));

    /** 单次结算的伤害上限，避免高放大等级瞬间抽干生命。 */
    private static final int MAX_DAMAGE = 4;

    public BleedEffect() {
        super(MobEffectCategory.HARMFUL, 0x8A1F2B);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) return;
        var holder = entity.level()
                .registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(BLEED_DAMAGE);
        DamageSource source = new DamageSource(holder, null, null);
        DamageKit.apply(entity, source, 1.0F + Math.min(MAX_DAMAGE, Math.max(0, amplifier)));
    }
}

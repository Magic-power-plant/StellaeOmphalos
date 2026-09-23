package com.mpp.stellaeomphalos.content.effect;

import net.minecraft.world.effect.MobEffectCategory;

/**
 * 时间冻结（id {@code time_freeze}）：效果本体只登记类别与颜色，定身逻辑由
 * {@link ContentEffects} 的 {@code LivingEvent.LivingTickEvent} 监听器执行。
 *
 * <p>规则：普通生物立即定身（每 tick 清空位移向量，因此无法跳跃/移动）；**玩家需要放大等级
 * ≥ {@link ContentEffects#PLAYER_FREEZE_AMPLIFIER} 才会被冻结**。
 */
public final class TimeFreezeEffect extends CustomIconEffect {

    public TimeFreezeEffect() {
        super(MobEffectCategory.HARMFUL, 0x9FD8F5);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }
}

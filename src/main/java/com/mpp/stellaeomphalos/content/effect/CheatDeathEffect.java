package com.mpp.stellaeomphalos.content.effect;

import net.minecraft.world.effect.MobEffectCategory;

/**
 * 免死效果本体（id {@code cheat_death}）。效果自身不承载逻辑：拦截死亡的监听器由
 * {@link PartSixEffects} 注册，见 {@code PartSixEffects#onLethalDamage} / {@code PartSixEffects#onDeath}。
 * 放大等级越高，触发后保留的生命越多（见 {@code PartSixEffects#tryCheatDeath}）。
 */
public final class CheatDeathEffect extends CustomIconEffect {

    public CheatDeathEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xF4D06F);
    }

    /** 效果为瞬发式：不对应周期性 tick 行为，行为全部在事件监听器中。 */
    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }
}

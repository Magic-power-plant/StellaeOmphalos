package com.mpp.stellaeomphalos.constellation.starmap;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Rare imprint bonus: one-shot death protection. The shell is intentionally empty (no icon
 * renderer on the server; client icon placeholder deferred to 《客户端渲染界面与音效》 resources). The actual
 * cancel-and-consume behavior lives in StarmapBootstrap's LivingDeathEvent listener.
 */
public final class CheatDeathEffect extends MobEffect {
    public CheatDeathEffect() { super(MobEffectCategory.BENEFICIAL, 0xF4D06F); }
}

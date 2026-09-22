package com.mpp.stellaeomphalos.content.enchantment;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

/**
 * 夜视附魔（id {@code night_vision}）：戴在头上时持续维持夜视效果。
 * 剩余时长低于阈值才补足，避免每 tick 反复写入导致效果实例抖动与客户端同步刷屏。
 */
public final class NightVisionEnchantment extends Enchantment implements EquippedTickEnchantment {

    /** 每次补足的目标时长（tick）。 */
    private static final int REFRESH_DURATION = 300;

    /** 低于该剩余时长才补足（tick）。 */
    private static final int REFRESH_THRESHOLD = 200;

    public NightVisionEnchantment() {
        super(Enchantment.Rarity.RARE, EnchantmentCategory.ARMOR_HEAD,
                new EquipmentSlot[] {EquipmentSlot.HEAD});
    }

    @Override
    public void onEquippedTick(ServerPlayer player, int level) {
        if (player.level().isClientSide) return;
        MobEffectInstance current = player.getEffect(MobEffects.NIGHT_VISION);
        if (current != null && current.getDuration() > REFRESH_THRESHOLD) return;
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, REFRESH_DURATION, 0, true, false));
    }

    @Override
    public int getMinLevel() {
        return 1;
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }
}

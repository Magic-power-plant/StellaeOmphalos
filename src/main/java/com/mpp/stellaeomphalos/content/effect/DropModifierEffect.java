package com.mpp.stellaeomphalos.content.effect;

import java.util.ArrayList;
import java.util.Collection;
import net.minecraft.core.Direction;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * 掉落修正（id {@code drop_modifier}）：效果自身不产生 tick 行为，只把放大等级当作
 * "幸运类修正值"暴露给其他系统（星象效果体系）。
 *
 * <p>提供两处消费点：{@link #dropBonus(LivingEntity)} 供其它系统直接查询；
 * {@link #applyDropBonus(Collection, int)} 由 {@link ContentEffects} 的
 * {@code LivingDropsEvent} 监听器调用，在击杀者携带该效果时，为每一件可堆叠掉落额外复制
 * {@code +amplifier} 份（独立 {@link ItemEntity}，避免与原掉落合并而被回收）。
 */
public final class DropModifierEffect extends CustomIconEffect {

    /** 单次击杀允许的最大额外掉落份数，防止放大器被滥用时刷爆区块。 */
    private static final int MAX_BONUS = 5;

    public DropModifierEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x6FA8DC);
    }

    /**
     * 读取实体身上的掉落修正值（额外掉落份数）。
     *
     * @return 放大等级（0 基，已按 {@link #MAX_BONUS} 钳制）；未携带该效果时返回 0
     */
    public static int dropBonus(LivingEntity entity) {
        if (entity == null) return 0;
        MobEffectInstance instance = entity.getEffect(ContentEffects.DROP_MODIFIER.get());
        if (instance == null) return 0;
        return Math.min(MAX_BONUS, Math.max(0, instance.getAmplifier()));
    }

    /**
     * 把 {@code bonus} 份额外掉落追加进事件掉落集合。只处理可堆叠物品（最大堆叠数 &gt; 1），
     * 每份使用独立的 {@link ItemEntity} 并给予短暂拾取延迟，避免与本体合并。
     */
    static void applyDropBonus(Collection<ItemEntity> drops, int bonus) {
        if (bonus <= 0 || drops.isEmpty()) return;
        var extras = new ArrayList<ItemEntity>(Math.min(64, bonus * drops.size()));
        for (ItemEntity drop : drops) {
            ItemStack stack = drop.getItem();
            if (stack.isEmpty() || stack.getMaxStackSize() <= 1) continue;
            for (int i = 0; i < bonus; i++) extras.add(copyOf(drop, stack));
        }
        drops.addAll(extras);
    }

    private static ItemEntity copyOf(ItemEntity original, ItemStack stack) {
        var copy = new ItemEntity(
                original.level(), original.getX(), original.getY(), original.getZ(), stack.copy());
        copy.setDeltaMovement(Vec3.ZERO);
        copy.setPickUpDelay(20);
        Direction towards = Direction.getRandom(original.level().getRandom());
        copy.setPos(
                original.getX() + towards.getStepX() * 0.1D,
                original.getY() + 0.05D,
                original.getZ() + towards.getStepZ() * 0.1D);
        return copy;
    }
}

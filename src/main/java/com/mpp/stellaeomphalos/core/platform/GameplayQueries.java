package com.mpp.stellaeomphalos.core.platform;

import com.mpp.stellaeomphalos.OmphalosConfig;
import com.mpp.stellaeomphalos.core.bootstrap.LifecycleOrchestrator;
import com.mpp.stellaeomphalos.core.platform.api.EnchantmentQueryEvent;
import com.mpp.stellaeomphalos.core.platform.api.WaterMovementQueryEvent;
import java.util.Map;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.common.MinecraftForge;

/** Reentrant reads by extension listeners return the unmodified vanilla value. */
public final class GameplayQueries {
    private static final ThreadLocal<Boolean> ENCHANTMENT_QUERY = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> WATER_QUERY = ThreadLocal.withInitial(() -> false);
    /** 当前正在查询的物品栈（**原始引用**，不是副本）；供监听器解析"这是谁的物品"。 */
    private static final ThreadLocal<ItemStack> QUERIED_STACK = new ThreadLocal<>();
    private GameplayQueries() {}
    public static Map<Enchantment, Integer> enchantments(ItemStack stack, Map<Enchantment, Integer> base) {
        if (LifecycleOrchestrator.phase() != LifecycleOrchestrator.Phase.LOADED || stack.isEmpty()
                || ENCHANTMENT_QUERY.get() || !OmphalosConfig.COMMON.flag("compat.enchantmentAmplification")) return base;
        ENCHANTMENT_QUERY.set(true);
        var previous = QUERIED_STACK.get();
        QUERIED_STACK.set(stack);
        try {
            var event = new EnchantmentQueryEvent(stack, base); MinecraftForge.EVENT_BUS.post(event); return event.levels();
        } finally {
            if (previous == null) QUERIED_STACK.remove(); else QUERIED_STACK.set(previous);
            ENCHANTMENT_QUERY.remove();
        }
    }

    /**
     * @return 本次附魔查询的**原始**物品栈引用；不在查询中时为 null
     *
     * <p>监听器只能用它做**身份判定**（例如"这是哪个玩家手里的物品"）：原版附魔读取没有玩家参数，
     * 而 {@link EnchantmentQueryEvent} 携带的是防御性副本，无法建立身份。**禁止修改或长期持有**该引用。
     */
    public static ItemStack currentQueryStack() {
        return QUERIED_STACK.get();
    }
    public static float waterSlowdown(LivingEntity entity, float base) {
        if (LifecycleOrchestrator.phase() != LifecycleOrchestrator.Phase.LOADED || WATER_QUERY.get()) return base;
        WATER_QUERY.set(true);
        try {
            var event = new WaterMovementQueryEvent(entity, base); MinecraftForge.EVENT_BUS.post(event); return event.slowdown();
        } finally { WATER_QUERY.remove(); }
    }
}

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
    private GameplayQueries() {}
    public static Map<Enchantment, Integer> enchantments(ItemStack stack, Map<Enchantment, Integer> base) {
        if (LifecycleOrchestrator.phase() != LifecycleOrchestrator.Phase.LOADED || stack.isEmpty()
                || ENCHANTMENT_QUERY.get() || !OmphalosConfig.COMMON.flag("compat.enchantmentAmplification")) return base;
        ENCHANTMENT_QUERY.set(true);
        try {
            var event = new EnchantmentQueryEvent(stack, base); MinecraftForge.EVENT_BUS.post(event); return event.levels();
        } finally { ENCHANTMENT_QUERY.remove(); }
    }
    public static float waterSlowdown(LivingEntity entity, float base) {
        if (LifecycleOrchestrator.phase() != LifecycleOrchestrator.Phase.LOADED || WATER_QUERY.get()) return base;
        WATER_QUERY.set(true);
        try {
            var event = new WaterMovementQueryEvent(entity, base); MinecraftForge.EVENT_BUS.post(event); return event.slowdown();
        } finally { WATER_QUERY.remove(); }
    }
}

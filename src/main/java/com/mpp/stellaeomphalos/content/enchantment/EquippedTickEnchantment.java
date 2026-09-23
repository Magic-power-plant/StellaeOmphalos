package com.mpp.stellaeomphalos.content.enchantment;

import net.minecraft.server.level.ServerPlayer;

/**
 * 《方块物品实体完整清单》的"装备时每 tick 生效"标记接口（对应原模组的 {@code EnchantmentPlayerWornTick}）。
 *
 * <p>{@link EnchantmentContent} 在注册期把所有实现了本接口的附魔实例收集进列表，
 * 由 {@code ContentEffects} 注册的 {@code PlayerTickEvent.End} 监听器遍历玩家装备槽后逐个回调。
 */
public interface EquippedTickEnchantment {

    /**
     * 玩家每 tick 结束时的回调（仅服务端、仅当对应装备槽上的附魔等级 &gt; 0 时触发）。
     *
     * @param player 装备者
     * @param level  该附魔在对应装备上的实际等级（≥ 1）
     */
    void onEquippedTick(ServerPlayer player, int level);
}

package com.mpp.stellaeomphalos.content.item.amulet;

import com.mpp.stellaeomphalos.content.item.PartSixItems;
import com.mpp.stellaeomphalos.core.platform.api.EnchantmentQueryEvent;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 护符修正注入原版附魔查询的桥（Part-6 §6.3.5，对应已登记的 Mixin **M-1 / M-2**）。
 *
 * <p>1.12.2 的 ASM 三入口在 1.20.1 由 `EnchantmentHelperMixin` 接管：它对
 * `EnchantmentHelper#getItemEnchantmentLevel`（M-1）与 `#getEnchantments`（M-2）注入 `@At("RETURN")`，
 * 把结果交给 `GameplayQueries.enchantments(...)`，进而投递 {@link EnchantmentQueryEvent}。
 * 本类是该事件的消费端：把**当前上下文所有者**佩戴的护符修正叠加到查询结果上。
 *
 * <p>原版附魔读取没有玩家参数，因此需要在线程局部建立"当前所有者"上下文。
 * {@link #withOwner} 是唯一入口；工具提示路径已经使用它，需要更广覆盖的调用方
 * （攻击 / 挖掘链路）应在自身作用域内自行包夹。**没有上下文时不施加任何修正**，
 * 这是刻意的保守行为，避免把某个玩家的护符加成泄漏到其它玩家的查询上。
 */
public final class AmuletEnchantBridge {

    private static final ThreadLocal<List<AmuletModifier>> ACTIVE = new ThreadLocal<>();

    /**
     * 佩戴护符的玩家索引（弱引用，避免长时间持有玩家对象）。
     *
     * <p>只有佩戴护符的玩家会进入这里，因此身份反查的扫描规模恒为 0~1 个玩家，
     * 不必遍历 PlayerList，也不依赖调用方包夹上下文。
     */
    private static final java.util.Map<java.util.UUID, java.lang.ref.WeakReference<Player>> TRACKED =
            new java.util.concurrent.ConcurrentHashMap<>();

    private AmuletEnchantBridge() {}

    /** 由玩家 tick 维护：佩戴护符则登记，未佩戴或已移除则取消登记。 */
    public static void refreshTracking(Player player) {
        if (player == null || player.level().isClientSide) return;
        if (player.isRemoved() || equippedModifiers(player).isEmpty()) TRACKED.remove(player.getUUID());
        else TRACKED.put(player.getUUID(), new java.lang.ref.WeakReference<>(player));
    }

    /** 登记一个佩戴护符的玩家（供玩家 tick 与测试使用）。 */
    public static void track(Player player) {
        if (player == null) return;
        TRACKED.put(player.getUUID(), new java.lang.ref.WeakReference<>(player));
    }

    /** 玩家退网时清理索引。 */
    public static void untrack(java.util.UUID player) {
        if (player != null) TRACKED.remove(player);
    }

    /** 在给定玩家的上下文中执行一段代码：期间所有附魔查询都会叠加该玩家护符的修正。 */
    public static <T> T withOwner(Player player, java.util.function.Supplier<T> body) {
        var previous = ACTIVE.get();
        ACTIVE.set(equippedModifiers(player));
        try {
            return body.get();
        } finally {
            if (previous == null) ACTIVE.remove();
            else ACTIVE.set(previous);
        }
    }

    /** @return 当前上下文中的护符修正；无上下文时为空表 */
    public static List<AmuletModifier> activeModifiers() {
        var active = ACTIVE.get();
        return active == null ? List.of() : active;
    }

    /** 只读取玩家物品栏与副手中的第一个护符修正。 */
    public static List<AmuletModifier> equippedModifiers(Player player) {
        if (player == null) return List.of();
        for (var stack : candidateSlots(player))
            if (stack.is(PartSixItems.WARDED_AMULET.get())) {
                var modifiers = AmuletHolder.modifiers(stack);
                if (!modifiers.isEmpty()) return modifiers;
            }
        return List.of();
    }

    private static List<ItemStack> candidateSlots(Player player) {
        var slots = new ArrayList<ItemStack>(player.getInventory().getContainerSize() + 1);
        for (int i = 0; i < player.getInventory().getContainerSize(); i++)
            slots.add(player.getInventory().getItem(i));
        slots.add(player.getOffhandItem());
        return slots;
    }

    /** 事件入口：把上下文修正叠加到查询结果。 */
    @SubscribeEvent
    public static void onEnchantmentQuery(EnchantmentQueryEvent event) {
        var modifiers = resolveModifiers();
        if (modifiers.isEmpty()) return;
        var base = new LinkedHashMap<ResourceLocation, Integer>();
        event.levels()
                .forEach(
                        (enchantment, level) -> {
                            var id = enchantmentId(enchantment);
                            if (id != null) base.put(id, level);
                        });
        var adjusted = AmuletRoller.applyAll(modifiers, base);
        for (var entry : adjusted.entrySet()) {
            if (entry.getValue() <= 0) continue;
            if (entry.getValue().equals(base.get(entry.getKey()))) continue;
            resolve(entry.getKey()).ifPresent(enchantment -> event.setLevel(enchantment, entry.getValue()));
        }
    }

    /**
     * 解析本次查询应当使用的护符修正。
     *
     * <p>优先使用显式上下文（{@link #withOwner} / {@link #pushContext}）；没有上下文时按**引用相等**
     * 在已登记的护符佩戴者中反查物品所有者——`GameplayQueries.currentQueryStack()` 给出的是原版传入的
     * 原始引用，因此"该玩家的手持/背包槽位正好是同一个对象"就是可靠的身份判定，不需要包夹调用点。
     * 查不到所有者时**不施加任何修正**。
     */
    static List<AmuletModifier> resolveModifiers() {
        var contextual = ACTIVE.get();
        if (contextual != null) return contextual;
        var queried = com.mpp.stellaeomphalos.core.platform.GameplayQueries.currentQueryStack();
        if (queried == null || queried.isEmpty()) return List.of();
        for (var reference : TRACKED.values()) {
            var player = reference.get();
            if (player == null || player.isRemoved() || !owns(player, queried)) continue;
            var modifiers = equippedModifiers(player);
            if (!modifiers.isEmpty()) return modifiers;
        }
        return List.of();
    }

    /** 按**引用相等**判断该玩家是否持有给定物品栈。 */
    public static boolean owns(Player player, ItemStack stack) {
        if (player.getMainHandItem() == stack || player.getOffhandItem() == stack) return true;
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++)
            if (inventory.getItem(i) == stack) return true;
        for (var slot : net.minecraft.world.entity.EquipmentSlot.values())
            if (player.getItemBySlot(slot) == stack) return true;
        return false;
    }

    private static ResourceLocation enchantmentId(Enchantment enchantment) {
        return enchantment == null ? null : ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
    }

    private static Optional<Enchantment> resolve(ResourceLocation id) {
        return Optional.ofNullable(ForgeRegistries.ENCHANTMENTS.getValue(id));
    }

    /** 供工具提示使用：在玩家上下文中预览叠加后的等级表。 */
    public static Map<ResourceLocation, Integer> previewFor(
            ServerPlayer player, Map<ResourceLocation, Integer> base) {
        return withOwner(player, () -> AmuletRoller.applyAll(activeModifiers(), base));
    }

    /** 供测试与调试注入上下文。 */
    public static void pushContext(List<AmuletModifier> modifiers) {
        ACTIVE.set(modifiers == null ? List.of() : List.copyOf(modifiers));
    }

    /** 供测试与调试清理上下文。 */
    public static void clearContext() {
        ACTIVE.remove();
    }
}

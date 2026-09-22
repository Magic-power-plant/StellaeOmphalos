package com.mpp.stellaeomphalos.content.effect;

import com.mpp.stellaeomphalos.content.enchantment.PartSixEnchantments;
import com.mpp.stellaeomphalos.content.particle.PartSixParticles;
import com.mpp.stellaeomphalos.content.particle.PartSixSounds;
import com.mpp.stellaeomphalos.core.bootstrap.RegistrationGuard;
import com.mpp.stellaeomphalos.core.registry.ModEffects;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * Part-6 药水效果与附魔的装配点。
 *
 * <p>职责：① 在类加载（DeferredRegister 登记窗口）声明 5 个 {@code MobEffect}；
 * ② 在 {@link #attach(IEventBus)} 中初始化附魔/粒子/音效/进度触发器的声明，
 * 并把免死、流血之外的跨实体行为（掉落修正、时间冻结、附魔装备 tick）注册到 Forge 事件总线。
 *
 * <p>事件监听器全部按静态方法引用注册（继承监听器语义），不持有注册表引用，
 * 因此调用方必须在服务器启动前完成 {@code attach} —— 与 {@code Omphalos} 构造函数中的调用顺序一致。
 */
public final class PartSixEffects {

    /** 玩家被时间冻结所需的最低放大等级。 */
    public static final int PLAYER_FREEZE_AMPLIFIER = 2;

    /** 免死效果触发后保留的生命；放大等级 ≥ {@link #CHEAT_DEATH_HIGH_AMPLIFIER} 时取较高的值。 */
    private static final float CHEAT_DEATH_LOW_HEALTH = 1.0F;

    private static final float CHEAT_DEATH_HIGH_HEALTH = 4.0F;

    /** 免死"高等级"判定阈值（放大等级语义，0 基）。 */
    private static final int CHEAT_DEATH_HIGH_AMPLIFIER = 2;

    /** 免死冷却（tick）。冷却期内再次致死不再拦截。 */
    private static final long CHEAT_DEATH_COOLDOWN_TICKS = 20L * 60L * 5L;

    /** 冷却表上限，超出后按插入顺序淘汰最旧条目（玩家退网时也会即时清理）。 */
    private static final int MAX_COOLDOWN_ENTRIES = 4096;

    private static final AtomicBoolean ATTACHED = new AtomicBoolean();

    /** 免死冷却：UUID -> 上次触发的游戏刻。只在服务端线程访问。 */
    private static final Map<UUID, Long> CHEAT_DEATH_COOLDOWNS =
            new LinkedHashMap<>(64, 0.75F, false) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<UUID, Long> eldest) {
                    return size() > MAX_COOLDOWN_ENTRIES;
                }
            };

    /** id {@code cheat_death}：免死拦截，行为由本类的事件监听器实现。 */
    public static final RegistrationGuard<CheatDeathEffect> CHEAT_DEATH =
            ModEffects.ENTRIES.declare("cheat_death", CheatDeathEffect::new);

    /** id {@code bleed}：周期性真伤流血。 */
    public static final RegistrationGuard<BleedEffect> BLEED =
            ModEffects.ENTRIES.declare("bleed", BleedEffect::new);

    /** id {@code spell_plague}：法术瘟疫。 */
    public static final RegistrationGuard<SpellPlagueEffect> SPELL_PLAGUE =
            ModEffects.ENTRIES.declare("spell_plague", SpellPlagueEffect::new);

    /** id {@code drop_modifier}：掉落修正载体，见 {@link DropModifierEffect#dropBonus(LivingEntity)}。 */
    public static final RegistrationGuard<DropModifierEffect> DROP_MODIFIER =
            ModEffects.ENTRIES.declare("drop_modifier", DropModifierEffect::new);

    /** id {@code time_freeze}：定身。 */
    public static final RegistrationGuard<TimeFreezeEffect> TIME_FREEZE =
            ModEffects.ENTRIES.declare("time_freeze", TimeFreezeEffect::new);

    private PartSixEffects() {}

    /** 幂等装配：声明其余 Part-6 内容并挂接 Forge 事件监听器。 */
    public static void attach(IEventBus modBus) {
        if (!ATTACHED.compareAndSet(false, true)) return;
        PartSixEnchantments.initialize();
        PartSixParticles.initialize();
        PartSixSounds.initialize();
        // 进度触发器必须在 ModAdvancementTriggers.register() 之前声明；装配发生在模组构造期，
        // 早于 FMLCommonSetupEvent，因此这里调用是安全的。核心层不引用 knowledge 层。
        com.mpp.stellaeomphalos.knowledge.advancement.PartSixTriggers.initialize();
        // 星眷等级由 player 层实现，经 core.platform 的只读查询点跨层暴露给 knowledge 层。
        com.mpp.stellaeomphalos.core.platform.BoonLevelQuery.install(
                player -> com.mpp.stellaeomphalos.player.boon.BoonProgress.getServer(player).level());
        var forge = MinecraftForge.EVENT_BUS;
        forge.addListener(PartSixEffects::onLethalDamage);
        forge.addListener(PartSixEffects::onDeath);
        forge.addListener(PartSixEffects::onDrops);
        forge.addListener(PartSixEffects::onLivingTick);
        forge.addListener(PartSixEffects::onPlayerTick);
        forge.addListener(PartSixEffects::onLogout);
        // Part-6 §6.3.5：护符修正经 M-1/M-2 的 GameplayQueries 桥注入原版附魔查询。
        forge.addListener(
                com.mpp.stellaeomphalos.content.item.amulet.AmuletEnchantBridge
                        ::onEnchantmentQuery);
        // §6.6.2：掉落物实体替换（高亮 / 抗爆 / 星屑 / 晶簇 / 晶簇工具）。
        forge.addListener(
                com.mpp.stellaeomphalos.content.item.ItemEntityReplacement::onEntityJoin);
    }

    // ------------------------------------------------------------------ cheat_death

    /**
     * 致死伤害拦截：伤害足以致死时取消本次伤害，消耗效果并把生命设为
     * {@value #CHEAT_DEATH_LOW_HEALTH}（放大等级 ≥ {@value #CHEAT_DEATH_HIGH_AMPLIFIER} 时为
     * {@value #CHEAT_DEATH_HIGH_HEALTH}），随后进入冷却。
     */
    private static void onLethalDamage(LivingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) return;
        if (victim.getHealth() - event.getAmount() > 0.0F) return;
        if (!tryCheatDeath(victim)) return;
        event.setCanceled(true);
    }

    /** 兜底：绕过 {@code LivingDamageEvent} 的致死路径（如 /kill）在死亡事件处再拦一次。 */
    private static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) return;
        if (!tryCheatDeath(victim)) return;
        event.setCanceled(true);
    }

    /**
     * 消耗免死效果并恢复少量生命。
     *
     * @return 是否成功拦截
     */
    private static boolean tryCheatDeath(LivingEntity victim) {
        var instance = victim.getEffect(CHEAT_DEATH.get());
        if (instance == null) return false;
        long now = victim.level().getGameTime();
        UUID id = victim.getUUID();
        Long last = CHEAT_DEATH_COOLDOWNS.get(id);
        if (last != null && now - last < CHEAT_DEATH_COOLDOWN_TICKS) return false;
        victim.removeEffect(CHEAT_DEATH.get());
        float restored = instance.getAmplifier() >= CHEAT_DEATH_HIGH_AMPLIFIER
                ? CHEAT_DEATH_HIGH_HEALTH
                : CHEAT_DEATH_LOW_HEALTH;
        victim.setHealth(Math.min(restored, victim.getMaxHealth()));
        CHEAT_DEATH_COOLDOWNS.put(id, now);
        return true;
    }

    /** 玩家退网时清理冷却记录，避免长跑服务器的表无限增长。 */
    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        CHEAT_DEATH_COOLDOWNS.remove(event.getEntity().getUUID());
        com.mpp.stellaeomphalos.content.item.amulet.AmuletEnchantBridge.untrack(
                event.getEntity().getUUID());
    }

    // ------------------------------------------------------------------ drop_modifier

    /** 击杀者携带 {@code drop_modifier} 时追加可堆叠掉落。 */
    private static void onDrops(LivingDropsEvent event) {
        if (event.getEntity().level().isClientSide) return;
        int bonus = DropModifierEffect.dropBonus(event.getSource().getEntity() instanceof LivingEntity killer
                ? killer
                : null);
        if (bonus <= 0) return;
        DropModifierEffect.applyDropBonus(event.getDrops(), bonus);
    }

    // ------------------------------------------------------------------ time_freeze

    /**
     * 定身：清空位移向量。玩家只有在放大等级 ≥ {@link #PLAYER_FREEZE_AMPLIFIER} 时才被冻结；
     * 已被冻结的生物若尝试起跳会被立刻拉回地面速度；载具/乘客不做处理以免破坏坐骑逻辑。
     */
    private static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;
        if (entity.isPassenger()) return;
        var instance = entity.getEffect(TIME_FREEZE.get());
        if (instance == null) return;
        if (entity instanceof Player && instance.getAmplifier() < PLAYER_FREEZE_AMPLIFIER) return;
        entity.setDeltaMovement(Vec3.ZERO);
        entity.hasImpulse = true;
    }

    // ------------------------------------------------------------------ 附魔装备 tick

    /** 遍历全部装备槽，驱动已登记的 {@code EquippedTickEnchantment}。 */
    private static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.level().isClientSide) return;
        // 护符身份索引：只有佩戴护符的玩家会被扫描，故不需要包夹攻击/挖掘链路。
        com.mpp.stellaeomphalos.content.item.amulet.AmuletEnchantBridge.refreshTracking(player);
        var equipped = PartSixEnchantments.equippedTickEnchantments();
        if (equipped.isEmpty()) return;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            var stack = player.getItemBySlot(slot);
            if (stack.isEmpty() || !stack.isEnchanted()) continue;
            for (var equippedTick : equipped) {
                if (!(equippedTick instanceof Enchantment enchantment)) continue;
                int level = EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack);
                if (level > 0) equippedTick.onEquippedTick(player, level);
            }
        }
    }
}

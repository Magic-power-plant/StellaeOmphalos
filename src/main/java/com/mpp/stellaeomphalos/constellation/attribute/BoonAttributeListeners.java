package com.mpp.stellaeomphalos.constellation.attribute;

import com.mpp.stellaeomphalos.core.util.combat.DamageKit;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** 13 个行为型属性的事件监听；事件里取玩家属性一律走 BoonValueBridge 快照，不做每 tick 重建。 */
public final class BoonAttributeListeners {

    private static final ThreadLocal<Boolean> SUPPRESS_HARVEST_SPEED = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> EXTENDING_EFFECT = ThreadLocal.withInitial(() -> false);

    private BoonAttributeListeners() {}

    /** 模组内部触发挖掘速度事件时临时屏蔽 harvest_speed 加成，防递归加成。 */
    public static <T> T withoutHarvestSpeedBonus(Supplier<T> action) {
        boolean previous = SUPPRESS_HARVEST_SPEED.get();
        SUPPRESS_HARVEST_SPEED.set(true);
        try {
            return action.get();
        } finally {
            SUPPRESS_HARVEST_SPEED.set(previous);
        }
    }

    // #1 元素减免：TagKey<DamageType> 关键词表驱动
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void elementalWard(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) return;
        if (!event.getSource().is(BoonDamageTypes.ELEMENTAL)) return;
        double ward = BoonValueBridge.value(player, BoonAttributes.ELEMENTAL_WARD);
        if (ward > 0) event.setAmount((float) (event.getAmount() * (1.0 - ward)));
    }

    // #2 投射物初速：整体缩放运动向量，方向不变，仅服务端
    @SubscribeEvent
    public static void projectileVelocity(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof Projectile projectile)) return;
        if (!(projectile.getOwner() instanceof Player player)) return;
        double velocity = BoonValueBridge.value(player, BoonAttributes.PROJECTILE_VELOCITY);
        if (velocity != 1.0) projectile.setDeltaMovement(projectile.getDeltaMovement().scale(velocity));
    }

    // #3 挖掘速度：以事件当前速度为基数，乘法通道
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void harvestSpeed(PlayerEvent.BreakSpeed event) {
        if (SUPPRESS_HARVEST_SPEED.get()) return;
        double speed = BoonValueBridge.value(event.getEntity(), BoonAttributes.HARVEST_SPEED);
        if (speed != 1.0) event.setNewSpeed((float) (event.getNewSpeed() * speed));
    }

    // #4/#5 暴击率掷骰 + 暴击伤害倍率（显式 HIGH 阶段，先于常规暴击结算）
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void criticalHit(CriticalHitEvent event) {
        var player = event.getEntity();
        if (player.level().isClientSide) return;
        double chance = BoonValueBridge.value(player, BoonAttributes.CRIT_CHANCE) / 100.0;
        if (chance > 0 && player.getRandom().nextDouble() < chance) {
            event.setResult(Event.Result.ALLOW);
            event.setDamageModifier((float) BoonValueBridge.value(player, BoonAttributes.CRIT_DAMAGE));
        }
    }

    // #6 闪避：取消伤害事件
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void dodge(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) return;
        double dodge = BoonValueBridge.value(player, BoonAttributes.DODGE);
        if (dodge > 0 && player.getRandom().nextDouble() < dodge) event.setCanceled(true);
    }

    // #7 动态附魔：倍率通道 round 到整级；事件由附魔体系（《星坛与制作系统》/《星典知识与玩家进度》）投递
    @SubscribeEvent
    public static void dynamicEnchant(DynamicEnchantEvent event) {
        double multiplier = BoonValueBridge.value(event.player(), BoonAttributes.DYNAMIC_ENCHANT);
        event.setAdjustedLevel((int) Math.round(event.baseLevel() * multiplier));
    }

    // #8 生命偷取：仅服务端实际伤害事件，按输出伤害回血
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void lifeLeech(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide || event.getAmount() <= 0) return;
        if (!(event.getSource().getEntity() instanceof Player attacker)) return;
        double leech = BoonValueBridge.value(attacker, BoonAttributes.LIFE_LEECH);
        if (leech > 0) attacker.heal((float) (event.getAmount() * leech));
    }

    // #9 治疗加成：以事件治疗量为基数，结果 ≤0 取消治疗
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void lifeRecovery(LivingHealEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) return;
        double recovery = BoonValueBridge.value(player, BoonAttributes.LIFE_RECOVERY);
        float healed = (float) (event.getAmount() * recovery);
        if (healed <= 0) {
            event.setCanceled(true);
            return;
        }
        event.setAmount(healed);
    }

    // #10 增益时长延长：仅当新时长超原时长时替换实例；跳过负面与无限时长
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void beneficialDuration(MobEffectEvent.Added event) {
        if (EXTENDING_EFFECT.get() || !(event.getEntity() instanceof Player player) || player.level().isClientSide) return;
        var instance = event.getEffectInstance();
        if (instance == null || !instance.getEffect().isBeneficial() || instance.isInfiniteDuration()) return;
        double multiplier = BoonValueBridge.value(player, BoonAttributes.BENEFICIAL_DURATION);
        int scaled = (int) (instance.getDuration() * multiplier);
        if (scaled <= instance.getDuration()) return;
        var extended = new MobEffectInstance(instance.getEffect(), scaled, instance.getAmplifier(),
                instance.isAmbient(), instance.isVisible(), instance.showIcon());
        EXTENDING_EFFECT.set(true);
        try {
            player.removeEffectNoUpdate(instance.getEffect());
            player.addEffect(extended);
        } finally {
            EXTENDING_EFFECT.set(false);
        }
    }

    // #11 弹射物伤害：IS_PROJECTILE 伤害类型标签且真源为玩家
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void projectileDamage(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide || !event.getSource().is(DamageTypeTags.IS_PROJECTILE)) return;
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        double boost = BoonValueBridge.value(player, BoonAttributes.PROJECTILE_DAMAGE);
        if (boost != 1.0) event.setAmount((float) (event.getAmount() * boost));
    }

    // #12/#13 荆棘：专属 DamageType 防递归；远程反弹需 thorns_ranged 独立开关解锁
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void thorns(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) return;
        var source = event.getSource();
        if (source.is(BoonDamageTypes.BOON_THORNS)) return;
        var attacker = source.getEntity();
        if (attacker == null || attacker == player || !attacker.isAlive()) return;
        double reflect = BoonValueBridge.value(player, BoonAttributes.THORNS);
        if (reflect <= 0) return;
        boolean ranged = source.is(DamageTypeTags.IS_PROJECTILE) || source.getDirectEntity() != attacker;
        if (ranged && BoonValueBridge.value(player, BoonAttributes.THORNS_RANGED) <= 0) return;
        var holder = player.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(BoonDamageTypes.BOON_THORNS);
        DamageKit.apply(attacker, new DamageSource(holder, player), (float) (event.getAmount() * reflect));
    }
}

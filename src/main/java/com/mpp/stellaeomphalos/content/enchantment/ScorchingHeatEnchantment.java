package com.mpp.stellaeomphalos.content.enchantment;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

/**
 * 灼热附魔（id {@code scorching_heat}）：穿着时①把攻击自己的生物点燃 {@code 2 * level} 秒；
 * ②每 40 tick 对身边 2 格内的敌对生物追加 1 点点燃伤害。
 *
 * <p>点燃与伤害都以 40 tick 为节拍，与 {@code LivingDamageEvent}/{@code PlayerTickEvent} 的
 * 触发频率解耦：伤害事件只在被攻击瞬间派发，因此"点燃攻击者"是即时的，而"灼烧周围"是周期性扫场。
 */
public final class ScorchingHeatEnchantment extends Enchantment implements EquippedTickEnchantment {

    /** 周期扫场的节拍（tick）。 */
    private static final int AURA_PERIOD = 40;

    /** 周围生效半径（格）。 */
    private static final double AURA_RADIUS = 2.0D;

    /** 每次扫场对单个目标的火焰伤害。 */
    private static final float AURA_DAMAGE = 1.0F;

    /** 每级点燃攻击者的秒数。 */
    private static final int ATTACKER_FIRE_SECONDS_PER_LEVEL = 2;

    public ScorchingHeatEnchantment() {
        super(Enchantment.Rarity.UNCOMMON, EnchantmentCategory.ARMOR,
                new EquipmentSlot[] {EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
                        EquipmentSlot.HEAD});
    }

    @Override
    public void onEquippedTick(ServerPlayer player, int level) {
        if (player.level().isClientSide) return;
        LivingEntity attacker = player.getLastHurtByMob();
        if (attacker != null && attacker != player && attacker.isAlive() && !attacker.fireImmune())
            attacker.setSecondsOnFire(ATTACKER_FIRE_SECONDS_PER_LEVEL * Math.max(1, level));
        if (player.tickCount % AURA_PERIOD != 0) return;
        for (LivingEntity nearby : player.level().getEntitiesOfClass(
                LivingEntity.class, player.getBoundingBox().inflate(AURA_RADIUS))) {
            if (nearby == player || !nearby.isAlive() || nearby.fireImmune()) continue;
            if (!(nearby instanceof Enemy) && nearby.getMobType() != MobType.UNDEAD) continue;
            if (!nearby.canAttack(player)
                    && !(nearby instanceof net.minecraft.world.entity.Mob mob
                            && mob.getTarget() == player)
                    && nearby.getLastHurtByMob() != player) continue;
            nearby.hurt(player.damageSources().onFire(), AURA_DAMAGE);
        }
    }

    @Override
    public int getMinLevel() {
        return 1;
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }
}

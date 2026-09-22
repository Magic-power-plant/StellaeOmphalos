package com.mpp.stellaeomphalos.content.entity.p6;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 爆炸免疫掉落物：普通掉落物会被爆炸清除，本子类对所有爆炸类伤害源免疫。
 *
 * <p>仅覆盖 {@link #isInvulnerableTo(DamageSource)}：火焰、仙人掌、虚空等原版判定全部保留，
 * 因此它依然会被岩浆烧毁、被掉出世界销毁，只是不会因为 TNT、苦力怕、床炸等爆炸而消失。
 */
public class ExplosionProofItemEntity extends ItemEntity {

    public ExplosionProofItemEntity(EntityType<? extends ExplosionProofItemEntity> type, Level level) {
        super(type, level);
    }

    public ExplosionProofItemEntity(Level level, double x, double y, double z, ItemStack stack) {
        super(level, x, y, z, stack);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.is(DamageTypeTags.IS_EXPLOSION)) return true;
        return super.isInvulnerableTo(source);
    }

    /** 以爆炸免疫掉落物的形式投放一个物品栈。 */
    public static ExplosionProofItemEntity drop(Level level, double x, double y, double z, ItemStack stack) {
        if (level.isClientSide || stack.isEmpty()) return null;
        var entity = new ExplosionProofItemEntity(level, x, y, z, stack.copy());
        level.addFreshEntity(entity);
        return entity;
    }
}

package com.mpp.stellaeomphalos.content.entity.p6;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;

/**
 * 微光精灵：纯环境氛围生物，只做漂浮与随机张望。
 *
 * <p>分类为 {@link net.minecraft.world.entity.MobCategory#AMBIENT}，不可被推动，离玩家过远即消失，
 * 免疫火焰且没有环境音（{@link #getAmbientSound()} 返回 null）。没有目标选择器，永不主动攻击，
 * 因此可以安全地大量生成在遗迹与观测站附近。
 */
public class WispEntity extends PathfinderMob {

    private static final int FLOAT_PRIORITY = 0;
    private static final int LOOK_PRIORITY = 1;
    private static final double FLY_SPEED = 0.6D;

    public WispEntity(EntityType<? extends WispEntity> type, Level level) {
        super(type, level);
        FlyerKit.makeFlyer(this);
        this.moveControl = new net.minecraft.world.entity.ai.control.FlyingMoveControl(this, 20, true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 6.0D)
                .add(Attributes.FLYING_SPEED, 0.4D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D)
                .add(Attributes.FOLLOW_RANGE, 12.0D);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return FlyerKit.flyingNavigation(this, level);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(FLOAT_PRIORITY, new WaterAvoidingRandomFlyingGoal(this, FLY_SPEED));
        goalSelector.addGoal(LOOK_PRIORITY, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return true;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }
}

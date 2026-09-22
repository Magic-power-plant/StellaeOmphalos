package com.mpp.stellaeomphalos.content.entity.p6;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 星矢：朝服务端记录的目标实体归航，目标消失后按当前方向直线飞行。
 *
 * <p>目标只以实体 id 保存在服务端（NBT 键 "TargetId"），不进入 {@code SynchedEntityData}，因此不会
 * 暴露给客户端。归航使用有上限的加速度，避免急转；命中任何东西都造成一次小额伤害后消失。
 */
public class StarBoltEntity extends ThrowableItemProjectile {

    /** 归航加速度。 */
    private static final double HOMING_ACCELERATION = 0.14D;
    /** 归航最大速度。 */
    private static final double HOMING_MAX_SPEED = 1.6D;
    /** 命中伤害。 */
    public static final float HIT_DAMAGE = 3.0F;
    /** 最长存活刻数。 */
    private static final int MAX_LIFETIME = 400;
    /** 命中判定半径。 */
    private static final double HIT_RADIUS = 0.5D;

    private int targetId = -1;
    private int age;

    public StarBoltEntity(EntityType<? extends StarBoltEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public StarBoltEntity(EntityType<? extends StarBoltEntity> type, LivingEntity owner, Level level) {
        super(type, owner, level);
        setNoGravity(true);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.AMETHYST_SHARD;
    }

    /** 设置归航目标；-1 表示直线飞行。 */
    public void setTargetId(int id) {
        targetId = id;
    }

    /** @return 归航目标 id，未设置时为 -1 */
    public int getTargetId() {
        return targetId;
    }

    @Override
    public void tick() {
        if (level() instanceof ServerLevel server && targetId >= 0 && !isRemoved()) {
            Entity target = server.getEntity(targetId);
            if (target == null || !target.isAlive()) {
                targetId = -1;
            } else {
                Vec3 aim = target.position()
                        .add(0.0D, target.getBbHeight() * 0.5D, 0.0D)
                        .subtract(position()).normalize().scale(HOMING_ACCELERATION);
                Vec3 velocity = getDeltaMovement().add(aim);
                if (velocity.lengthSqr() > HOMING_MAX_SPEED * HOMING_MAX_SPEED) {
                    velocity = velocity.normalize().scale(HOMING_MAX_SPEED);
                }
                setDeltaMovement(velocity);
            }
        }
        super.tick();
        if (!level().isClientSide && ++age > MAX_LIFETIME) discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        damageNearby();
        discard();
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        damageNearby();
        discard();
    }

    /** 对判定半径内的非主人生物造成一次小额伤害；主人不是生物时只做命中判定。 */
    private void damageNearby() {
        if (level().isClientSide) return;
        Entity owner = getOwner();
        DamageSource source = owner instanceof LivingEntity living
                ? level().damageSources().thrown(this, living)
                : level().damageSources().generic();
        for (Entity candidate : level().getEntities(this, getBoundingBox().inflate(HIT_RADIUS),
                entity -> entity instanceof LivingEntity && entity != owner)) {
            candidate.hurt(source, HIT_DAMAGE);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("TargetId", targetId);
        tag.putInt("BoltAge", age);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        targetId = tag.getInt("TargetId");
        age = tag.getInt("BoltAge");
    }
}

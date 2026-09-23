package com.mpp.stellaeomphalos.content.entity.catalog;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 流星：沿固定的水平速度矢量飞行，落点只产生一次无破坏的小爆炸。
 *
 * <p>水平速度分量保存在 NBT 键 "ShootX" 与 "ShootZ" 上，并在每个服务端刻重新施加，因此即使中途
 * 存档重载也保持同一条轨迹。同步长整型 "EffectSeed" 供客户端生成确定性特效。持久键 "LastUpdate"
 * 是心跳：若某个服务端刻发现 {@code gameTime - LastUpdate > 20}，说明中间有 20 刻以上没有推进
 * （区块卸载/卡顿/存档回滚），实体立即自毁。爆炸使用
 * {@link Explosion.BlockInteraction#NONE}，只造成冲击不破坏任何方块。
 */
public class FallingStarEntity extends ThrowableItemProjectile {

    private static final EntityDataAccessor<Long> DATA_EFFECT_SEED =
            SynchedEntityData.defineId(FallingStarEntity.class, EntityDataSerializers.LONG);

    /** 心跳超时阈值（刻）。 */
    public static final long HEARTBEAT_TIMEOUT = 20L;
    /** 落地爆炸半径，足够推动实体但不足以破坏方块。 */
    private static final float IMPACT_RADIUS = 1.5F;
    /** 最长存活刻数，避免失速后永久驻留。 */
    private static final int MAX_LIFETIME = 600;

    private double shootX;
    private double shootZ;
    private long lastUpdate;
    private int age;

    public FallingStarEntity(EntityType<? extends FallingStarEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public FallingStarEntity(EntityType<? extends FallingStarEntity> type, LivingEntity owner, Level level) {
        super(type, owner, level);
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        getEntityData().define(DATA_EFFECT_SEED, 0L);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.FIREWORK_ROCKET;
    }

    /** @return 客户端用于生成特效的确定性随机种子 */
    public long getEffectSeed() {
        return getEntityData().get(DATA_EFFECT_SEED);
    }

    public void setEffectSeed(long seed) {
        getEntityData().set(DATA_EFFECT_SEED, seed);
    }

    public double getShootX() {
        return shootX;
    }

    public double getShootZ() {
        return shootZ;
    }

    /** 记录本体的水平飞行方向（会被逐刻重新施加）。 */
    public void setShoot(double x, double z) {
        shootX = x;
        shootZ = z;
    }

    @Override
    public void tick() {
        if (!level().isClientSide) {
            long now = level().getGameTime();
            if (lastUpdate != 0L && now - lastUpdate > HEARTBEAT_TIMEOUT) {
                discard();
                return;
            }
            lastUpdate = now;
            if (++age > MAX_LIFETIME) {
                impact();
                return;
            }
        }
        super.tick();
        if (isRemoved()) return;
        Vec3 velocity = getDeltaMovement();
        if (shootX != 0.0D || shootZ != 0.0D) {
            setDeltaMovement(shootX, velocity.y, shootZ);
        } else {
            shootX = velocity.x;
            shootZ = velocity.z;
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        if (level().isClientSide) {
            discard();
            return;
        }
        impact();
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (level().isClientSide) {
            discard();
            return;
        }
        impact();
    }

    /** 制造一次无方块破坏的小爆炸并消失。 */
    private void impact() {
        if (level().isClientSide || isRemoved()) return;
        level().playSound(null,getX(),getY(),getZ(),com.mpp.stellaeomphalos.content.particle.ClientVisualContent.sound("meteor_impact"),net.minecraft.sounds.SoundSource.AMBIENT,.8F,1);
        level().explode(
                this,
                getX(),
                getY(),
                getZ(),
                IMPACT_RADIUS,
                net.minecraft.world.level.Level.ExplosionInteraction.NONE);
        discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putDouble("ShootX", shootX);
        tag.putDouble("ShootZ", shootZ);
        tag.putLong("EffectSeed", getEffectSeed());
        tag.putLong("LastUpdate", lastUpdate);
        tag.putInt("StarAge", age);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        shootX = tag.getDouble("ShootX");
        shootZ = tag.getDouble("ShootZ");
        setEffectSeed(tag.getLong("EffectSeed"));
        lastUpdate = tag.getLong("LastUpdate");
        age = tag.getInt("StarAge");
    }
}

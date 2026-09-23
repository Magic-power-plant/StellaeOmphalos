package com.mpp.stellaeomphalos.content.entity.catalog;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 抓钩：命中方块后钩住并把自己拉向钩点。
 *
 * <p>命中方块时停止运动、记录钩点并把同步布尔 "Pulling" 置为 true；此后每刻朝钩点给主人一个很小的
 * 速度增量。当主人距钩点 2 格以内、或连续 15 刻没有靠近（进度停滞）时立刻移除。本类只读写实体数据，
 * 不修改任何方块。
 */
public class GrapnelEntity extends ThrowableItemProjectile {

    private static final EntityDataAccessor<Boolean> DATA_PULLING =
            SynchedEntityData.defineId(GrapnelEntity.class, EntityDataSerializers.BOOLEAN);

    /** 拉拽加速度。 */
    private static final double PULL_STRENGTH = 0.32D;
    /** 允许的最大拉拽速度。 */
    private static final double MAX_PULL_SPEED = 1.1D;
    /** 抵达判定距离（格）。 */
    private static final double ARRIVAL_DISTANCE = 2.0D;
    /** 允许的无进展刻数。 */
    private static final int STALLED_TICKS = 15;

    private BlockPos hooked;
    private double lastDistance = Double.NaN;
    private int stalled;

    public GrapnelEntity(EntityType<? extends GrapnelEntity> type, Level level) {
        super(type, level);
    }

    public GrapnelEntity(EntityType<? extends GrapnelEntity> type, LivingEntity owner, Level level) {
        super(type, owner, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        getEntityData().define(DATA_PULLING, false);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.LEAD;
    }

    /** @return 是否正在拉拽 */
    public boolean isPulling() {
        return getEntityData().get(DATA_PULLING);
    }

    public void setPulling(boolean pulling) {
        getEntityData().set(DATA_PULLING, pulling);
    }

    /** @return 已钩住的方块坐标，未钩住时为 null */
    public BlockPos getHooked() {
        return hooked;
    }

    @Override
    public void tick() {
        if (isPulling()) {
            if (level().isClientSide) {
                super.tick();
                return;
            }
            if (!pullOwner()) return;
            return;
        }
        super.tick();
        if (hooked != null) setDeltaMovement(Vec3.ZERO);
    }

    /**
     * 朝钩点拉主人一格。
     *
     * @return 实体是否仍然存活（false 表示本刻已被移除）
     */
    private boolean pullOwner() {
        if (hooked == null) {
            discard();
            return false;
        }
        Entity owner = getOwner();
        if (!(owner instanceof LivingEntity living) || owner.isRemoved()
                || living instanceof Player player && player.isSpectator()) {
            discard();
            return false;
        }
        Vec3 toHook = Vec3.atCenterOf(hooked).subtract(living.position());
        double distance = toHook.length();
        if (distance <= ARRIVAL_DISTANCE) {
            discard();
            return false;
        }
        stalled = Double.isFinite(lastDistance) && distance > lastDistance - 0.05D ? stalled + 1 : 0;
        lastDistance = distance;
        if (stalled >= STALLED_TICKS) {
            discard();
            return false;
        }
        Vec3 pull = toHook.normalize().scale(PULL_STRENGTH);
        Vec3 velocity = living.getDeltaMovement().add(pull);
        if (velocity.lengthSqr() > MAX_PULL_SPEED * MAX_PULL_SPEED) {
            velocity = velocity.normalize().scale(MAX_PULL_SPEED);
        }
        living.setDeltaMovement(velocity);
        living.fallDistance = 0.0F;
        living.hurtMarked = true;
        setPos(living.position().add(toHook.normalize().scale(-0.5D)));
        return true;
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        if (level().isClientSide) return;
        hooked = hit.getBlockPos().immutable();
        setPulling(true);
        setDeltaMovement(Vec3.ZERO);
        setPos(hooked.getX() + 0.5D, hooked.getY() + 1.0D, hooked.getZ() + 0.5D);
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (level().isClientSide || hooked != null) return;
        hooked = hit.getEntity().blockPosition().immutable();
        setPulling(true);
        setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Pulling", isPulling());
        if (hooked != null) {
            tag.putInt("HookedX", hooked.getX());
            tag.putInt("HookedY", hooked.getY());
            tag.putInt("HookedZ", hooked.getZ());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setPulling(tag.getBoolean("Pulling"));
        hooked = tag.contains("HookedX")
                ? new BlockPos(tag.getInt("HookedX"), tag.getInt("HookedY"), tag.getInt("HookedZ"))
                : null;
    }
}

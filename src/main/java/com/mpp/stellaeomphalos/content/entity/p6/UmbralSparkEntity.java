package com.mpp.stellaeomphalos.content.entity.p6;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 幽影火花：落地后进入 200 刻的"孵化"阶段，每 20 刻尝试在周围 3×3×3 体积内召唤一次怪物。
 *
 * <p>召唤走原版 {@link NaturalSpawner#spawnCategoryForPosition}，因此完全遵守生物群系刷怪表、
 * 光照、玩家距离与 Forge 生成事件；只在夜晚或天空变暗（{@code getSkyDarken() > 4}）时尝试，
 * 最多 8 次，无论成功与否到时都自行消失。同步布尔 "Spawning" 让客户端可以播放对应视觉效果。
 */
public class UmbralSparkEntity extends ThrowableItemProjectile {

    private static final EntityDataAccessor<Boolean> DATA_SPAWNING =
            SynchedEntityData.defineId(UmbralSparkEntity.class, EntityDataSerializers.BOOLEAN);

    /** 孵化总时长（刻）。 */
    public static final int SPAWNING_TICKS = 200;
    /** 两次尝试之间的间隔。 */
    public static final int ATTEMPT_INTERVAL = 20;
    /** 最多尝试次数。 */
    public static final int MAX_ATTEMPTS = 8;
    /** 黑暗阈值。 */
    private static final int DARK_SKY_THRESHOLD = 4;
    /** 候选点检查半径（3×3×3 体积的一半）。 */
    private static final int CANDIDATE_RADIUS = 1;

    private int spawnedTicks;
    private int attempts;

    public UmbralSparkEntity(EntityType<? extends UmbralSparkEntity> type, Level level) {
        super(type, level);
    }

    public UmbralSparkEntity(EntityType<? extends UmbralSparkEntity> type, LivingEntity owner, Level level) {
        super(type, owner, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        getEntityData().define(DATA_SPAWNING, false);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.COAL;
    }

    /** @return 是否正在孵化 */
    public boolean isSpawning() {
        return getEntityData().get(DATA_SPAWNING);
    }

    public void setSpawning(boolean spawning) {
        getEntityData().set(DATA_SPAWNING, spawning);
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        if (level().isClientSide || isSpawning()) return;
        setSpawning(true);
        setDeltaMovement(Vec3.ZERO);
        spawnedTicks = 0;
        attempts = 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || isRemoved() || !isSpawning()) return;
        setDeltaMovement(Vec3.ZERO);
        spawnedTicks++;
        if (spawnedTicks % ATTEMPT_INTERVAL == 0 && attempts < MAX_ATTEMPTS && canSpawnHere()) {
            attempts++;
            attemptSpawn();
        }
        if (spawnedTicks >= SPAWNING_TICKS) discard();
    }

    /** @return 当前是否处于足够黑暗的环境 */
    private boolean canSpawnHere() {
        return level().isNight() || level().getSkyDarken() > DARK_SKY_THRESHOLD;
    }

    /** 在 3×3×3 体积内逐个候选点尝试原版怪物生成，成功后立即返回。 */
    private void attemptSpawn() {
        if (!(level() instanceof ServerLevel server)) return;
        BlockPos origin = blockPosition();
        for (int offsetY = -CANDIDATE_RADIUS; offsetY <= CANDIDATE_RADIUS; offsetY++) {
            for (int offsetX = -CANDIDATE_RADIUS; offsetX <= CANDIDATE_RADIUS; offsetX++) {
                for (int offsetZ = -CANDIDATE_RADIUS; offsetZ <= CANDIDATE_RADIUS; offsetZ++) {
                    BlockPos candidate = origin.offset(offsetX, offsetY, offsetZ);
                    if (!server.hasChunkAt(candidate)) continue;
                    BlockState state = server.getBlockState(candidate);
                    if (!state.isAir() && !state.canBeReplaced()) continue;
                    NaturalSpawner.spawnCategoryForPosition(
                            MobCategory.MONSTER, server, candidate);
                    if (!server.getEntities((Entity) null,
                                    new AABB(candidate).inflate(CANDIDATE_RADIUS),
                                    entity -> entity instanceof Mob)
                            .isEmpty()) {
                        return;
                    }
                }
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Spawning", isSpawning());
        tag.putInt("SpawnedTicks", spawnedTicks);
        tag.putInt("Attempts", attempts);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setSpawning(tag.getBoolean("Spawning"));
        spawnedTicks = tag.getInt("SpawnedTicks");
        attempts = tag.getInt("Attempts");
    }
}

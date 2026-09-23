package com.mpp.stellaeomphalos.content.entity.catalog;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

/**
 * 观测台座位：零体积、无物理、不可选中/不可碰撞的纯逻辑座椅。
 *
 * <p>座位通过 {@code FixedX/FixedY/FixedZ} 记住自己所属的方块；每 20 刻校验该方块是否仍然是当初
 * 放置座位时的那个方块，一旦被破坏（或换成别的方块）立即自毁，避免留下悬空座椅。乘客每刻被强制
 * 对齐到座位保存的 yaw/pitch，因此视角不会漂移。
 */
public class ObservatorySeatEntity extends Entity {

    /** 方块校验间隔（刻）。 */
    private static final int VALIDATE_INTERVAL = 20;
    /** 乘客相对座位的 Y 偏移。 */
    private static final double PASSENGER_OFFSET = 0.25D;

    private BlockPos fixed = BlockPos.ZERO;
    private float seatYaw;
    private float seatPitch;

    public ObservatorySeatEntity(EntityType<? extends ObservatorySeatEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || isRemoved()) return;
        validateOwnerBlock();
        if (isRemoved()) return;
        lockPassenger();
    }

    /** 校验固定方块是否仍然存在；缺失或换块即销毁。 */
    private void validateOwnerBlock() {
        if (fixed.equals(BlockPos.ZERO)) return;
        if (level().getGameTime() % VALIDATE_INTERVAL != 0L) return;
        if (!level().hasChunkAt(fixed) || level().getBlockState(fixed).isAir()) discard();
    }

    /** 把第一名乘客强制对齐到座位朝向。 */
    private void lockPassenger() {
        Entity passenger = getFirstPassenger();
        if (!(passenger instanceof LivingEntity living)) return;
        living.setYRot(seatYaw);
        living.setXRot(seatPitch);
        living.setYHeadRot(seatYaw);
        living.setYBodyRot(seatYaw);
        living.setDeltaMovement(living.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D));
    }

    /** 设置座位所属方块，并把实体摆到该方块中心。 */
    public void setFixed(BlockPos pos) {
        fixed = pos.immutable();
        setPos(fixed.getX() + 0.5D, fixed.getY() + 0.5D, fixed.getZ() + 0.5D);
    }

    /** @return 座位所属方块坐标 */
    public BlockPos getFixed() {
        return fixed;
    }

    /** 记录并应用座位朝向。 */
    public void setSeatRotation(float yaw, float pitch) {
        seatYaw = yaw;
        seatPitch = pitch;
        setYRot(yaw);
        setXRot(pitch);
        yRotO = yaw;
        xRotO = pitch;
    }

    public float getSeatYaw() {
        return seatYaw;
    }

    public float getSeatPitch() {
        return seatPitch;
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction mover) {
        if (!hasPassenger(passenger)) return;
        mover.accept(passenger, getX(), getY() + PASSENGER_OFFSET, getZ());
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.fixed(0.0F, 0.0F);
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty() && passenger instanceof Player;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("FixedX", fixed.getX());
        tag.putInt("FixedY", fixed.getY());
        tag.putInt("FixedZ", fixed.getZ());
        tag.putFloat("SeatYaw", seatYaw);
        tag.putFloat("SeatPitch", seatPitch);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        fixed = new BlockPos(tag.getInt("FixedX"), tag.getInt("FixedY"), tag.getInt("FixedZ"));
        seatYaw = tag.getFloat("SeatYaw");
        seatPitch = tag.getFloat("SeatPitch");
        setYRot(seatYaw);
        setXRot(seatPitch);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public boolean isAttackable() {
        return false;
    }
}

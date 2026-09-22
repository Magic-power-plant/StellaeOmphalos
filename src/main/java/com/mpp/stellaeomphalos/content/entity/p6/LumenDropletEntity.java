package com.mpp.stellaeomphalos.content.entity.p6;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 流光液滴：一种漂浮的"液体载具"实体，把一份 {@link FluidStack} 从管道运到目标方块。
 *
 * <p>流体的注册名与数量分别存在 NBT 键 "Fluid" 与 "Amount" 上（可选附加数据放在 "FluidTag"），
 * 目标坐标存在 "TargetX"、"TargetY"、"TargetZ" 上。本类不渲染任何东西，视觉表现由客户端工作流负责。
 * 每服务端刻朝目标做有上限的加速飞行，抵达 1.5 格以内即减速悬停，仍走原版移动解算因此不会穿墙。
 */
public class LumenDropletEntity extends PathfinderMob {

    private static final int LOOK_PRIORITY = 0;
    private static final double FLY_ACCELERATION = 0.06D;
    private static final double MAX_SPEED = 0.32D;
    private static final double ARRIVAL_DISTANCE_SQR = 2.25D;

    private FluidStack carried = FluidStack.EMPTY;

    public LumenDropletEntity(EntityType<? extends LumenDropletEntity> type, Level level) {
        super(type, level);
        FlyerKit.makeFlyer(this);
        this.moveControl = new net.minecraft.world.entity.ai.control.FlyingMoveControl(this, 20, true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 4.0D)
                .add(Attributes.FLYING_SPEED, 0.5D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return FlyerKit.flyingNavigation(this, level);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(LOOK_PRIORITY, new RandomLookAroundGoal(this));
    }

    /** 设置携带的流体；null 视为清空。 */
    public void setFluid(FluidStack stack) {
        if (stack == null || stack.isEmpty()) {
            carried = FluidStack.EMPTY;
            return;
        }
        setFluidInternal(new FluidStack(stack.getFluid(), Math.max(0, stack.getAmount()),
                stack.hasTag() ? stack.getTag().copy() : null));
    }

    private void setFluidInternal(FluidStack stack) {
        carried = stack;
        CompoundTag data = getPersistentData();
        if (stack == null || stack.isEmpty()) {
            data.remove("Fluid");
            data.remove("Amount");
            data.remove("FluidTag");
            return;
        }
        ResourceLocation id = ForgeRegistries.FLUIDS.getKey(stack.getFluid());
        if (id == null) {
            carried = FluidStack.EMPTY;
            return;
        }
        data.putString("Fluid", id.toString());
        data.putInt("Amount", stack.getAmount());
        if (stack.hasTag()) data.put("FluidTag", stack.getTag().copy());
        else data.remove("FluidTag");
    }

    /** @return 当前携带的流体，未设置时为 {@link FluidStack#EMPTY} */
    public FluidStack getFluid() {
        return carried;
    }

    /** 设置飞行目标方块。 */
    public void setTarget(BlockPos target) {
        CompoundTag data = getPersistentData();
        data.putInt("TargetX", target.getX());
        data.putInt("TargetY", target.getY());
        data.putInt("TargetZ", target.getZ());
    }

    /** @return 当前飞行目标，未设置时为 null */
    public BlockPos targetPos() {
        CompoundTag data = getPersistentData();
        if (!data.contains("TargetX") || !data.contains("TargetY") || !data.contains("TargetZ")) {
            return null;
        }
        return new BlockPos(data.getInt("TargetX"), data.getInt("TargetY"), data.getInt("TargetZ"));
    }

    /** @return 是否已经抵达目标（没有目标时也视为抵达） */
    public boolean arrived() {
        BlockPos target = targetPos();
        return target == null || position().distanceToSqr(Vec3.atCenterOf(target)) <= ARRIVAL_DISTANCE_SQR;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || isRemoved()) return;
        BlockPos target = targetPos();
        if (target == null || carried.isEmpty()) return;
        Vec3 toTarget = Vec3.atCenterOf(target).subtract(position());
        if (toTarget.lengthSqr() <= ARRIVAL_DISTANCE_SQR) {
            setDeltaMovement(getDeltaMovement().scale(0.6D));
            return;
        }
        Vec3 velocity = getDeltaMovement().add(toTarget.normalize().scale(FLY_ACCELERATION));
        if (velocity.lengthSqr() > MAX_SPEED * MAX_SPEED) velocity = velocity.normalize().scale(MAX_SPEED);
        setDeltaMovement(velocity);
        double horizontal = velocity.horizontalDistance();
        setYRot((float) (Math.atan2(velocity.x, velocity.z) * (180.0D / Math.PI)));
        setXRot((float) (Math.atan2(velocity.y, horizontal) * (180.0D / Math.PI)));
        hasImpulse = true;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        writeFluid(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        readFluid(tag);
    }

    private void writeFluid(CompoundTag tag) {
        if (carried.isEmpty()) return;
        ResourceLocation id = ForgeRegistries.FLUIDS.getKey(carried.getFluid());
        if (id == null) return;
        tag.putString("Fluid", id.toString());
        tag.putInt("Amount", carried.getAmount());
        if (carried.hasTag()) tag.put("FluidTag", carried.getTag().copy());
    }

    private void readFluid(CompoundTag tag) {
        if (!tag.contains("Fluid")) {
            carried = FluidStack.EMPTY;
            return;
        }
        ResourceLocation id = ResourceLocation.tryParse(tag.getString("Fluid"));
        var fluid = id == null ? null : ForgeRegistries.FLUIDS.getValue(id);
        if (fluid == null || fluid == Fluids.EMPTY) {
            carried = FluidStack.EMPTY;
            return;
        }
        setFluidInternal(new FluidStack(fluid, Math.max(0, tag.getInt("Amount")),
                tag.contains("FluidTag") ? tag.getCompound("FluidTag").copy() : null));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }
}

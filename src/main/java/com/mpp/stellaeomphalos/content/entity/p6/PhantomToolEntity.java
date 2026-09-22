package com.mpp.stellaeomphalos.content.entity.p6;

import com.mpp.stellaeomphalos.core.util.world.TreeHarvester;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 幻影工具：一件被"派出去干活"的工具实体，每 20 刻执行一个受限的工作单元。
 *
 * <p>任务名保存在 NBT 键 "Task" 上（{@code MINE}/{@code FELL}/{@code FIGHT}），工具栈保存在
 * "Tool" 上，作业原点保存在 "StartX"/"StartY"/"StartZ" 上。
 * <ul>
 *   <li>{@code MINE}：工具是镐时，在原点周围破坏最多 4 个可破坏方块；
 *   <li>{@code FELL}：工具是斧时，用 {@link TreeHarvester} 迭代识别原点的树并逐块砍倒；
 *   <li>{@code FIGHT}：对 3 格内最近的敌对生物造成一次伤害。
 * </ul>
 * 作业总时长固定为 {@link #DEATH_TICKS} 刻，到时自毁且不掉落任何物品。
 */
public class PhantomToolEntity extends PathfinderMob {

    /** 每次作业之间的间隔（刻）。 */
    public static final int WORK_INTERVAL = 20;
    /** 固定寿命（刻）。 */
    public static final int DEATH_TICKS = 1200;
    /** 单次 MINE 破坏的方块上限。 */
    public static final int MINE_LIMIT = 4;
    /** 单次 FELL 破坏的方块上限。 */
    public static final int FELL_LIMIT = 16;
    /** FELL 每次推进的预算。 */
    private static final int FELL_BUDGET = 8;
    /** FELL 单棵树的识别上限。 */
    private static final int FELL_SCAN_LIMIT = 256;
    /** FIGHT 的搜索半径。 */
    private static final double FIGHT_RANGE = 3.0D;
    /** FIGHT 单次伤害。 */
    private static final float FIGHT_DAMAGE = 4.0F;

    private static final net.minecraft.network.syncher.EntityDataAccessor<ItemStack> DISPLAY_TOOL =
            net.minecraft.network.syncher.SynchedEntityData.defineId(PhantomToolEntity.class, net.minecraft.network.syncher.EntityDataSerializers.ITEM_STACK);
    @Override protected void defineSynchedData() { super.defineSynchedData(); entityData.define(DISPLAY_TOOL, ItemStack.EMPTY); }
    private ItemStack tool = ItemStack.EMPTY;
    private String task = "MINE";
    private BlockPos startPos = BlockPos.ZERO;
    private int age;
    private int minedInBatch;
    private TreeHarvester harvester;

    public PhantomToolEntity(EntityType<? extends PhantomToolEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        moveControl = new FlyingMoveControl(this, 20, true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0D)
                .add(Attributes.FLYING_SPEED, 0.4D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D)
                .add(Attributes.ATTACK_DAMAGE, FIGHT_DAMAGE);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        var navigation = new FlyingPathNavigation(this, level);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || isRemoved()) return;
        age++;
        if (age >= DEATH_TICKS) {
            discard();
            return;
        }
        setDeltaMovement(getDeltaMovement().scale(0.5D));
        if (age % WORK_INTERVAL != 0) return;
        executeWork();
    }

    /** 执行一个受限的工作单元。 */
    private void executeWork() {
        if (!(level() instanceof ServerLevel server)) return;
        if (tool.isEmpty() || startPos.equals(BlockPos.ZERO)) return;
        switch (task) {
            case "MINE" -> mine(server);
            case "FELL" -> fell(server);
            case "FIGHT" -> fight(server);
            default -> { }
        }
    }

    /** 在原点周围破坏最多 {@link #MINE_LIMIT} 个可破坏方块。 */
    private void mine(ServerLevel server) {
        if (!(tool.getItem() instanceof PickaxeItem)) return;
        minedInBatch = 0;
        for (int offsetY = -1; offsetY <= 1 && minedInBatch < MINE_LIMIT; offsetY++) {
            for (int offsetX = -1; offsetX <= 1 && minedInBatch < MINE_LIMIT; offsetX++) {
                for (int offsetZ = -1; offsetZ <= 1 && minedInBatch < MINE_LIMIT; offsetZ++) {
                    if (breakBlock(server, startPos.offset(offsetX, offsetY, offsetZ))) minedInBatch++;
                }
            }
        }
    }

    /** 迭代识别原点处的树并逐块砍倒。 */
    private void fell(ServerLevel server) {
        if (!(tool.getItem() instanceof AxeItem)) return;
        if (harvester == null) harvester = new TreeHarvester(startPos);
        boolean exhausted = harvester.advance(server, FELL_BUDGET, FELL_SCAN_LIMIT);
        List<BlockPos> found = harvester.result();
        int broken = 0;
        for (BlockPos pos : found) {
            if (broken >= FELL_LIMIT) break;
            if (breakBlock(server, pos)) broken++;
        }
        if (exhausted) harvester = null;
    }

    /** 对 3 格内最近的敌对生物造成一次伤害。 */
    private void fight(ServerLevel server) {
        List<Mob> candidates = server.getEntitiesOfClass(Mob.class,
                getBoundingBox().inflate(FIGHT_RANGE), this::isHostileTarget);
        if (candidates.isEmpty()) return;
        Mob nearest = candidates.get(0);
        double best = Double.MAX_VALUE;
        for (Mob candidate : candidates) {
            double distance = candidate.distanceToSqr(this);
            if (distance < best) {
                best = distance;
                nearest = candidate;
            }
        }
        nearest.hurt(server.damageSources().mobAttack(this), FIGHT_DAMAGE);
    }

    private boolean isHostileTarget(Mob candidate) {
        if (candidate == this || !candidate.isAlive()) return false;
        if (candidate instanceof Monster) return true;
        MobType type = candidate.getMobType();
        return type == MobType.UNDEAD || type == MobType.ARTHROPOD || type == MobType.ILLAGER;
    }

    /** 尝试破坏一个方块；不可破坏或工具不适用时返回 false。 */
    private boolean breakBlock(ServerLevel server, BlockPos pos) {
        if (!server.hasChunkAt(pos)) return false;
        BlockState state = server.getBlockState(pos);
        if (state.isAir() || state.getDestroySpeed(server, pos) < 0.0F) return false;
        if (state.requiresCorrectToolForDrops() && !tool.isCorrectToolForDrops(state)) return false;
        return server.destroyBlock(pos, true);
    }

    public void setTool(ItemStack stack) {
        tool = stack == null ? ItemStack.EMPTY : stack.copy();
        entityData.set(DISPLAY_TOOL, tool.copy());
    }

    public ItemStack getTool() {
        return level().isClientSide ? entityData.get(DISPLAY_TOOL) : tool;
    }

    public void setTask(String name) {
        task = name == null ? "MINE" : name.toUpperCase(Locale.ROOT);
    }

    public String getTask() {
        return task;
    }

    public void setStartPos(BlockPos pos) {
        startPos = pos.immutable();
        harvester = null;
    }

    public BlockPos getStartPos() {
        return startPos;
    }

    /** @return 已经过的工作刻数 */
    public int getWorkAge() {
        return age;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        // 幻影工具不携带战利品；工具栈由它自己保管，不允许掉出。
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("Tool", tool.save(new CompoundTag()));
        tag.putString("Task", task);
        tag.putInt("StartX", startPos.getX());
        tag.putInt("StartY", startPos.getY());
        tag.putInt("StartZ", startPos.getZ());
        tag.putInt("WorkAge", age);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setTool(ItemStack.of(tag.getCompound("Tool")));
        setTask(tag.getString("Task"));
        setStartPos(new BlockPos(tag.getInt("StartX"), tag.getInt("StartY"), tag.getInt("StartZ")));
        age = tag.getInt("WorkAge");
    }
}

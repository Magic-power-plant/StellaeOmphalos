package com.mpp.stellaeomphalos.content.entity.p6;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 带高亮的掉落物：在普通掉落物物理之上额外同步一个 "Color" 颜色值。
 *
 * <p>普通 {@link ItemEntity} 不带任何发光/着色信息，渲染层无法区分特殊掉落物；本类把颜色存入
 * {@link SynchedEntityData}（服务端权威、客户端可读），并保证物品栈本身仍是普通物品栈——物理、
 * 拾取、合并、消失计时全部沿用原版行为。颜色只作为附加元数据随实体同步。
 */
public class HighlightedItemEntity extends ItemEntity {

    /** 同步的颜色（ARGB）；0 表示不额外着色。 */
    private static final EntityDataAccessor<Integer> DATA_COLOR =
            SynchedEntityData.defineId(HighlightedItemEntity.class, EntityDataSerializers.INT);

    public HighlightedItemEntity(EntityType<? extends HighlightedItemEntity> type, Level level) {
        super(type, level);
    }

    public HighlightedItemEntity(Level level, double x, double y, double z, ItemStack stack) {
        super(level, x, y, z, stack);
    }

    public HighlightedItemEntity(Level level, double x, double y, double z, ItemStack stack, int color) {
        this(level, x, y, z, stack);
        setColor(color);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        getEntityData().define(DATA_COLOR, 0);
    }

    /** @return 当前同步的颜色，未设置时为 0 */
    public int getColor() {
        return getEntityData().get(DATA_COLOR);
    }

    public void setColor(int color) {
        getEntityData().set(DATA_COLOR, color);
    }

    public boolean hasColor() {
        return getColor() != 0;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Color", getColor());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setColor(tag.getInt("Color"));
    }

    /**
     * 掉落一个带高亮的物品实体；颜色为 0 时退化为普通掉落物。
     *
     * @return 已经加入世界的实体，堆叠为空时返回 null
     */
    public static HighlightedItemEntity drop(Level level, double x, double y, double z, ItemStack stack, int color) {
        if (level.isClientSide || stack.isEmpty()) return null;
        var entity = new HighlightedItemEntity(level, x, y, z, stack.copy(), color);
        level.addFreshEntity(entity);
        return entity;
    }
}

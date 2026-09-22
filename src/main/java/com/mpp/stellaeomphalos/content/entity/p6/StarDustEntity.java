package com.mpp.stellaeomphalos.content.entity.p6;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 星尘掉落物：落在熔融流光（molten lumen）液源上时会累积惰性合并进度。
 *
 * <p>每服务端刻检查脚下是否为熔融流光液源；是则把计数器 +1 写入持久数据键 "InertMerge"。达到
 * {@link #INERT_MERGE_TICKS} 后消耗整叠物品并移除自身（真正的产物转换由后续工作流接管，本类只负责
 * 完成"就地吸收"这一步）。物品栈为空时不做任何事，也不会抛异常。
 */
public class StarDustEntity extends ItemEntity {

    /** 完成吸收所需的持续浸泡刻数。 */
    public static final int INERT_MERGE_TICKS = 200;

    public StarDustEntity(EntityType<? extends StarDustEntity> type, Level level) {
        super(type, level);
    }

    public StarDustEntity(Level level, double x, double y, double z, ItemStack stack) {
        super(level, x, y, z, stack);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || isRemoved()) return;
        if (!onLumen()) return;
        int merged = getInertMerge() + 1;
        setInertMerge(merged);
        if (merged < INERT_MERGE_TICKS) return;
        ItemStack stack = getItem();
        if (!stack.isEmpty()) {
            stack.shrink(stack.getCount());
            setItem(stack);
        }
        discard();
    }

    /** @return 已经累积的惰性合并刻数；缺失时为 0 */
    public int getInertMerge() {
        return getPersistentData().getInt("InertMerge");
    }

    public void setInertMerge(int ticks) {
        getPersistentData().putInt("InertMerge", Math.max(0, ticks));
    }

    /** @return 脚下是否为熔融流光液源 */
    public boolean onLumen() {
        BlockPos below = blockPosition().below();
        if (!level().hasChunkAt(below)) return false;
        BlockState state = level().getBlockState(below);
        return GeodeEntity.isMoltenLumen(state);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("InertMerge", getInertMerge());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setInertMerge(tag.getInt("InertMerge"));
    }
}

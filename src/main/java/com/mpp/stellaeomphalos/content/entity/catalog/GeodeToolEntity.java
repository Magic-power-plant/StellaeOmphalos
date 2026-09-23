package com.mpp.stellaeomphalos.content.entity.catalog;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 晶体工具掉落物：与 {@link GeodeEntity} 相同的"熔融流光上生长"语义，另加永不消失开关。
 *
 * <p>{@link #setExtendedLife()} 使用原版 {@link ItemEntity#setUnlimitedLifetime()} 的 age 哨兵值，
 * 让实体跳过消失计时；开启状态记录在持久数据键 "ExtendedLife" 上，重新加载后仍然有效。工具本体不写
 * 世界，只提供 {@link #onLumen()} 供后续工作流判断。
 */
public class GeodeToolEntity extends ItemEntity {

    private boolean extended;

    public GeodeToolEntity(EntityType<? extends GeodeToolEntity> type, Level level) {
        super(type, level);
    }

    public GeodeToolEntity(Level level, double x, double y, double z, ItemStack stack) {
        super(level, x, y, z, stack);
    }

    /** 让实体永不自然消失（age 哨兵值 -32768）。 */
    public void setExtendedLife() {
        extended = true;
        setUnlimitedLifetime();
    }

    /** @return 是否已经开启了永不消失 */
    public boolean hasExtendedLife() {
        return extended;
    }

    /** @return 脚下是否为熔融流光液源 */
    public boolean onLumen() {
        BlockPos below = blockPosition().below();
        if (!level().hasChunkAt(below)) return false;
        return GeodeEntity.isMoltenLumen(level().getBlockState(below));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("ExtendedLife", extended);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.getBoolean("ExtendedLife")) setExtendedLife();
    }
}

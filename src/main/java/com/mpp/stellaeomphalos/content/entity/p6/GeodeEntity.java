package com.mpp.stellaeomphalos.content.entity.p6;

import com.mpp.stellaeomphalos.lumen.fluid.MoltenLumenContent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 晶簇掉落物标记子类：只有在熔融流光液源上方时才视为"生长中"。
 *
 * <p>本类不写世界、不改方块，只提供一个可被后续工作流查询的判定 {@link #onLumen()}。之所以单独成类，
 * 是因为原版 {@link ItemEntity} 无法表达"这块掉落物属于晶簇语义"这一信息，而后续的结晶/生长逻辑
 * 需要一个稳定的类型标记。
 */
public class GeodeEntity extends ItemEntity {

    public GeodeEntity(EntityType<? extends GeodeEntity> type, Level level) {
        super(type, level);
    }

    public GeodeEntity(Level level, double x, double y, double z, ItemStack stack) {
        super(level, x, y, z, stack);
    }

    /** @return 脚下是否为熔融流光液源；无区块或注册缺失时返回 false */
    public boolean onLumen() {
        BlockPos below = blockPosition().below();
        if (!level().hasChunkAt(below)) return false;
        return isMoltenLumen(level().getBlockState(below));
    }

    /**
     * 判定一个方块状态是否为熔融流光液源。注册表尚未就绪时 {@code get()} 会抛异常，因此这里显式
     * 捕获并按"不是流光"处理，避免在早期阶段崩溃。
     */
    public static boolean isMoltenLumen(BlockState state) {
        try {
            return state.is(MoltenLumenContent.BLOCK.get());
        } catch (RuntimeException unavailable) {
            return false;
        }
    }
}

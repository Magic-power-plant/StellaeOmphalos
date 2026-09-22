package com.mpp.stellaeomphalos.content.block;

import com.mpp.stellaeomphalos.content.world.capability.WorldCapabilities;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.common.util.FakePlayer;

import java.util.*;

/**
 * 晶簇矿（Part-6 §6.2.1.3）。
 *
 * <p>一个方块承载两个变体（§6.2.1.3 的 `GEODE` / `ASTRAL`），取代 1.12.2 的 meta：
 * `GEODE` 掉落随机晶簇，`ASTRAL`（星辉矿）掉落自身并且是水晶簇加速生长的判据。
 * 反自动化语义保留：只有 10 格内存在**真实**玩家时才产出掉落（假玩家与自动化掉落 0）。
 */
public final class GeodeOreBlock extends Block {

    /** 变体属性。 */
    public static final EnumProperty<Variant> VARIANT =
            EnumProperty.create("variant", Variant.class);

    /** 两个变体。 */
    public enum Variant implements StringRepresentable {
        /** 晶簇矿：掉落随机晶簇。 */
        GEODE,
        /** 星辉矿：掉落自身，并作为水晶簇的加速判据。 */
        ASTRAL;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public GeodeOreBlock() {
        super(Properties.of().strength(3, 9).requiresCorrectToolForDrops());
        registerDefaultState(stateDefinition.any().setValue(VARIANT, Variant.GEODE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT);
    }

    /** 星辉矿方块状态；世界生成、结构与测试共用同一构造点。 */
    public static BlockState astralState(Block block) {
        return block.defaultBlockState().setValue(VARIANT, Variant.ASTRAL);
    }

    /** 给定状态是否为星辉矿变体。 */
    public static boolean isAstral(BlockState state) {
        return state.getBlock() instanceof GeodeOreBlock
                && state.getValue(VARIANT) == Variant.ASTRAL;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        var entity = params.getOptionalParameter(LootContextParams.THIS_ENTITY);
        var at = params.getOptionalParameter(LootContextParams.ORIGIN);
        if (entity instanceof FakePlayer
                || at == null
                || params.getLevel().players().stream()
                        .noneMatch(p -> !(p instanceof FakePlayer) && p.distanceToSqr(at) <= 100))
            return List.of();
        return super.getDrops(state, params);
    }

    @Override
    public void onRemove(BlockState old, Level level, BlockPos p, BlockState next, boolean moving) {
        // 运行期晶簇索引只维护 GEODE 变体（§6.6.1 的"从索引中注销坐标"）。
        if (!next.is(this)
                && old.getValue(VARIANT) == Variant.GEODE
                && level instanceof ServerLevel server) {
            var chunk = server.getChunkSource().getChunkNow(p.getX() >> 4, p.getZ() >> 4);
            if (chunk != null)
                chunk.getCapability(WorldCapabilities.GEODES).ifPresent(i -> i.remove(p));
        }
        super.onRemove(old, level, p, next, moving);
    }
}

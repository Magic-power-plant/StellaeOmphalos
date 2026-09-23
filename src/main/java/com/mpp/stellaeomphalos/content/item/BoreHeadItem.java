package com.mpp.stellaeomphalos.content.item;

import com.mpp.stellaeomphalos.content.block.BoreHeadBlock;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 泉头物品（《方块物品实体完整清单》§6.2.2.7）。
 *
 * <p>`bore_head` 方块只暴露 `LIQUID` / `VORTEX` 两个变体；钻头档位属于实现扩展，
 * 由放置用的物品决定（石 / 铁 / 钻石三个物品 id），放置时写进 {@link BoreHeadBlock#TIER}。
 */
public final class BoreHeadItem extends BlockItem {

    private final BoreHeadBlock.BoreMode mode;
    private final BoreHeadBlock.Tier tier;

    public BoreHeadItem(Block block, BoreHeadBlock.BoreMode mode, BoreHeadBlock.Tier tier) {
        super(block, new Item.Properties());
        this.mode = mode;
        this.tier = tier;
    }

    public BoreHeadBlock.BoreMode mode() {
        return mode;
    }

    public BoreHeadBlock.Tier tier() {
        return tier;
    }

    @Override
    protected BlockState getPlacementState(BlockPlaceContext context) {
        var state = super.getPlacementState(context);
        if (state == null) return null;
        return BoreHeadBlock.configure(state, mode, tier);
    }
}

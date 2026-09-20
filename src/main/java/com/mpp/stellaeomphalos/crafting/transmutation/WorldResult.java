package com.mpp.stellaeomphalos.crafting.transmutation;

import com.google.gson.JsonObject;
import com.mpp.stellaeomphalos.constellation.attribute.CrystalAttunement;
import com.mpp.stellaeomphalos.data.codec.*;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Place block first, otherwise remove the source and spawn the item exactly once. */
public final class WorldResult {
    private final BlockState block;
    private final ItemStack item;
    private final boolean randomCrystal, halfLife;

    public WorldResult(JsonObject json) {
        block = json.has("block") || json.has("block_state") ? RecipeJson.blockState(json) : null;
        item = RecipeJson.stack(json);
        randomCrystal = RecipeJson.text(json, "kind", "").equals("random_crystal");
        halfLife = RecipeJson.flag(json, "item_half_life", false);
        if (randomCrystal && item.isEmpty())
            throw new IllegalArgumentException("Random crystal requires a crystal item");
    }

    public ItemStack display() {
        return block != null ? new ItemStack(block.getBlock()) : item.copy();
    }

    public boolean empty() {
        return block == null && item.isEmpty();
    }

    public boolean apply(ServerLevel level, BlockPos pos, BlockState expected) {
        if (!level.hasChunkAt(pos)
                || !level.getBlockState(pos).equals(expected)
                || level.getBlockEntity(pos) != null) return false;
        if (!level.setBlock(pos, block != null ? block : Blocks.AIR.defaultBlockState(), 3)
                && !expected.isAir()) return false;
        if (block == null && !item.isEmpty()) {
            var output = item.copy();
            if (randomCrystal)
                output.getOrCreateTag()
                        .put("CrystalTraits", CrystalAttunement.random(level.random).save());
            var entity =
                    new ItemEntity(
                            level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, output);
            if (halfLife) entity.lifespan /= 2;
            level.addFreshEntity(entity);
        }
        return true;
    }
}

package com.mpp.stellaeomphalos.content.block;

import com.mpp.stellaeomphalos.content.world.capability.WorldCapabilities;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.common.util.FakePlayer;

import java.util.*;

public final class GeodeOreBlock extends Block {
    public GeodeOreBlock() {
        super(Properties.of().strength(3, 9).requiresCorrectToolForDrops());
    }

    public List<ItemStack> getDrops(BlockState s, LootParams.Builder params) {
        var entity = params.getOptionalParameter(LootContextParams.THIS_ENTITY);
        var at = params.getOptionalParameter(LootContextParams.ORIGIN);
        if (entity instanceof FakePlayer
                || at == null
                || params.getLevel().players().stream()
                        .noneMatch(p -> !(p instanceof FakePlayer) && p.distanceToSqr(at) <= 100))
            return List.of();
        return super.getDrops(s, params);
    }

    public void onRemove(BlockState old, Level level, BlockPos p, BlockState next, boolean moving) {
        if (!next.is(this) && level instanceof ServerLevel server) {
            var chunk = server.getChunkSource().getChunkNow(p.getX() >> 4, p.getZ() >> 4);
            if (chunk != null)
                chunk.getCapability(WorldCapabilities.GEODES).ifPresent(i -> i.remove(p));
        }
        super.onRemove(old, level, p, next, moving);
    }
}

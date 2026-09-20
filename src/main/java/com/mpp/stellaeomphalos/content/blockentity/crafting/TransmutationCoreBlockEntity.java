package com.mpp.stellaeomphalos.content.blockentity.crafting;

import com.mpp.stellaeomphalos.constellation.sign.SignSkyService;
import com.mpp.stellaeomphalos.crafting.transmutation.WorldRecipeIndex;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The charged core illuminates its upper target; each successful conversion debits its LU buffer.
 */
public final class TransmutationCoreBlockEntity extends AbstractCraftingMachine {
    public TransmutationCoreBlockEntity(BlockPos pos, BlockState state) {
        super(CraftingContent.TRANSMUTER_ENTITY.get(), pos, state, 1);
    }

    @Override
    public String machineKind() {
        return "light_transmuter";
    }

    @Override
    protected boolean start(Player player) {
        return false;
    }

    @Override
    protected void tickMachine(ServerLevel server) {
        if (server.getGameTime() % 5 != 0 || lumenStored() == 0) return;
        var pos = worldPosition.above();
        if (!server.hasChunkAt(pos)) return;
        var state = server.getBlockState(pos);
        var signs =
                SignSkyService.activeSigns(server).stream()
                        .map(com.mpp.stellaeomphalos.constellation.sign.Sign::id)
                        .collect(java.util.stream.Collectors.toSet());
        for (var recipe : WorldRecipeIndex.of(server).transmutation(state))
            if (recipe.matches(state, lumenStored(), signs)
                    && consumeLumen(recipe.cost(), true) == recipe.cost()) {
                if (recipe.output().apply(server, pos, state)) consumeLumen(recipe.cost(), false);
                break;
            }
    }
}

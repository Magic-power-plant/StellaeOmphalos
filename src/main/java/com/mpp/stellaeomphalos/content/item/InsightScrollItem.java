package com.mpp.stellaeomphalos.content.item;

import com.mpp.stellaeomphalos.player.boon.BoonProgress;
import com.mpp.stellaeomphalos.player.progress.StarRecords;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public final class InsightScrollItem extends Item {
    public InsightScrollItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer server
                && !(server instanceof net.minecraftforge.common.util.FakePlayer)) {
            var record = StarRecords.get(server);
            if (!record.valid()) return InteractionResultHolder.fail(stack);
            if (!stack.hasTag() || !stack.getTag().contains("Insight"))
                stack.getOrCreateTag().put("Insight", record.share());
            else if (StarRecords.mutate(
                    server, r -> r.merge(stack.getTag().getCompound("Insight")))) {
                BoonProgress.getServer(server)
                        .mergeKnowledge(server, record.knownSigns(), record.seenSigns());
                stack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}

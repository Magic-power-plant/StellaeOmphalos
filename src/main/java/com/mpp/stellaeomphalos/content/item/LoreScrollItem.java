package com.mpp.stellaeomphalos.content.item;

import com.mpp.stellaeomphalos.player.boon.BoonProgress;
import com.mpp.stellaeomphalos.player.progress.StarRecords;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public final class LoreScrollItem extends Item {
    public LoreScrollItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer server
                && !(server instanceof net.minecraftforge.common.util.FakePlayer)) {
            if (player.isShiftKeyDown() && stack.hasTag() && stack.getTag().contains("Insight")) {
                var known=stack.getTag().getCompound("Insight").getList("KnownSigns", net.minecraft.nbt.Tag.TAG_STRING);
                var ids=new java.util.ArrayList<net.minecraft.resources.ResourceLocation>();
                for(int i=0;i<Math.min(128,known.size());i++) {
                    var id=net.minecraft.resources.ResourceLocation.tryParse(known.getString(i));
                    if(id!=null&&com.mpp.stellaeomphalos.constellation.sign.SignRegistry.byId(id)!=null)ids.add(id);
                }
                com.mpp.stellaeomphalos.network.SafeDispatch.send(server,
                        new com.mpp.stellaeomphalos.network.toClient.PktOpenObservation("lore_scroll",java.util.Optional.empty(),ids));
                return InteractionResultHolder.success(stack);
            }
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

package com.mpp.stellaeomphalos.content.world;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public final class AstrolabeItem extends Item {
    public AstrolabeItem() {
        super(new Item.Properties().stacksTo(1).durability(128));
    }

    public InteractionResultHolder<ItemStack> use(
            Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer server) {
            String selected = stack.hasTag() ? stack.getTag().getString("Target") : "";
            var target = ResourceLocation.tryParse(selected);
            if (player.isShiftKeyDown())
                WorldProtocol.probe(
                        server,
                        new com.mpp.stellaeomphalos.network.toServer.SpringProbePayload(
                                player.blockPosition().getX() >> 4,
                                player.blockPosition().getZ() >> 4));
            else
                WorldProtocol.query(
                        server,
                        target == null
                                ? new ResourceLocation("stellaeomphalos", "ancient_shrine")
                                : target);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}

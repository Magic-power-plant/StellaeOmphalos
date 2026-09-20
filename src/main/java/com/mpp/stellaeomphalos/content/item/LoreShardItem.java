package com.mpp.stellaeomphalos.content.item;

import com.mpp.stellaeomphalos.content.item.knowledge.KnowledgeProtocol;

import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public final class LoreShardItem extends Item {
    public LoreShardItem() {
        super(new Properties().stacksTo(1));
    }

    public static boolean seeded(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains("ShardSeed", Tag.TAG_LONG);
    }

    @Override
    public void inventoryTick(
            ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide && !seeded(stack)) stack.setCount(0);
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (!entity.level().isClientSide && !seeded(stack)) {
            entity.discard();
            return true;
        }
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer server) KnowledgeProtocol.reveal(server, hand);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
}

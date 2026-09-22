package com.mpp.stellaeomphalos.content.item;

import com.mpp.stellaeomphalos.content.item.knowledge.KnowledgeProtocol;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public final class LoreCapsuleItem extends Item {
    public LoreCapsuleItem() {
        super(new Properties().stacksTo(1).fireResistant());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer server
                && !(player instanceof net.minecraftforge.common.util.FakePlayer)) {
            var shard = KnowledgeProtocol.createShard(server);
            if (shard.isEmpty()) return InteractionResultHolder.fail(player.getItemInHand(hand));
            player.setItemInHand(hand, shard);
            level.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.GLASS_BREAK,
                    SoundSource.PLAYERS,
                    .7F,
                    1.4F);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public int getEntityLifespan(ItemStack stack, Level level) {
        return 300;
    }

    @Override
    public boolean canBeHurtBy(net.minecraft.world.damagesource.DamageSource source) {
        return !source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)
                && super.canBeHurtBy(source);
    }
}

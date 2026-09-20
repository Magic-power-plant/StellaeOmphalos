package com.mpp.stellaeomphalos.player.mantle;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;
import java.util.function.Function;

/** Optional accessories supply the actual equipped stack, not a defensive copy. */
public final class MantleSlotBridge {
    private static Function<Player, ItemStack> slot = p -> p.getItemBySlot(EquipmentSlot.CHEST);

    private MantleSlotBridge() {}

    public static void register(Function<Player, ItemStack> provider) {
        slot = Objects.requireNonNull(provider);
    }

    public static ItemStack equipped(Player player) {
        return slot.apply(player);
    }
}

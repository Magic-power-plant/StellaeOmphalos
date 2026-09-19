package com.mpp.stellaeomphalos.core.util.platform;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Capability adapter. Optional integrations supply a bridge without leaking external types. */
public final class EquipmentBridge {
    private final Function<Player, List<ItemStack>> accessoryProvider;
    public EquipmentBridge(Function<Player, List<ItemStack>> accessoryProvider) { this.accessoryProvider = accessoryProvider; }
    public static EquipmentBridge vanilla() { return new EquipmentBridge(player -> List.of()); }
    public List<ItemStack> equipped(Player player) {
        var result = new ArrayList<ItemStack>();
        player.getArmorSlots().forEach(stack -> { if (!stack.isEmpty()) result.add(stack.copy()); });
        for (var stack : accessoryProvider.apply(player)) if (!stack.isEmpty()) result.add(stack.copy());
        return List.copyOf(result);
    }
    public List<ItemStack> carried(Player player) {
        var result = new ArrayList<>(equipped(player));
        for (var stack : player.getInventory().items) if (!stack.isEmpty()) result.add(stack.copy());
        for (var stack : player.getInventory().offhand) if (!stack.isEmpty()) result.add(stack.copy());
        return List.copyOf(result);
    }
}

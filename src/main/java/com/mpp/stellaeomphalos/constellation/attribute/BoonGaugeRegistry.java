package com.mpp.stellaeomphalos.constellation.attribute;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;

import java.util.*;

public final class BoonGaugeRegistry {
    private static final Map<ResourceLocation, BoonGauge> overrides = new LinkedHashMap<>();

    private BoonGaugeRegistry() {}

    public static void register(ResourceLocation id, BoonGauge gauge) {
        if (overrides.putIfAbsent(id, gauge) != null)
            throw new IllegalArgumentException("Duplicate gauge " + id);
    }

    public static List<GaugeReading> readAll(Player player) {
        VanillaBoonBridge.refreshAll(player);
        return BoonAttributeRegistry.all().stream()
                .map(a -> overrides.getOrDefault(a.id(), p -> read(p, a)).read(player))
                .filter(
                        r ->
                                r.breakdown().addition() != 0
                                        || r.breakdown().addedMultiply() != 0
                                        || r.breakdown().stackingMultiply() != 1)
                .toList();
    }

    private static GaugeReading read(Player player, BoonAttribute attribute) {
        double base = attribute.defaultValue(),
                add = BoonValueBridge.resolve(player, attribute, BoonModifier.Mode.ADDITION),
                multiply =
                        BoonValueBridge.resolve(
                                player, attribute, BoonModifier.Mode.ADDED_MULTIPLY),
                stack =
                        BoonValueBridge.resolve(
                                player, attribute, BoonModifier.Mode.STACKING_MULTIPLY),
                result = BoonValueBridge.value(player, attribute);
        if (attribute == BoonAttributes.HARVEST_SPEED) {
            base =
                    BoonAttributeListeners.withoutHarvestSpeedBonus(
                            () ->
                                    (double)
                                            player.getDigSpeed(
                                                    Blocks.STONE.defaultBlockState(),
                                                    player.blockPosition()));
            // Harvest bonus is itself a composed multiplier applied to vanilla's dynamic sample.
            double factor = result;
            add *= base;
            result = base * factor;
        } else if (attribute instanceof VanillaBoonAttribute vanilla
                && !vanilla.vanillaAttributes().isEmpty()) {
            var instance = player.getAttribute(vanilla.vanillaAttributes().get(0));
            if (instance != null) {
                base = instance.getBaseValue();
                result = instance.getValue();
            }
        }
        return GaugeReading.evaluate(
                "boon_attribute." + attribute.id().getNamespace() + "." + attribute.id().getPath(),
                base,
                add,
                multiply,
                stack,
                result,
                "");
    }
}

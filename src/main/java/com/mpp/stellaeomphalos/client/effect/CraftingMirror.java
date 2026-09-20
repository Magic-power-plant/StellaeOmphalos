package com.mpp.stellaeomphalos.client.effect;

import com.mpp.stellaeomphalos.client.OmphalosClient;
import com.mpp.stellaeomphalos.client.event.ClientSessionCleaner;
import com.mpp.stellaeomphalos.network.toClient.PktRecipeEpoch;

import net.minecraft.resources.ResourceLocation;

import java.util.*;

/**
 * Session-cleared read-only recipe availability for recipe browsers and future effect renderers.
 */
public final class CraftingMirror {
    private static int epoch;
    private static Set<ResourceLocation> disabled = Set.of();

    private CraftingMirror() {}

    public static void attach() {
        OmphalosClient.handlers()
                .register(
                        PktRecipeEpoch.class,
                        (client, packet) -> {
                            if (packet.epoch() >= epoch) {
                                epoch = packet.epoch();
                                disabled = Set.copyOf(packet.disabledFamilies());
                            }
                        });
        ClientSessionCleaner.register(
                "crafting",
                () -> {
                    epoch = 0;
                    disabled = Set.of();
                });
    }

    public static int epoch() {
        return epoch;
    }

    public static boolean enabled(ResourceLocation family) {
        return !disabled.contains(family);
    }
}

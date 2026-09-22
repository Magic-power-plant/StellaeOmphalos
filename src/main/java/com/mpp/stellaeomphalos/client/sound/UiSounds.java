package com.mpp.stellaeomphalos.client.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;

public final class UiSounds {
    private static final java.util.Map<String, Long> LAST = new java.util.HashMap<>();

    private UiSounds() {}

    public static void play(String id) {
        long now = net.minecraft.Util.getMillis() / 50;
        if (LAST.getOrDefault(id, -1L) == now) return;
        LAST.put(id, now);
        var s =
                net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS.getValue(
                        new ResourceLocation("stellaeomphalos", id));
        if (s != null)
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(s, 1, .5F));
    }

    public static void clear() {
        LAST.clear();
    }
}

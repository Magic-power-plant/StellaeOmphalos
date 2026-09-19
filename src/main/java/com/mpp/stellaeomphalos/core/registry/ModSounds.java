package com.mpp.stellaeomphalos.core.registry;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.registries.Registries;

public final class ModSounds {
    public static final RegistryFamily<SoundEvent> ENTRIES = new RegistryFamily<>(Registries.SOUND_EVENT);
    private ModSounds() {}
}

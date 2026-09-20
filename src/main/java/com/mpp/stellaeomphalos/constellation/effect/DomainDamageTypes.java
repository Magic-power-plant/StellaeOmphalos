package com.mpp.stellaeomphalos.constellation.effect;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;

/** Damage type keys of the domain module; JSON lives in data/stellaeomphalos/damage_type/. */
public final class DomainDamageTypes {
    /** High celestial damage used by the corrupted Herd branch. */
    public static final ResourceKey<DamageType> STARLIGHT =
            ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("stellaeomphalos", "starlight"));

    private DomainDamageTypes() {}
}

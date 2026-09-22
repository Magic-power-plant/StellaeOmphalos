package com.mpp.stellaeomphalos.content.particle;

import com.mpp.stellaeomphalos.core.bootstrap.RegistrationGuard;
import com.mpp.stellaeomphalos.core.registry.ModParticles;
import com.mpp.stellaeomphalos.core.registry.ModSounds;

import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import java.util.*;

/** Common-side type declarations for Part 7; no client classes are linked here. */
public final class ClientVisualContent {
    public static final List<String> SOUND_IDS =
            List.of(
                    "altar_craft_loop",
                    "altar_craft_complete",
                    "resonance_attune",
                    "boon_unlock",
                    "boon_seal_break",
                    "codex_open",
                    "codex_close",
                    "codex_page_turn",
                    "codex_search_type",
                    "grindstone_grind_loop",
                    "grindstone_complete",
                    "infuser_craft_loop",
                    "well_liquid_loop",
                    "lumen_collect_loop",
                    "crystal_grow",
                    "crystal_fracture",
                    "ritual_start",
                    "ritual_end",
                    "ritual_fail",
                    "relay_link",
                    "relay_unlink",
                    "astrolabe_ping",
                    "astrolabe_found",
                    "sign_discover",
                    "shard_reveal",
                    "meteor_fall",
                    "meteor_impact",
                    "gateway_charge",
                    "gateway_teleport",
                    "wand_augment_switch",
                    "mantle_activate",
                    "view_sequence_whoosh");
    public static final List<String> PARTICLE_IDS =
            List.of(
                    "lumen_spark",
                    "star_dust",
                    "sign_mote",
                    "mantle_trail",
                    "grindstone_fleck",
                    "crystal_grow",
                    "transmute_charge",
                    "meteor_ash",
                    "fountain_droplet",
                    "phantom_fleck",
                    "astrolabe_ping");
    public static final Map<String, RegistrationGuard<SimpleParticleType>> PARTICLES =
            new LinkedHashMap<>();
    public static final RegistrationGuard<
                    net.minecraft.core.particles.ParticleType<ResonanceRingOptions>>
            RESONANCE_RING =
                    ModParticles.ENTRIES.declare(
                            "resonance_ring",
                            () ->
                                    new net.minecraft.core.particles.ParticleType<
                                            ResonanceRingOptions>(
                                            false, ResonanceRingOptions.DESERIALIZER) {
                                        @Override
                                        public com.mojang.serialization.Codec<ResonanceRingOptions>
                                                codec() {
                                            return ResonanceRingOptions.CODEC;
                                        }
                                    });

    private ClientVisualContent() {}

    public static SoundEvent sound(String id) {
        return java.util.Objects.requireNonNull(net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("stellaeomphalos",id)),id);
    }

    public static void initialize() {
        for (String id : SOUND_IDS) {
            var key = new ResourceLocation("stellaeomphalos", id);
            if (!ModSounds.ENTRIES.ids().contains(key))
                ModSounds.ENTRIES.declare(id, () -> SoundEvent.createVariableRangeEvent(key));
        }
        for (String id : PARTICLE_IDS)
            PARTICLES.put(
                    id, ModParticles.ENTRIES.declare(id, () -> new SimpleParticleType(false)));
    }
}

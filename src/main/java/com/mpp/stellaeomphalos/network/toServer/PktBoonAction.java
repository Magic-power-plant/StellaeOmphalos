package com.mpp.stellaeomphalos.network.toServer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.network.OmphalosPayload;

import net.minecraft.resources.ResourceLocation;

/** Append-only payload 51; business validation belongs to the receiving domain. */
public record PktBoonAction(String action, ResourceLocation node) implements OmphalosPayload {
    public static final Codec<PktBoonAction> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            Codec.STRING
                                                    .fieldOf("action")
                                                    .forGetter(PktBoonAction::action),
                                            ResourceLocation.CODEC
                                                    .fieldOf("node")
                                                    .forGetter(PktBoonAction::node))
                                    .apply(i, PktBoonAction::new));
}

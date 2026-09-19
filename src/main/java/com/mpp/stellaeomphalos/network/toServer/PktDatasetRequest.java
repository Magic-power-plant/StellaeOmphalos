package com.mpp.stellaeomphalos.network.toServer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.network.OmphalosPayload;
import net.minecraft.resources.ResourceLocation;

public record PktDatasetRequest(ResourceLocation dataset, long version) implements OmphalosPayload {
    public static final Codec<PktDatasetRequest> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("dataset").forGetter(PktDatasetRequest::dataset),
            com.mpp.stellaeomphalos.data.codec.FoundationCodecs.NONNEGATIVE_LONG.fieldOf("version").forGetter(PktDatasetRequest::version)
    ).apply(instance, PktDatasetRequest::new));
}

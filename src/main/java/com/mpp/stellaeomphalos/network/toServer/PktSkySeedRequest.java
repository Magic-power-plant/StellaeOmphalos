package com.mpp.stellaeomphalos.network.toServer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.network.OmphalosPayload;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/** C2S (payload id 11): the client asks for a dimension's sky seed. Rate-limited per session. */
public record PktSkySeedRequest(int sessionId, ResourceKey<Level> dim) implements OmphalosPayload {
    public static final Codec<ResourceKey<Level>> DIMENSION = ResourceLocation.CODEC.xmap(
            location -> ResourceKey.create(Registries.DIMENSION, location), ResourceKey::location);

    public static final Codec<PktSkySeedRequest> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("session_id").forGetter(PktSkySeedRequest::sessionId),
            DIMENSION.fieldOf("dim").forGetter(PktSkySeedRequest::dim)
    ).apply(instance, PktSkySeedRequest::new));
}

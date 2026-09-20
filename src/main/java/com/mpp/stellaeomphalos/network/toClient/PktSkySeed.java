package com.mpp.stellaeomphalos.network.toClient;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.network.OmphalosPayload;
import com.mpp.stellaeomphalos.network.toServer.PktSkySeedRequest;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** S2C (payload id 12): answers a sky seed request; stale session ids are dropped by the client. */
public record PktSkySeed(int sessionId, ResourceKey<Level> dim, long skySeed) implements OmphalosPayload {
    public static final Codec<PktSkySeed> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("session_id").forGetter(PktSkySeed::sessionId),
            PktSkySeedRequest.DIMENSION.fieldOf("dim").forGetter(PktSkySeed::dim),
            Codec.LONG.fieldOf("sky_seed").forGetter(PktSkySeed::skySeed)
    ).apply(instance, PktSkySeed::new));
}

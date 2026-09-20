package com.mpp.stellaeomphalos.network.toClient;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.network.OmphalosPayload;

import net.minecraft.nbt.CompoundTag;

/** Append-only payload 48; business validation belongs to the receiving domain. */
public record PktReaderProjection(CompoundTag data) implements OmphalosPayload {
    public static final Codec<PktReaderProjection> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            CompoundTag.CODEC
                                                    .fieldOf("data")
                                                    .forGetter(PktReaderProjection::data))
                                    .apply(i, PktReaderProjection::new));

    public PktReaderProjection {
        data = data.copy();
    }

    @Override
    public CompoundTag data() {
        return data.copy();
    }
}

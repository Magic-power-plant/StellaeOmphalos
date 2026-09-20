package com.mpp.stellaeomphalos.network.toClient;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.network.OmphalosPayload;

import net.minecraft.nbt.CompoundTag;

/** Append-only payload 50; business validation belongs to the receiving domain. */
public record PktGaugeReadings(CompoundTag data) implements OmphalosPayload {
    public static final Codec<PktGaugeReadings> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            CompoundTag.CODEC
                                                    .fieldOf("data")
                                                    .forGetter(PktGaugeReadings::data))
                                    .apply(i, PktGaugeReadings::new));

    public PktGaugeReadings {
        data = data.copy();
    }

    @Override
    public CompoundTag data() {
        return data.copy();
    }
}

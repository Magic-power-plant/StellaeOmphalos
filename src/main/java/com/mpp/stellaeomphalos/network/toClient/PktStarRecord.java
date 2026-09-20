package com.mpp.stellaeomphalos.network.toClient;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.network.OmphalosPayload;

import net.minecraft.nbt.CompoundTag;

/** Append-only payload 39; business validation belongs to the receiving domain. */
public record PktStarRecord(int session, long revision, CompoundTag data)
        implements OmphalosPayload {
    public static final Codec<PktStarRecord> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            Codec.INT
                                                    .fieldOf("session")
                                                    .forGetter(PktStarRecord::session),
                                            Codec.LONG
                                                    .fieldOf("revision")
                                                    .forGetter(PktStarRecord::revision),
                                            CompoundTag.CODEC
                                                    .fieldOf("data")
                                                    .forGetter(PktStarRecord::data))
                                    .apply(i, PktStarRecord::new));

    public PktStarRecord {
        data = data.copy();
    }

    @Override
    public CompoundTag data() {
        return data.copy();
    }
}

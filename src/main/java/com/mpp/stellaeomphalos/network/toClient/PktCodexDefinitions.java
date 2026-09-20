package com.mpp.stellaeomphalos.network.toClient;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.network.OmphalosPayload;

import net.minecraft.nbt.CompoundTag;

/** Append-only payload 46; business validation belongs to the receiving domain. */
public record PktCodexDefinitions(long epoch, CompoundTag data) implements OmphalosPayload {
    public static final Codec<PktCodexDefinitions> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            Codec.LONG
                                                    .fieldOf("epoch")
                                                    .forGetter(PktCodexDefinitions::epoch),
                                            CompoundTag.CODEC
                                                    .fieldOf("data")
                                                    .forGetter(PktCodexDefinitions::data))
                                    .apply(i, PktCodexDefinitions::new));

    public PktCodexDefinitions {
        data = data.copy();
    }

    @Override
    public CompoundTag data() {
        return data.copy();
    }
}

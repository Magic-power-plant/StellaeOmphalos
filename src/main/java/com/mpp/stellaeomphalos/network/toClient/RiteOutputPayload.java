package com.mpp.stellaeomphalos.network.toClient;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.network.OmphalosPayload;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public record RiteOutputPayload(BlockPos origin, ItemStack output, int cycleIndex)
        implements OmphalosPayload {
    public static final Codec<RiteOutputPayload> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            BlockPos.CODEC
                                                    .fieldOf("origin")
                                                    .forGetter(RiteOutputPayload::origin),
                                            ItemStack.CODEC
                                                    .fieldOf("output")
                                                    .forGetter(RiteOutputPayload::output),
                                            Codec.intRange(0, Integer.MAX_VALUE)
                                                    .fieldOf("cycleIndex")
                                                    .forGetter(RiteOutputPayload::cycleIndex))
                                    .apply(i, RiteOutputPayload::new));

    public RiteOutputPayload {
        output = output.copy();
        if (output.isEmpty() || output.getCount() > 64)
            throw new IllegalArgumentException("Output count");
    }

    public ItemStack output() {
        return output.copy();
    }
}

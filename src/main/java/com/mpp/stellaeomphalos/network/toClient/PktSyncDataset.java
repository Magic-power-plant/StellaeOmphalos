package com.mpp.stellaeomphalos.network.toClient;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.network.OmphalosPayload;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public record PktSyncDataset(ResourceLocation dataset, long version, boolean fullSnapshot, CompoundTag data) implements OmphalosPayload {
    public static final Codec<PktSyncDataset> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("dataset").forGetter(PktSyncDataset::dataset),
            com.mpp.stellaeomphalos.data.codec.FoundationCodecs.NONNEGATIVE_LONG.fieldOf("version").forGetter(PktSyncDataset::version),
            Codec.BOOL.fieldOf("full_snapshot").forGetter(PktSyncDataset::fullSnapshot),
            CompoundTag.CODEC.fieldOf("data").forGetter(PktSyncDataset::data)
    ).apply(instance, PktSyncDataset::new));
    public PktSyncDataset { data = data.copy(); }
    @Override public CompoundTag data() { return data.copy(); }
}

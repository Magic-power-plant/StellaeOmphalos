package com.mpp.stellaeomphalos.data.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/** Typed foundation data; gameplay schemas are registered by their owning modules. */
public record PolicyEntry(int schemaVersion, boolean enabled, List<ResourceLocation> targets, List<Integer> values) {
    public static final Codec<PolicyEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(1, 1).fieldOf("schema_version").forGetter(PolicyEntry::schemaVersion),
            FoundationCodecs.optional(Codec.BOOL, "enabled", true).forGetter(PolicyEntry::enabled),
            FoundationCodecs.optional(ResourceLocation.CODEC.listOf(), "targets", List.<ResourceLocation>of()).forGetter(PolicyEntry::targets),
            FoundationCodecs.optional(Codec.intRange(0, Integer.MAX_VALUE).listOf(), "values", List.<Integer>of()).forGetter(PolicyEntry::values)
    ).apply(instance, PolicyEntry::new));
    public PolicyEntry { targets = List.copyOf(targets); values = List.copyOf(values); }
}

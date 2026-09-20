package com.mpp.stellaeomphalos.network.toClient;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.network.OmphalosPayload;
import com.mpp.stellaeomphalos.network.toServer.PktSkySeedRequest;
import java.util.List;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/** S2C (payload id 14): sky anchors for the currently laid-out signs; the anchor is 10 doubles
 * (base xyz + incU xyz + incV xyz + radius) to keep the wire format free of constellation types. */
public record PktSignSkyLayout(int sessionId, ResourceKey<Level> dim, List<Entry> entries) implements OmphalosPayload {
    public record Entry(ResourceLocation sign, List<Double> anchor) {
        private static final Codec<List<Double>> ANCHOR = Codec.DOUBLE.listOf().flatXmap(Entry::check, Entry::check);
        private static DataResult<List<Double>> check(List<Double> list) {
            return list.size() == 10 ? DataResult.success(list) : DataResult.error(() -> "Anchor needs exactly 10 components");
        }
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("sign").forGetter(Entry::sign),
                ANCHOR.fieldOf("anchor").forGetter(Entry::anchor)
        ).apply(instance, Entry::new));
        public Entry { anchor = List.copyOf(anchor); }
    }

    public static final Codec<PktSignSkyLayout> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("session_id").forGetter(PktSignSkyLayout::sessionId),
            PktSkySeedRequest.DIMENSION.fieldOf("dim").forGetter(PktSignSkyLayout::dim),
            Entry.CODEC.listOf().fieldOf("entries").forGetter(PktSignSkyLayout::entries)
    ).apply(instance, PktSignSkyLayout::new));

    public PktSignSkyLayout { entries = List.copyOf(entries); }
}

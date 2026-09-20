package com.mpp.stellaeomphalos.network.toClient;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.network.OmphalosPayload;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ExtraCodecs;

/** S2C (payload id 15): server-authoritative generated names overriding local lexicon naming. */
public record PktSignRename(int sessionId, List<Entry> entries) implements OmphalosPayload {
    public record Entry(int sign, Component name) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("sign").forGetter(Entry::sign),
                ExtraCodecs.COMPONENT.fieldOf("name").forGetter(Entry::name)
        ).apply(instance, Entry::new));
    }

    public static final Codec<PktSignRename> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("session_id").forGetter(PktSignRename::sessionId),
            Entry.CODEC.listOf().fieldOf("entries").forGetter(PktSignRename::entries)
    ).apply(instance, PktSignRename::new));

    public PktSignRename { entries = List.copyOf(entries); }
}

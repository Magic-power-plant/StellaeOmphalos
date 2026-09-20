package com.mpp.stellaeomphalos.network.toClient;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.network.OmphalosPayload;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/**
 * Logical ID 16 (S2C): full boon tree sync on login / tree change. Carries the tree version,
 * every node's layout (grid coordinates + kind) and the player's applied bitmap. The client only
 * renders; it never derives prerequisites itself. Sent at most once per session plus on rebuilds.
 */
public record PktBoonTreeSync(int sessionId, int treeVersion, List<Entry> entries) implements OmphalosPayload {

    public record Entry(ResourceLocation id, short x, short z, byte kind, boolean applied) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("id").forGetter(Entry::id),
                Codec.SHORT.fieldOf("x").forGetter(Entry::x),
                Codec.SHORT.fieldOf("z").forGetter(Entry::z),
                Codec.BYTE.fieldOf("kind").forGetter(Entry::kind),
                Codec.BOOL.fieldOf("applied").forGetter(Entry::applied)
        ).apply(instance, Entry::new));
    }

    public static final Codec<PktBoonTreeSync> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("session_id").forGetter(PktBoonTreeSync::sessionId),
            Codec.INT.fieldOf("tree_version").forGetter(PktBoonTreeSync::treeVersion),
            Entry.CODEC.listOf().fieldOf("entries").forGetter(PktBoonTreeSync::entries)
    ).apply(instance, PktBoonTreeSync::new));

    public PktBoonTreeSync {
        entries = List.copyOf(entries);
    }
}

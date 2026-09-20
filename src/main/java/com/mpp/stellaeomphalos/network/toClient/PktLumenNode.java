package com.mpp.stellaeomphalos.network.toClient;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.network.OmphalosPayload;
import java.util.List;
import net.minecraft.core.BlockPos;

/** ID 20 (S2C): a lumen node entered the player's visual range; carries its link targets for line rendering. */
public record PktLumenNode(int sessionId, BlockPos pos, byte io, List<BlockPos> connections) implements OmphalosPayload {
    public static final Codec<PktLumenNode> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("session_id").forGetter(PktLumenNode::sessionId),
            BlockPos.CODEC.fieldOf("pos").forGetter(PktLumenNode::pos),
            Codec.BYTE.fieldOf("io").forGetter(PktLumenNode::io),
            BlockPos.CODEC.listOf().fieldOf("connections").forGetter(PktLumenNode::connections))
            .apply(instance, PktLumenNode::new));
    public PktLumenNode {
        pos = pos.immutable();
        connections = List.copyOf(connections);
    }
}

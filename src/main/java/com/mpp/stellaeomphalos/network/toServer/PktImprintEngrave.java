package com.mpp.stellaeomphalos.network.toServer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.network.OmphalosPayload;
import java.util.List;

/** Logical ID 23 (C2S): drawing-table engrave request; the server recomputes coverage from these strokes. */
public record PktImprintEngrave(int sessionId, net.minecraft.resources.ResourceLocation dimension,
        net.minecraft.core.BlockPos origin, int slot, List<Stroke> strokes) implements OmphalosPayload {
    public static final int MAX_STROKES = 64;
    public static final int GRID_LIMIT = 30;

    public record Stroke(int signId, int gridX, int gridZ) {
        public static final Codec<Stroke> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.intRange(0, Integer.MAX_VALUE).fieldOf("sign_id").forGetter(Stroke::signId),
                Codec.intRange(0, GRID_LIMIT).fieldOf("grid_x").forGetter(Stroke::gridX),
                Codec.intRange(0, GRID_LIMIT).fieldOf("grid_z").forGetter(Stroke::gridZ)
        ).apply(instance, Stroke::new));
    }

    public PktImprintEngrave { strokes = List.copyOf(strokes); }

    public static final Codec<PktImprintEngrave> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("session_id", -1).forGetter(PktImprintEngrave::sessionId),
            net.minecraft.resources.ResourceLocation.CODEC.optionalFieldOf("dimension",
                    new net.minecraft.resources.ResourceLocation("minecraft", "overworld")).forGetter(PktImprintEngrave::dimension),
            net.minecraft.core.BlockPos.CODEC.optionalFieldOf("origin", net.minecraft.core.BlockPos.ZERO).forGetter(PktImprintEngrave::origin),
            Codec.intRange(-1, 8).optionalFieldOf("slot", -1).forGetter(PktImprintEngrave::slot),
            Stroke.CODEC.listOf().flatXmap(PktImprintEngrave::checkSize, PktImprintEngrave::checkSize)
                    .fieldOf("strokes").forGetter(PktImprintEngrave::strokes)
    ).apply(i, PktImprintEngrave::new));

    private static DataResult<List<Stroke>> checkSize(List<Stroke> strokes) {
        return strokes.size() <= MAX_STROKES ? DataResult.success(strokes)
                : DataResult.error(() -> "Too many imprint strokes " + strokes.size());
    }
}

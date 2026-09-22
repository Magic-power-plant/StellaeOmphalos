package com.mpp.stellaeomphalos.content.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;

/** Radius and packed color are synchronized once; expansion is entirely client-side. */
public record ResonanceRingOptions(float radius, int color) implements ParticleOptions {
    public ResonanceRingOptions {
        if (!Float.isFinite(radius) || radius < .05F || radius > 16)
            throw new IllegalArgumentException("Invalid ring radius");
    }

    public static final Codec<ResonanceRingOptions> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            Codec.floatRange(.05F, 16)
                                                    .fieldOf("radius")
                                                    .forGetter(ResonanceRingOptions::radius),
                                            Codec.INT
                                                    .fieldOf("color")
                                                    .forGetter(ResonanceRingOptions::color))
                                    .apply(i, ResonanceRingOptions::new));
    public static final Deserializer<ResonanceRingOptions> DESERIALIZER =
            new Deserializer<>() {
                public ResonanceRingOptions fromCommand(
                        ParticleType<ResonanceRingOptions> type, StringReader reader)
                        throws CommandSyntaxException {
                    reader.expect(' ');
                    float radius = reader.readFloat();
                    reader.expect(' ');
                    int color = reader.readInt();
                    if (!Float.isFinite(radius) || radius < .05F || radius > 16)
                        throw com.mojang.brigadier.exceptions.CommandSyntaxException
                                .BUILT_IN_EXCEPTIONS
                                .readerInvalidFloat()
                                .createWithContext(reader, radius);
                    return new ResonanceRingOptions(radius, color);
                }

                public ResonanceRingOptions fromNetwork(
                        ParticleType<ResonanceRingOptions> type, FriendlyByteBuf buffer) {
                    return new ResonanceRingOptions(buffer.readFloat(), buffer.readInt());
                }
            };

    public ParticleType<?> getType() {
        return ClientVisualContent.RESONANCE_RING.get();
    }

    public void writeToNetwork(FriendlyByteBuf buffer) {
        buffer.writeFloat(radius);
        buffer.writeInt(color);
    }

    public String writeToString() {
        return String.format(
                java.util.Locale.ROOT, "stellaeomphalos:resonance_ring %.3f %d", radius, color);
    }
}

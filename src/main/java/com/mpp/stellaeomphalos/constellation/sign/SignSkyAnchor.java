package com.mpp.stellaeomphalos.constellation.sign;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;

/** Sky-sphere patch geometry for one sign: base point, U/V tangent increment vectors and radius. */
public record SignSkyAnchor(double baseX, double baseY, double baseZ,
                            double incUx, double incUy, double incUz,
                            double incVx, double incVy, double incVz,
                            double radius) {
    private static final Codec<List<Double>> VECTOR = Codec.DOUBLE.listOf().flatXmap(SignSkyAnchor::checkVector, SignSkyAnchor::checkVector);

    private static DataResult<List<Double>> checkVector(List<Double> list) {
        return list.size() == 3 ? DataResult.success(list) : DataResult.error(() -> "Vector needs exactly 3 components");
    }

    public static final Codec<SignSkyAnchor> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            VECTOR.fieldOf("base").forGetter(anchor -> List.of(anchor.baseX(), anchor.baseY(), anchor.baseZ())),
            VECTOR.fieldOf("inc_u").forGetter(anchor -> List.of(anchor.incUx(), anchor.incUy(), anchor.incUz())),
            VECTOR.fieldOf("inc_v").forGetter(anchor -> List.of(anchor.incVx(), anchor.incVy(), anchor.incVz())),
            Codec.DOUBLE.fieldOf("radius").forGetter(SignSkyAnchor::radius)
    ).apply(instance, (base, u, v, radius) -> new SignSkyAnchor(
            base.get(0), base.get(1), base.get(2),
            u.get(0), u.get(1), u.get(2),
            v.get(0), v.get(1), v.get(2), radius)));

    /** Encoded form used by the layout payload: base(3) + incU(3) + incV(3) + radius. */
    public List<Double> asList() {
        return List.of(baseX, baseY, baseZ, incUx, incUy, incUz, incVx, incVy, incVz, radius);
    }

    public static SignSkyAnchor fromList(List<Double> values) {
        if (values.size() != 10) throw new IllegalArgumentException("Anchor needs exactly 10 components");
        return new SignSkyAnchor(values.get(0), values.get(1), values.get(2), values.get(3), values.get(4),
                values.get(5), values.get(6), values.get(7), values.get(8), values.get(9));
    }
}

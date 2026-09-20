package com.mpp.stellaeomphalos.constellation.sign;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.data.codec.FoundationCodecs;
import com.mpp.stellaeomphalos.data.loader.DataBootstrap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The 15 sky slots (5 major near the zenith, 10 minor farther out), loaded from the
 * sign_sky_anchors data table. {@link #layout(List)} requires the input to be pre-sorted
 * with major signs first; trait signs never occupy a slot.
 */
public final class SignSkyAnchorTable {
    public record SlotTable(List<SignSkyAnchor> majorSlots, List<SignSkyAnchor> minorSlots) {
        public static final Codec<SlotTable> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                FoundationCodecs.optional(Codec.intRange(1, 1), "schema_version", 1).forGetter(table -> 1),
                SignSkyAnchor.CODEC.listOf().fieldOf("major_slots").forGetter(SlotTable::majorSlots),
                SignSkyAnchor.CODEC.listOf().fieldOf("minor_slots").forGetter(SlotTable::minorSlots)
        ).apply(instance, (version, majorSlots, minorSlots) -> new SlotTable(majorSlots, minorSlots)));
        public SlotTable {
            majorSlots = List.copyOf(majorSlots);
            minorSlots = List.copyOf(minorSlots);
        }
    }

    private SignSkyAnchorTable() {}

    /** The published table snapshot; empty before the first successful data load. */
    public static Optional<SlotTable> table() {
        return DataBootstrap.TABLES.entries(SignBootstrap.SKY_ANCHORS).values().stream().findFirst();
    }

    /** Assigns slots in input order: majors take major slots, other non-trait signs take minor slots. */
    public static Map<Sign, SignSkyAnchor> layout(List<Sign> activeSigns) {
        var slots = table();
        if (slots.isEmpty()) return Map.of();
        var major = slots.get().majorSlots();
        var minor = slots.get().minorSlots();
        var result = new LinkedHashMap<Sign, SignSkyAnchor>();
        int majorIndex = 0;
        int minorIndex = 0;
        for (var sign : activeSigns) {
            if (sign instanceof TraitSign) continue;
            if (sign instanceof MajorSign) {
                if (majorIndex < major.size()) result.put(sign, major.get(majorIndex++));
            } else if (minorIndex < minor.size()) {
                result.put(sign, minor.get(minorIndex++));
            }
        }
        return java.util.Collections.unmodifiableMap(result);
    }
}

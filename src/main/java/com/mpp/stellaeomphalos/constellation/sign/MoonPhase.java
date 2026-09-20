package com.mpp.stellaeomphalos.constellation.sign;

import com.mojang.serialization.Codec;
import java.util.Locale;

/** The eight moon phases in vanilla order; the ordinal sequence must never change. */
public enum MoonPhase {
    FULL, WANING_3_4, WANING_1_2, WANING_1_4, NEW, WAXING_1_4, WAXING_1_2, WAXING_3_4;

    public static final Codec<MoonPhase> CODEC = Codec.STRING.xmap(MoonPhase::byName,
            phase -> phase.name().toLowerCase(Locale.ROOT));

    /** The phase visible on the given world day; matches the vanilla moon cycle. */
    public static MoonPhase ofDay(long day) { return values()[(int) Math.floorMod(day, 8)]; }

    public static MoonPhase byName(String name) {
        try { return valueOf(name.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException exception) { throw new IllegalArgumentException("Unknown moon phase " + name); }
    }
}

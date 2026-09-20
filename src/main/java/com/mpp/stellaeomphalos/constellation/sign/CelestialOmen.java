package com.mpp.stellaeomphalos.constellation.sign;

import com.mojang.serialization.Codec;
import java.util.Locale;

/** Celestial omen kinds; absence is represented by null/empty, never by a NONE value. */
public enum CelestialOmen {
    SOLAR_ECLIPSE, LUNAR_ECLIPSE;

    public static final Codec<CelestialOmen> CODEC = Codec.STRING.xmap(CelestialOmen::byName,
            omen -> omen.name().toLowerCase(Locale.ROOT));

    public static CelestialOmen byName(String name) {
        try { return valueOf(name.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException exception) { throw new IllegalArgumentException("Unknown celestial omen " + name); }
    }
}

package com.mpp.stellaeomphalos.core.platform;

/** Stable names are the wire/storage contract; ordering is only used in memory. */
public enum StarTier {
    DISCOVERY,
    BASIC_CRAFT,
    ATTUNEMENT,
    CONSTELLATION,
    RADIANCE,
    BRILLIANCE;

    public boolean reaches(StarTier required) {
        return compareTo(required) >= 0;
    }

    public boolean hasNext() {
        return this != BRILLIANCE;
    }

    public StarTier next() {
        if (!hasNext()) throw new IllegalStateException("Already at highest star tier");
        return values()[ordinal() + 1];
    }

    public static StarTier parse(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return DISCOVERY;
        }
    }
}

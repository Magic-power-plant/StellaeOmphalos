package com.mpp.stellaeomphalos.knowledge.research;

import com.mpp.stellaeomphalos.core.platform.StarTier;

import java.util.Set;

public enum StudyBranch {
    DISCOVERY,
    BASIC_CRAFT,
    ATTUNEMENT,
    CONSTELLATION,
    RADIANCE,
    BRILLIANCE;

    public StarTier requiredTier() {
        return StarTier.valueOf(name());
    }

    public Set<StudyBranch> prerequisites() {
        return this == DISCOVERY ? Set.of() : Set.of(values()[ordinal() - 1]);
    }
}

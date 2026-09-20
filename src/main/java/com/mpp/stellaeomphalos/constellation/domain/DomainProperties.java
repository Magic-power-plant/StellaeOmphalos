package com.mpp.stellaeomphalos.constellation.domain;

import com.mpp.stellaeomphalos.constellation.sign.TraitSign;
import javax.annotation.Nullable;

/**
 * Immutable effect parameters for one domain tick: size / potency / amplifier / corruption flag /
 * fracture lower bound / fracture rate.
 *
 * <p>{@link #modify(TraitSign)} applies the trait-sign multiplier row from the {@code domain_traits}
 * data table. <b>Warning: repeated {@code modify} calls accumulate multiplicatively</b> — the caller
 * (Part-4 pedestal scheduler or {@link DomainEffectRunner}) must construct the base record and call
 * {@code modify} exactly once per tick; chaining twice would square the trait scales.
 */
public record DomainProperties(double size, double potency, double effectAmplifier,
                               boolean corrupted, double fractureLower, double fractureRate) {
    public DomainProperties modify(@Nullable TraitSign trait) {
        if (trait == null) return this;
        var scale = DomainTraits.modifier(trait.id());
        return new DomainProperties(size * scale.sizeScale(), potency * scale.potencyScale(),
                effectAmplifier * scale.amplifierScale(), corrupted || scale.corrupted(),
                fractureLower * scale.fractureLowerScale(), fractureRate * scale.fractureRateScale());
    }
}

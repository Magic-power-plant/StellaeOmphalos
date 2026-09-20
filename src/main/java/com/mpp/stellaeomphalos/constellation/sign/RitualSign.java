package com.mpp.stellaeomphalos.constellation.sign;

import com.mpp.stellaeomphalos.constellation.domain.DomainEffect;
import com.mpp.stellaeomphalos.constellation.domain.DomainOrigin;
import javax.annotation.Nullable;

/** A sign that can power a ritual domain effect. */
public interface RitualSign extends Sign {
    @Nullable DomainEffect ritualEffect(DomainOrigin origin);
}

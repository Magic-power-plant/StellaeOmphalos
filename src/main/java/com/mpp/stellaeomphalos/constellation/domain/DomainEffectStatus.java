package com.mpp.stellaeomphalos.constellation.domain;

/**
 * Status channel for effects that do not consume starlight strength: they only report whether the
 * domain is currently in effect (used by Lumina and Soar). The mirror count rides on the context.
 */
public interface DomainEffectStatus {
    boolean isActive(DomainContext ctx);
}

package com.mpp.stellaeomphalos.ritual.effect;

import com.mpp.stellaeomphalos.constellation.domain.*;

import net.minecraft.resources.ResourceLocation;

/** Downward adapter keeps constellation free of imports from the ritual layer. */
public final class DomainRiteEffect implements RiteEffect {
    private final ResourceLocation id;

    public DomainRiteEffect(ResourceLocation id) {
        this.id = id;
    }

    public ResourceLocation id() {
        return id;
    }

    public int suggestedInterval() {
        return 20;
    }

    public int budgetWeight() {
        return 1;
    }

    public int positionCost() {
        return 256;
    }

    private DomainContext domain(RiteEffectContext c) {
        return new DomainContext(c.level(), c.origin(), c.owner(), c.amplifierCount());
    }

    public void onRiteStart(RiteEffectContext c) {}

    public void onRiteResume(RiteEffectContext c) {}

    public void onRiteTick(RiteEffectContext c, int elapsed) {
        var effect = DomainEffectRegistry.bySign(id);
        if (effect == null || !DomainEffectRunner.enabled()) return;
        var properties = effect.provideProperties(c.amplifierCount()).modify(null);
        try (var budget = DomainWorkBudget.begin(c.positionBudget())) {
            effect.play(
                    domain(c),
                    c.intensity(),
                    new DomainProperties(
                            c.radius(),
                            properties.potency(),
                            properties.effectAmplifier(),
                            properties.corrupted(),
                            properties.fractureLower(),
                            properties.fractureRate()));
        }
    }

    public void onRiteSuspend(RiteEffectContext c) {
        var effect = DomainEffectRegistry.bySign(id);
        if (effect != null) effect.suspend(domain(c));
    }

    public void onRiteEnd(RiteEffectContext c, EndReason reason) {
        onRiteSuspend(c);
    }

    public void onDetach(RiteEffectContext c) {
        var effect = DomainEffectRegistry.bySign(id);
        if (effect != null) effect.detach(domain(c));
    }
}

package com.mpp.stellaeomphalos.ritual.effect;

import net.minecraft.resources.ResourceLocation;

public interface RiteEffect {
    enum EndReason {
        HOST_REMOVED,
        RELOADED,
        WORLD_UNLOADED,
        FAILED
    }

    ResourceLocation id();

    int suggestedInterval();

    int budgetWeight();

    int positionCost();

    void onRiteStart(RiteEffectContext context);

    void onRiteTick(RiteEffectContext context, int elapsedTicks);

    void onRiteSuspend(RiteEffectContext context);

    void onRiteResume(RiteEffectContext context);

    void onRiteEnd(RiteEffectContext context, EndReason reason);

    void onDetach(RiteEffectContext context);
}

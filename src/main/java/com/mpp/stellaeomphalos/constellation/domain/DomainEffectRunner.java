package com.mpp.stellaeomphalos.constellation.domain;

import com.mpp.stellaeomphalos.OmphalosConfig;
import com.mpp.stellaeomphalos.constellation.sign.RitualSign;
import com.mpp.stellaeomphalos.constellation.sign.TraitSign;
import javax.annotation.Nullable;

/**
 * The tool 《星构多方块与星仪世界生成》 pedestal schedulers call: given the pedestal position and the driving sign,
 * executes one {@code play}. Discipline guaranteed here (plan 2.2.6.4): the {@link DomainProperties}
 * base is constructed and {@code modify} applied exactly once per invocation — callers must invoke
 * {@link #run} at most once per tick per pedestal.
 *
 * <p>AC-2.23: when {@code gameplay.domainEffectsEnabled=false} every call short-circuits.
 */
public final class DomainEffectRunner {
    private DomainEffectRunner() {}

    public static boolean enabled() { return OmphalosConfig.SERVER.flag("gameplay.domainEffectsEnabled"); }

    public static boolean run(DomainContext ctx, RitualSign sign, float strength, @Nullable TraitSign trait) {
        if (!enabled()) return false;
        var effect = sign.ritualEffect(new DomainOrigin(ctx.level(), ctx.origin()));
        if (effect == null) {
            // Signs are rebuilt on datapack reload; lazily rebind before giving up.
            DomainEffectRegistry.bindToSigns();
            effect = sign.ritualEffect(new DomainOrigin(ctx.level(), ctx.origin()));
            if (effect == null) return false;
        }
        var props = effect.provideProperties(ctx.mirrorCount()).modify(trait);
        return effect.play(ctx, strength, props);
    }

    /** Status-channel query (Lumina/Soar); false when disabled or the effect is not a status effect. */
    public static boolean statusActive(DomainContext ctx, RitualSign sign) {
        if (!enabled()) return false;
        var effect = sign.ritualEffect(new DomainOrigin(ctx.level(), ctx.origin()));
        return effect instanceof DomainEffectStatus status && status.isActive(ctx);
    }
}

package com.mpp.stellaeomphalos.constellation.domain;

import com.mpp.stellaeomphalos.constellation.sign.MajorSign;

import net.minecraft.util.RandomSource;

import java.util.UUID;
import java.util.function.Function;

import javax.annotation.Nullable;

/**
 * Contract of one constellation domain effect. Implementations run world mutations on the server
 * only; clients receive particle parameters through {@code PktDomainParticle} and may additionally
 * implement {@link #playClient(DomainContext)}.
 *
 * <p>{@link #play} may be invoked several times within one tick (several pedestals sharing the sign
 * instance); implementations must throttle/dedupe themselves via {@link #strengthGate} and the
 * frequency declared per effect.
 *
 * <p>Relay resolution: if the origin position hosts a "star-structure link block" (Part-4), the
 * registered relay resolver redirects the origin to the linked target. The resolver defaults to
 * identity; Part-4 installs it through {@link #registerRelayResolver}.
 */
public abstract class DomainEffect {
    protected final @Nullable MajorSign owner;
    private static volatile Function<DomainOrigin, DomainOrigin> relayResolver = origin -> origin;

    protected DomainEffect(@Nullable MajorSign owner) {
        this.owner = owner;
    }

    public abstract boolean play(DomainContext ctx, float strength, DomainProperties props);

    public abstract DomainProperties provideProperties(int mirrorCount);

    public void playClient(DomainContext ctx) {}

    public void suspend(DomainContext ctx) {}

    public void detach(DomainContext ctx) {
        suspend(ctx);
    }

    /** Resolves relay links; null-safe, never returns null. */
    protected DomainOrigin resolveOrigin(DomainContext ctx) {
        var resolved = relayResolver.apply(new DomainOrigin(ctx.level(), ctx.origin()));
        return resolved != null ? resolved : new DomainOrigin(ctx.level(), ctx.origin());
    }

    /** Backtracks the owning player of the driving pedestal; null when ownerless. */
    protected @Nullable UUID owningPlayer(DomainContext ctx) {
        return ctx.owningPlayer();
    }

    /** Uniform throttle for strength < 1: execute this tick with probability {@code strength}. */
    protected static boolean strengthGate(float strength, RandomSource random) {
        return strength >= 1.0F || random.nextFloat() < Math.max(strength, 0.0F);
    }

    /** Part-4 hook: redirect origins standing on star-structure link blocks. Must be total. */
    public static void registerRelayResolver(Function<DomainOrigin, DomainOrigin> resolver) {
        relayResolver = java.util.Objects.requireNonNull(resolver);
    }
}

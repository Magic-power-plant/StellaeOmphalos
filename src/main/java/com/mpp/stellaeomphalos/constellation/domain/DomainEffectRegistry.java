package com.mpp.stellaeomphalos.constellation.domain;

import com.mojang.logging.LogUtils;
import com.mpp.stellaeomphalos.constellation.effect.DomainEffectAegis;
import com.mpp.stellaeomphalos.constellation.effect.DomainEffectAngling;
import com.mpp.stellaeomphalos.constellation.effect.DomainEffectChronos;
import com.mpp.stellaeomphalos.constellation.effect.DomainEffectCrucible;
import com.mpp.stellaeomphalos.constellation.effect.DomainEffectHerd;
import com.mpp.stellaeomphalos.constellation.effect.DomainEffectLumina;
import com.mpp.stellaeomphalos.constellation.effect.DomainEffectPetrogenesis;
import com.mpp.stellaeomphalos.constellation.effect.DomainEffectSeverance;
import com.mpp.stellaeomphalos.constellation.effect.DomainEffectSoar;
import com.mpp.stellaeomphalos.constellation.effect.DomainEffectSpawn;
import com.mpp.stellaeomphalos.constellation.effect.DomainEffectUpheaval;
import com.mpp.stellaeomphalos.constellation.effect.DomainEffectVerdance;
import com.mpp.stellaeomphalos.constellation.sign.AbstractSign;
import com.mpp.stellaeomphalos.constellation.sign.MajorSign;
import com.mpp.stellaeomphalos.constellation.sign.SignRegistry;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;

/**
 * Effect registry: sign id -> authoritative (server) effect instance, plus a client rendering
 * instance table (owner == null, only {@code playClient} semantics). {@link #initialize()} wires
 * the twelve built-in effects; {@link #register} is the open hook for external additions and throws
 * on duplicates. Binding onto sign instances is redone after every datapack reload because the
 * sign module replaces sign objects wholesale.
 */
public final class DomainEffectRegistry {
    private static final Map<ResourceLocation, DomainEffect> PROVIDER_MAP = new LinkedHashMap<>();
    private static final Map<ResourceLocation, DomainEffect> CLIENT_RENDERERS = new LinkedHashMap<>();
    private static boolean initialized;

    private DomainEffectRegistry() {}

    /** Open registration hook; duplicate sign ids fail fast. */
    public static synchronized void register(ResourceLocation signId, DomainEffect effect) {
        if (PROVIDER_MAP.putIfAbsent(signId, effect) != null)
            throw new IllegalStateException("Duplicate domain effect for " + signId);
    }

    /** Registers the client rendering instance (owner == null); duplicates throw. */
    public static synchronized void registerClientRenderer(ResourceLocation signId, DomainEffect renderer) {
        if (renderer.owner != null) throw new IllegalArgumentException("Client renderer must be ownerless: " + signId);
        if (CLIENT_RENDERERS.putIfAbsent(signId, renderer) != null)
            throw new IllegalStateException("Duplicate domain client renderer for " + signId);
    }

    public static synchronized @Nullable DomainEffect bySign(ResourceLocation signId) { return PROVIDER_MAP.get(signId); }
    public static synchronized @Nullable DomainEffect clientRenderer(ResourceLocation signId) { return CLIENT_RENDERERS.get(signId); }
    public static synchronized Map<ResourceLocation, DomainEffect> effects() { return Map.copyOf(PROVIDER_MAP); }

    /** The twelve built-ins and their bound sign ids (plan 2.2.6.2). */
    private static final Map<String, Function<MajorSign, DomainEffect>> BUILTIN = Map.ofEntries(
            Map.entry("aevitas", DomainEffectVerdance::new),
            Map.entry("armara", DomainEffectAegis::new),
            Map.entry("bootes", DomainEffectHerd::new),
            Map.entry("discidia", DomainEffectSeverance::new),
            Map.entry("evorsio", DomainEffectUpheaval::new),
            Map.entry("fornax", DomainEffectCrucible::new),
            Map.entry("horologium", DomainEffectChronos::new),
            Map.entry("lucerna", DomainEffectLumina::new),
            Map.entry("mineralis", DomainEffectPetrogenesis::new),
            Map.entry("octans", DomainEffectAngling::new),
            Map.entry("pelotrio", DomainEffectSpawn::new),
            Map.entry("vicio", DomainEffectSoar::new));

    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        BUILTIN.forEach((path, ctor) -> {
            var signId = new ResourceLocation(com.mpp.stellaeomphalos.Omphalos.MODID, path);
            register(signId, ctor.apply(null));
            registerClientRenderer(signId, ctor.apply(null));
        });
    }

    /**
     * Binds each registered effect onto its sign instance via {@code bindRitualEffect}. Safe to
     * call repeatedly; invoked at common setup and after every sign-registry rebuild.
     */
    public static synchronized void bindToSigns() {
        Map<ResourceLocation, DomainEffect> snapshot;
        synchronized (DomainEffectRegistry.class) { snapshot = Map.copyOf(PROVIDER_MAP); }
        snapshot.forEach((signId, effect) -> {
            var sign = SignRegistry.byId(signId);
            if (sign instanceof AbstractSign.Major major) major.bindRitualEffect(effect);
            else if (sign instanceof AbstractSign.Ritual ritual) ritual.bindRitualEffect(effect);
            else if (sign == null) LogUtils.getLogger().debug("Domain effect {} awaits its sign", signId);
            else LogUtils.getLogger().warn("Domain effect {} targets a non-ritual sign", signId);
        });
    }

    /** Test hook: drop all registrations. */
    public static synchronized void resetForTesting() {
        PROVIDER_MAP.clear();
        CLIENT_RENDERERS.clear();
        initialized = false;
    }
}

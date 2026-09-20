package com.mpp.stellaeomphalos.player.mantle;

import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.function.*;

/** Effects are bound by data ids, not by inheritance from celestial signs. */
public final class MantleRegistry {
    public enum Kind {
        GROWTH,
        GUARDIAN,
        HERDER,
        RETORT,
        RUIN,
        HEARTH,
        HOROLOGE,
        LANTERN,
        PROSPECTOR,
        TIDE,
        ARTIFICER,
        SWIFT
    }

    @FunctionalInterface
    public interface Factory {
        MantleEffect create(MantleState state, MantleParameters parameters);
    }

    private static final Map<ResourceLocation, Factory> FACTORIES = new LinkedHashMap<>();
    private static final Map<String, Kind> SIGNS =
            Map.ofEntries(
                    Map.entry("aevitas", Kind.GROWTH),
                    Map.entry("armara", Kind.GUARDIAN),
                    Map.entry("bootes", Kind.HERDER),
                    Map.entry("discidia", Kind.RETORT),
                    Map.entry("evorsio", Kind.RUIN),
                    Map.entry("fornax", Kind.HEARTH),
                    Map.entry("horologium", Kind.HOROLOGE),
                    Map.entry("lucerna", Kind.LANTERN),
                    Map.entry("mineralis", Kind.PROSPECTOR),
                    Map.entry("octans", Kind.TIDE),
                    Map.entry("pelotrio", Kind.ARTIFICER),
                    Map.entry("vicio", Kind.SWIFT));

    static {
        for (var kind : Kind.values())
            register(id(kind), (state, parameters) -> new StatefulEffect(kind, state, parameters));
    }

    private MantleRegistry() {}

    public static ResourceLocation id(Kind kind) {
        return new ResourceLocation(
                "stellaeomphalos", "mantle_" + kind.name().toLowerCase(Locale.ROOT));
    }

    public static void register(ResourceLocation id, Factory factory) {
        if (FACTORIES.putIfAbsent(id, factory) != null)
            throw new IllegalArgumentException("Duplicate mantle effect " + id);
    }

    public static Optional<Kind> forSign(ResourceLocation sign) {
        return Optional.ofNullable(SIGNS.get(sign.getPath()));
    }

    public static Optional<Kind> kind(ResourceLocation id) {
        return Arrays.stream(Kind.values()).filter(k -> id(k).equals(id)).findFirst();
    }

    public static Set<ResourceLocation> ids() {
        return Set.copyOf(FACTORIES.keySet());
    }

    public static MantleEffect create(MantleState state, MantleParameters parameters) {
        var factory = FACTORIES.get(state.effect());
        if (factory == null) throw new IllegalArgumentException("Unknown mantle " + state.effect());
        return factory.create(state, parameters);
    }

    private static final class StatefulEffect extends MantleEffect {
        private final Kind kind;
        private final MantleParameters params;

        StatefulEffect(Kind kind, MantleState state, MantleParameters params) {
            super(state);
            this.kind = kind;
            this.params = params;
        }

        protected DamageIntercept preview(MantleAction action) {
            float damage = Math.max(0, action.damage()), heal = 0;
            boolean immune = false;
            if (action.kind() == MantleAction.Kind.HURT
                    && kind == Kind.GUARDIAN
                    && state.getInt("Stacks") > 0) {
                damage = 0;
                immune = true;
            }
            if (action.kind() == MantleAction.Kind.HURT && kind == Kind.HEARTH && action.fire()) {
                heal = (float) (damage * params.reduction());
                damage -= heal;
            }
            if (action.kind() == MantleAction.Kind.ATTACK
                    && kind == Kind.RETORT
                    && action.tick() - state.getLong("LastHitAt") <= params.retortTicks()
                    && action.tick() >= state.getLong("LastHitAt"))
                damage += (float) (state.getFloat("LastHit") * params.retortScale());
            if (action.kind() == MantleAction.Kind.TICK && kind == Kind.TIDE && action.water())
                heal = (float) params.healing();
            return new DamageIntercept(damage, heal, immune);
        }

        protected void commit(MantleAction action, DamageIntercept result) {
            if (kind == Kind.GUARDIAN) {
                if (result.immune())
                    state.putInt("Stacks", Math.max(0, state.getInt("Stacks") - 1));
                if (action.kind() == MantleAction.Kind.TICK) {
                    int recharge = state.getInt("RechargeTicks") + 1;
                    if (recharge >= params.rechargeTicks()) {
                        recharge = 0;
                        state.putInt(
                                "Stacks", Math.min(params.maxStacks(), state.getInt("Stacks") + 1));
                    }
                    state.putInt("RechargeTicks", recharge);
                }
            }
            if (kind == Kind.RETORT) {
                if (action.kind() == MantleAction.Kind.HURT) {
                    state.putFloat("LastHit", Math.max(0, action.damage()));
                    state.putLong("LastHitAt", action.tick());
                } else if (action.tick() - state.getLong("LastHitAt") > params.retortTicks())
                    state.putFloat("LastHit", 0);
            }
        }
    }
}

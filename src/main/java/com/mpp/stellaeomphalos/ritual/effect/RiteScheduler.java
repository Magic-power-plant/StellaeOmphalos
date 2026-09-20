package com.mpp.stellaeomphalos.ritual.effect;

import com.mpp.stellaeomphalos.OmphalosConfig;
import com.mpp.stellaeomphalos.constellation.domain.DomainEffectRegistry;
import com.mpp.stellaeomphalos.ritual.rite.*;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

import java.util.*;
import java.util.function.BooleanSupplier;

/** Per-world sessions retain deferred work and release every external resource on detach. */
public final class RiteScheduler {
    private record Key(BlockPos origin, ResourceLocation effect) {}

    private static final class Runtime {
        final RiteEffectDispatcher dispatcher = new RiteEffectDispatcher();
        final Map<Key, Session> sessions = new HashMap<>();
    }

    private static final Map<ServerLevel, Runtime> LEVELS = new IdentityHashMap<>();

    private RiteScheduler() {}

    private static final class Context implements RiteEffectContext {
        private final ServerLevel level;
        private final BlockPos origin;
        private final UUID owner;
        private final int amplifiers;
        private final float intensity;
        private final RiteRecipe recipe;
        private final double radius;
        private AffectedRegion region;

        Context(
                ServerLevel l,
                BlockPos p,
                UUID owner,
                int amps,
                float intensity,
                RiteRecipe recipe,
                ResourceLocation effect) {
            level = l;
            origin = p.immutable();
            this.owner = owner;
            amplifiers = amps;
            this.intensity = intensity;
            this.recipe = recipe;
            String radiusKey = "radius";
            double value =
                    recipe.effectParams().contains(radiusKey)
                            ? recipe.effectParams().getDouble(radiusKey)
                            : DomainEffectRegistry.bySign(effect) == null
                                    ? 6
                                    : DomainEffectRegistry.bySign(effect)
                                            .provideProperties(amps)
                                            .size();
            radius = Math.max(0, Math.min(2048, value));
        }

        public ServerLevel level() {
            return level;
        }

        public BlockPos origin() {
            return origin;
        }

        public UUID owner() {
            return owner;
        }

        public int amplifierCount() {
            return amplifiers;
        }

        public float intensity() {
            return intensity;
        }

        public double radius() {
            return radius;
        }

        public ResourceLocation riteId() {
            return recipe.sign();
        }

        public ResourceLocation sign() {
            return recipe.sign();
        }

        public int positionBudget() {
            return 256;
        }

        public net.minecraft.util.RandomSource random() {
            return level.random;
        }

        public AffectedRegion requestRegion() {
            if (region == null || region.invalid())
                region = new AffectedRegion(level, origin, Math.min(32, radius));
            return region;
        }
    }

    private static final class Session extends AbstractRiteEffectSession {
        final RiteEffect effect;
        Context context;
        BooleanSupplier active;

        Session(RiteEffect effect, Context context, BooleanSupplier active) {
            this.effect = effect;
            this.context = context;
            this.active = active;
        }

        void update(Context next, BooleanSupplier live) {
            if (context.region != null
                    && Math.round(context.radius * 2) == Math.round(next.radius * 2))
                next.region = context.region;
            context = next;
            active = live;
        }

        protected void onStart() {
            effect.onRiteStart(context);
        }

        protected void onResume() {
            effect.onRiteResume(context);
        }

        protected void onTick(int elapsed) {
            effect.onRiteTick(context, elapsed);
        }

        protected void onSuspend() {
            effect.onRiteSuspend(context);
            if (context.region != null)
                context.region.invalidate(AffectedRegion.InvalidationReason.STRUCTURE_CHANGED);
        }

        protected void onDetach(RiteEffect.EndReason reason) {
            effect.onRiteEnd(context, reason);
            effect.onDetach(context);
        }
    }

    public static void schedule(
            ServerLevel level,
            BlockPos origin,
            UUID owner,
            int mirrors,
            float intensity,
            RiteRecipe recipe,
            BooleanSupplier active) {
        var runtime = LEVELS.computeIfAbsent(level, k -> new Runtime());
        for (var id : recipe.effects()) {
            var key = new Key(origin.immutable(), id);
            var session = runtime.sessions.get(key);
            if (session == null) {
                var effect = RiteEffectRegistry.create(id);
                if (effect.isEmpty()) {
                    com.mojang.logging.LogUtils.getLogger().warn("Unknown rite effect {}", id);
                    continue;
                }
                session =
                        new Session(
                                effect.get(),
                                new Context(level, origin, owner, mirrors, intensity, recipe, id),
                                active);
                runtime.sessions.put(key, session);
            } else
                session.update(
                        new Context(level, origin, owner, mirrors, intensity, recipe, id), active);
            var work = session;
            if (level.getGameTime()
                            % Math.max(recipe.tickInterval(), work.effect.suggestedInterval())
                    != 0) continue;
            runtime.dispatcher.submit(
                    key,
                    work.effect.positionCost(),
                    work.effect.budgetWeight(),
                    () -> {
                        if (!work.active.getAsBoolean() || !level.hasChunkAt(origin)) {
                            work.suspend();
                            return;
                        }
                        try {
                            work.tick(level.getGameTime());
                        } catch (RuntimeException failure) {
                            work.detach(RiteEffect.EndReason.FAILED);
                            com.mojang.logging.LogUtils.getLogger()
                                    .error("Rite effect {} at {} failed", id, origin, failure);
                        }
                    });
        }
    }

    public static void tick(ServerLevel level) {
        var runtime = LEVELS.get(level);
        if (runtime != null)
            runtime.dispatcher.tick(OmphalosConfig.SERVER.integer("ritual.positionBudget"));
    }

    public static void suspend(ServerLevel level, BlockPos origin) {
        var runtime = LEVELS.get(level);
        if (runtime != null)
            runtime.sessions.forEach(
                    (key, session) -> {
                        if (key.origin().equals(origin)) {
                            runtime.dispatcher.cancel(key);
                            session.suspend();
                        }
                    });
    }

    public static void detach(ServerLevel level, BlockPos origin) {
        var runtime = LEVELS.get(level);
        if (runtime == null) return;
        var it = runtime.sessions.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            if (entry.getKey().origin().equals(origin)) {
                runtime.dispatcher.cancel(entry.getKey());
                entry.getValue().detach(RiteEffect.EndReason.HOST_REMOVED);
                it.remove();
            }
        }
    }

    public static void unload(ServerLevel level) {
        var runtime = LEVELS.remove(level);
        if (runtime != null) {
            runtime.sessions.values().forEach(s -> s.detach(RiteEffect.EndReason.WORLD_UNLOADED));
            runtime.dispatcher.clear();
        }
    }
}

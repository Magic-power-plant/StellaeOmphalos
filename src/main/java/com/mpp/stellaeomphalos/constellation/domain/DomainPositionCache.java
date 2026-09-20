package com.mpp.stellaeomphalos.constellation.domain;

import com.mpp.stellaeomphalos.constellation.sign.MajorSign;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/**
 * Bounded, deduplicated position store with a warmup curve and NBT persistence (plan 2.2.6.3).
 *
 * <p>Because one effect instance is bound per sign and may serve several pedestals, the live list
 * is keyed by (dimension, origin): call {@link #focus(DomainContext)} at the top of {@code play}.
 * {@link #offer}, {@link #randomByChance}, {@link #write} and {@link #read} operate on the focused
 * list; before any focus they operate on a scratch list (used by unit tests).
 *
 * <p>Random candidate selection requires the chunk to be loaded and the {@link #verifier} (via the
 * level-aware {@link #verify} template) to accept the position. The warmup curve keeps the original
 * feel: {@code rand.nextInt(max((cap - size)/4, 0) + 1) == 0} — the fuller the cache, the higher
 * the hit probability; a full cache always hits.
 */
public abstract class DomainPositionCache<T extends DomainPositionEntry> extends DomainEffect {
    private record OriginKey(ResourceKey<Level> dimension, BlockPos origin) {}

    /** Focused view; plan field name kept for the scratch (unfocused) list. */
    protected final List<T> entries = new ArrayList<>();
    protected final int cap;
    protected final Predicate<BlockPos> verifier;
    protected final Function<BlockPos, T> factory;

    private final Map<OriginKey, List<T>> store = new HashMap<>();
    private List<T> focused = entries;

    protected DomainPositionCache(@Nullable MajorSign owner, int cap,
                                  Predicate<BlockPos> verifier, Function<BlockPos, T> factory) {
        super(owner);
        if (cap < 1) throw new IllegalArgumentException("Nonpositive cache cap");
        this.cap = cap;
        this.verifier = verifier;
        this.factory = factory;
    }

    /** Selects the per-origin live list; call once per play() invocation. */
    public void focus(DomainContext ctx) {
        focused = store.computeIfAbsent(new OriginKey(ctx.level().dimension(), ctx.origin().immutable()),
                key -> new ArrayList<>());
    }

    /** Live entries of the focused origin (scratch list when unfocused). */
    public List<T> entries() { return List.copyOf(focused); }
    public int size() { return focused.size(); }

    /** Level-aware template hook; defaults to the position-only verifier. */
    protected boolean verify(ServerLevel level, BlockPos pos) { return verifier.test(pos); }

    /** Dedupes by position, enforces the cap; false when rejected. */
    public boolean offer(T entry) {
        if (focused.size() >= cap) return false;
        for (var existing : focused) if (existing.pos().equals(entry.pos())) return false;
        return focused.add(entry);
    }

    /** Convenience for plain positions: verifies, wraps through the factory, then offers. */
    public boolean offer(ServerLevel level, BlockPos pos) {
        return level.hasChunkAt(pos) && verify(level, pos) && offer(factory.apply(pos.immutable()));
    }

    /** Warmup-curve pick; null when empty or the curve says "not yet". */
    public @Nullable T randomByChance(RandomSource random) {
        if (focused.isEmpty()) return null;
        return warmupHit(cap, focused.size(), random) ? focused.get(random.nextInt(focused.size())) : null;
    }

    /** Pure warmup curve, exposed for boundary tests. A full cache always hits. */
    public static boolean warmupHit(int cap, int size, RandomSource random) {
        if (size <= 0) return false;
        return random.nextInt(Math.max((cap - size) / 4, 0) + 1) == 0;
    }

    /** Tries a handful of random candidates inside the cube around center; offers the first valid one. */
    public boolean findNewPosition(ServerLevel level, BlockPos center, int radius) {
        var random = level.random;
        for (int attempt = 0; attempt < 16; attempt++) {
            var candidate = center.offset(random.nextInt(radius * 2 + 1) - radius,
                    random.nextInt(radius * 2 + 1) - radius, random.nextInt(radius * 2 + 1) - radius);
            if (level.hasChunkAt(candidate) && !level.isOutsideBuildHeight(candidate) && verify(level, candidate))
                return offer(factory.apply(candidate.immutable()));
        }
        return false;
    }

    /** Offers a random position around an explicit candidate (e.g. chosen by an external scanner). */
    public boolean findNewPositionAt(ServerLevel level, BlockPos candidate, int radius) {
        return findNewPosition(level, candidate, Math.max(radius, 1));
    }

    /** Drops invalid entries of the focused list (verifier re-check); returns the removal count. */
    public int prune(ServerLevel level) {
        int before = focused.size();
        focused.removeIf(entry -> !level.hasChunkAt(entry.pos()) || !verify(level, entry.pos()));
        return before - focused.size();
    }

    /** "Positions" list of compounds; the focused origin only. */
    public void write(CompoundTag tag) {
        var list = new ListTag();
        for (var entry : focused) {
            var compound = new CompoundTag();
            entry.write(compound);
            list.add(compound);
        }
        tag.put("Positions", list);
    }

    /** Template method: each record is materialized through the factory, then read into it. */
    public void read(CompoundTag tag) {
        focused.clear();
        var list = tag.getList("Positions", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size() && focused.size() < cap; i++) {
            var compound = list.getCompound(i);
            var entry = factory.apply(BlockPos.of(compound.getLong("Pos")));
            entry.read(compound);
            offer(entry);
        }
    }

    public void clearCache() { focused.clear(); }
    /** Drops every origin's state (structure teardown / module reset). */
    public void clearAll() { store.clear(); entries.clear(); focused = entries; }
}

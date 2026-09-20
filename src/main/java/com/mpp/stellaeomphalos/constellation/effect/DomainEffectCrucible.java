package com.mpp.stellaeomphalos.constellation.effect;

import com.mpp.stellaeomphalos.constellation.domain.DomainContext;
import com.mpp.stellaeomphalos.constellation.domain.DomainParticles;
import com.mpp.stellaeomphalos.constellation.domain.DomainPositionCache;
import com.mpp.stellaeomphalos.constellation.domain.DomainPositionEntries.CounterEntry;
import com.mpp.stellaeomphalos.constellation.domain.DomainProperties;
import com.mpp.stellaeomphalos.constellation.sign.MajorSign;
import com.mpp.stellaeomphalos.network.toClient.PktDomainParticle;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

import javax.annotation.Nullable;

/**
 * fornax — crucible. Smelts cached blocks in place (cap 10, per-entry progress counter persisted in
 * the entry). Particle packets obey the smelting merge rule: at most one per tick per position.
 * Corrupted ("solidify"): water -> ice, lava -> obsidian, fire extinguished.
 */
public final class DomainEffectCrucible extends DomainPositionCache<CounterEntry> {
    private static final int CAP = 10;

    public DomainEffectCrucible(@Nullable MajorSign owner) {
        super(owner, CAP, pos -> true, CounterEntry::new);
    }

    private static boolean corruptedTarget(ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        return state.is(Blocks.WATER) || state.is(Blocks.LAVA) || state.is(Blocks.FIRE);
    }

    @Override
    protected boolean verify(ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        if (state.isAir()) return false;
        return com.mpp.stellaeomphalos.core.platform.WorldCraftingBridge.melting(level, state)
                .isPresent();
    }

    @Override
    public DomainProperties provideProperties(int mirrorCount) {
        return new DomainProperties(5.0, 1.0, 1.0, false, 0.0, 1.0);
    }

    @Override
    public boolean play(DomainContext ctx, float strength, DomainProperties props) {
        var level = ctx.level();
        focus(ctx);
        if (!strengthGate(strength, level.random)) return false;
        int radius = (int) Math.max(1, Math.round(props.size()));
        if (size() == 0) findNewPosition(level, ctx.origin(), radius);
        return props.corrupted()
                ? playCorrupted(ctx, props, radius)
                : playSmelt(ctx, props, radius);
    }

    private boolean playSmelt(DomainContext ctx, DomainProperties props, int radius) {
        var level = ctx.level();
        var entry = randomByChance(level.random);
        if (entry == null || !level.hasChunkAt(entry.pos())) return false;
        var expected = level.getBlockState(entry.pos());
        var operation =
                com.mpp.stellaeomphalos.core.platform.WorldCraftingBridge.melting(level, expected);
        if (operation.isEmpty()) {
            prune(level);
            return false;
        }
        int needed =
                (int)
                        Math.max(
                                1,
                                Math.round(
                                        operation.get().duration()
                                                / Math.max(props.effectAmplifier(), 0.25)));
        if (entry.increment() < needed) return false;
        if (!operation.get().apply(level, entry.pos(), expected)) {
            entry.reset();
            return false;
        }
        entry.reset();
        prune(level);
        // Smelting merge: same tick + same type + same position collapse into one packet.
        if (DomainParticles.mergedThisTick(level, entry.pos(), PktDomainParticle.Types.CRUCIBLE))
            DomainParticles.broadcast(
                    level,
                    entry.pos(),
                    96.0,
                    PktDomainParticle.Types.CRUCIBLE,
                    ctx.origin(),
                    level.random.nextLong());
        return true;
    }

    private boolean playCorrupted(DomainContext ctx, DomainProperties props, int radius) {
        var level = ctx.level();
        var random = level.random;
        for (int attempt = 0; attempt < 8; attempt++) {
            var target =
                    ctx.origin()
                            .offset(
                                    random.nextInt(radius * 2 + 1) - radius,
                                    random.nextInt(radius * 2 + 1) - radius,
                                    random.nextInt(radius * 2 + 1) - radius);
            if (!level.hasChunkAt(target) || level.isOutsideBuildHeight(target)) continue;
            var state = level.getBlockState(target);
            if (state.is(Blocks.WATER)) {
                level.setBlock(target, Blocks.ICE.defaultBlockState(), 3);
                return true;
            }
            if (state.is(Blocks.LAVA)) {
                level.setBlock(target, Blocks.OBSIDIAN.defaultBlockState(), 3);
                return true;
            }
            if (state.is(Blocks.FIRE)) {
                level.removeBlock(target, false);
                return true;
            }
        }
        return false;
    }
}

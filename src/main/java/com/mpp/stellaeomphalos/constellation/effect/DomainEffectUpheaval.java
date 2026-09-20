package com.mpp.stellaeomphalos.constellation.effect;

import com.mpp.stellaeomphalos.constellation.domain.DomainContext;
import com.mpp.stellaeomphalos.constellation.domain.DomainParticles;
import com.mpp.stellaeomphalos.constellation.domain.DomainPositionCache;
import com.mpp.stellaeomphalos.constellation.domain.DomainPositionEntries.CounterEntry;
import com.mpp.stellaeomphalos.constellation.domain.DomainProperties;
import com.mpp.stellaeomphalos.constellation.domain.StarStructureLedger;
import com.mpp.stellaeomphalos.constellation.sign.MajorSign;
import com.mpp.stellaeomphalos.core.util.world.BlockDropCollector;
import com.mpp.stellaeomphalos.network.toClient.PktDomainParticle;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * evorsio — upheaval. Breaks cached blocks (hardness in [0, 75], never inside a recorded
 * star-structure volume) at 3 ticks per block, cap 8, and ferries the drops to the pedestal.
 * Corrupted: paves dirt/stone in range with a small chance of ore veins.
 */
public final class DomainEffectUpheaval extends DomainPositionCache<CounterEntry> {
    private static final int CAP = 8;
    private static final int TICKS_PER_BLOCK = 3;
    private static final float MAX_HARDNESS = 75.0F;
    private static final double STRUCTURE_EXCLUSION = 8.0;

    public DomainEffectUpheaval(@Nullable MajorSign owner) {
        super(owner, CAP, pos -> true, CounterEntry::new);
    }

    @Override
    protected boolean verify(ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        if (state.isAir()) return false;
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0 || hardness > MAX_HARDNESS) return false;
        var ledger = StarStructureLedger.get(level);
        for (var type : ledger.state().structures().keySet()) {
            var nearest = ledger.nearestDistance(type, pos);
            if (nearest.isPresent() && nearest.getAsDouble() <= STRUCTURE_EXCLUSION) return false;
        }
        return true;
    }

    @Override
    public DomainProperties provideProperties(int mirrorCount) {
        return new DomainProperties(6.0, 1.0, 1.0, false, 0.0, 1.0);
    }

    @Override
    public boolean play(DomainContext ctx, float strength, DomainProperties props) {
        var level = ctx.level();
        focus(ctx);
        if (!strengthGate(strength, level.random)) return false;
        int radius = (int) Math.max(1, Math.round(props.size()));
        if (size() == 0) findNewPosition(level, ctx.origin(), radius);
        if (props.corrupted()) return playCorrupted(ctx, props, radius);
        var entry = randomByChance(level.random);
        if (entry == null || !level.hasChunkAt(entry.pos())) return false;
        if (!verify(level, entry.pos())) { prune(level); return false; }
        if (entry.increment() < TICKS_PER_BLOCK) return false;
        // One block completes: capture the drops and deliver them to the pedestal.
        try (var collector = new BlockDropCollector(level, new AABB(entry.pos()))) {
            level.destroyBlock(entry.pos(), true);
            for (var stack : collector.drain()) {
                var drop = new ItemEntity(level, ctx.origin().getX() + 0.5, ctx.origin().getY() + 1.0,
                        ctx.origin().getZ() + 0.5, stack);
                level.addFreshEntity(drop);
            }
        }
        entry.reset();
        prune(level);
        if (DomainParticles.mergedThisTick(level, entry.pos(), PktDomainParticle.Types.UPHEAVAL))
            DomainParticles.broadcast(level, entry.pos(), 96.0, PktDomainParticle.Types.UPHEAVAL,
                    ctx.origin(), level.random.nextLong());
        return true;
    }

    private boolean playCorrupted(DomainContext ctx, DomainProperties props, int radius) {
        var level = ctx.level();
        var random = level.random;
        var target = ctx.origin().offset(random.nextInt(radius * 2 + 1) - radius,
                random.nextInt(radius) - radius / 2, random.nextInt(radius * 2 + 1) - radius);
        if (!level.hasChunkAt(target) || level.isOutsideBuildHeight(target)) return false;
        var state = level.getBlockState(target);
        float hardness = state.getDestroySpeed(level, target);
        if (hardness < 0 || hardness > MAX_HARDNESS) return false;
        var fill = random.nextFloat() < 0.04
                ? (random.nextBoolean() ? Blocks.COAL_ORE : Blocks.IRON_ORE).defaultBlockState()
                : (random.nextBoolean() ? Blocks.DIRT : Blocks.STONE).defaultBlockState();
        if (state.is(fill.getBlock())) return false;
        level.setBlock(target, fill, 3);
        return true;
    }
}

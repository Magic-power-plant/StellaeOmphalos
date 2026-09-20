package com.mpp.stellaeomphalos.constellation.effect;

import com.mpp.stellaeomphalos.constellation.domain.DomainContext;
import com.mpp.stellaeomphalos.constellation.domain.DomainParticles;
import com.mpp.stellaeomphalos.constellation.domain.DomainPositionCache;
import com.mpp.stellaeomphalos.constellation.domain.DomainPositionEntries.CounterCapEntry;
import com.mpp.stellaeomphalos.constellation.domain.DomainProperties;
import com.mpp.stellaeomphalos.constellation.sign.MajorSign;
import com.mpp.stellaeomphalos.network.toClient.PktDomainParticle;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * octans — angling. Caches water sources with air above (cap 8); per-entry progress advances
 * probabilistically and yields a fish when the per-entry cap is reached. The cap is clamped so
 * {@code maxFishTickTime >= minFishTickTime} always holds. Corrupted: randomly creates/drains water
 * and immediately spews fish.
 */
public final class DomainEffectAngling extends DomainPositionCache<CounterCapEntry> {
    private static final int CAP = 8;
    private static final int MIN_FISH_TICK_TIME = 100;
    private static final int MAX_FISH_TICK_TIME = 400;
    private static final float PROGRESS_CHANCE = 0.05F;

    private record WeightedFish(Item item, int weight) {}
    private static final List<WeightedFish> CATCHES = List.of(
            new WeightedFish(Items.COD, 60),
            new WeightedFish(Items.SALMON, 25),
            new WeightedFish(Items.PUFFERFISH, 10),
            new WeightedFish(Items.TROPICAL_FISH, 5));

    public DomainEffectAngling(@Nullable MajorSign owner) {
        super(owner, CAP, pos -> true, pos -> new CounterCapEntry(pos, rollCap(pos)));
    }

    /** Forced fix: max >= min, then a uniform cap in the range. */
    private static int rollCap(BlockPos pos) {
        int min = Math.min(MIN_FISH_TICK_TIME, MAX_FISH_TICK_TIME);
        int max = Math.max(MIN_FISH_TICK_TIME, MAX_FISH_TICK_TIME);
        return min + Math.floorMod(pos.hashCode(), max - min + 1);
    }

    @Override
    protected boolean verify(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(Blocks.WATER) && level.getBlockState(pos.above()).isAir();
    }

    private static Item rollCatch(net.minecraft.util.RandomSource random) {
        int total = CATCHES.stream().mapToInt(WeightedFish::weight).sum();
        int roll = random.nextInt(total);
        for (var fish : CATCHES) {
            roll -= fish.weight();
            if (roll < 0) return fish.item();
        }
        return Items.COD;
    }

    @Override
    public DomainProperties provideProperties(int mirrorCount) {
        return new DomainProperties(8.0, 1.0, 1.0, false, 0.0, 1.0);
    }

    @Override
    public boolean play(DomainContext ctx, float strength, DomainProperties props) {
        var level = ctx.level();
        focus(ctx);
        if (!strengthGate(strength, level.random)) return false;
        int radius = (int) Math.max(1, Math.round(props.size()));
        if (props.corrupted()) return playCorrupted(ctx, props, radius);
        if (size() == 0) findNewPosition(level, ctx.origin(), radius);
        var entry = randomByChance(level.random);
        if (entry == null || !level.hasChunkAt(entry.pos())) return false;
        if (!verify(level, entry.pos())) { prune(level); return false; }
        if (level.random.nextFloat() < PROGRESS_CHANCE * props.potency()) {
            if (!entry.tick()) return true;   // progressed, not yet due
        } else return false;
        // Due: spew the catch above the surface.
        var drop = new ItemEntity(level, entry.pos().getX() + 0.5, entry.pos().getY() + 1.0,
                entry.pos().getZ() + 0.5, new ItemStack(rollCatch(level.random)));
        level.addFreshEntity(drop);
        entry.reset();
        DomainParticles.broadcast(level, entry.pos(), 96.0, PktDomainParticle.Types.ANGLING,
                ctx.origin(), level.random.nextLong());
        return true;
    }

    private boolean playCorrupted(DomainContext ctx, DomainProperties props, int radius) {
        var level = ctx.level();
        var random = level.random;
        var target = ctx.origin().offset(random.nextInt(radius * 2 + 1) - radius,
                random.nextInt(radius * 2 + 1) - radius, random.nextInt(radius * 2 + 1) - radius);
        if (!level.hasChunkAt(target) || level.isOutsideBuildHeight(target)) return false;
        var state = level.getBlockState(target);
        if (state.is(Blocks.WATER)) {
            level.removeBlock(target, false);
        } else if (state.isAir() && replaceableGround(level, target.below())) {
            level.setBlock(target, Blocks.WATER.defaultBlockState(), 3);
        } else return false;
        var drop = new ItemEntity(level, target.getX() + 0.5, target.getY() + 1.0, target.getZ() + 0.5,
                new ItemStack(rollCatch(random)));
        level.addFreshEntity(drop);
        return true;
    }

    private static boolean replaceableGround(ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        return state.isSolidRender(level, pos);
    }
}

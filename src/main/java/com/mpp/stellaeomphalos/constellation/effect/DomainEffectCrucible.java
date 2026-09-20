package com.mpp.stellaeomphalos.constellation.effect;

import com.mpp.stellaeomphalos.constellation.domain.DomainContext;
import com.mpp.stellaeomphalos.constellation.domain.DomainParticles;
import com.mpp.stellaeomphalos.constellation.domain.DomainPositionCache;
import com.mpp.stellaeomphalos.constellation.domain.DomainPositionEntries.CounterEntry;
import com.mpp.stellaeomphalos.constellation.domain.DomainProperties;
import com.mpp.stellaeomphalos.constellation.sign.MajorSign;
import com.mpp.stellaeomphalos.network.toClient.PktDomainParticle;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;

/**
 * fornax — crucible. Smelts cached blocks in place (cap 10, per-entry progress counter persisted in
 * the entry). Particle packets obey the smelting merge rule: at most one per tick per position.
 * Corrupted ("solidify"): water -> ice, lava -> obsidian, fire extinguished.
 */
public final class DomainEffectCrucible extends DomainPositionCache<CounterEntry> {
    private static final int CAP = 10;
    private static final int BASE_SMELT_TICKS = 40;

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
        return smeltResult(level, pos) != null;
    }

    @Nullable
    private static ItemStack smeltResult(ServerLevel level, BlockPos pos) {
        var stack = new ItemStack(level.getBlockState(pos).getBlock().asItem());
        if (stack.isEmpty()) return null;
        return level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, new net.minecraft.world.SimpleContainer(stack), level)
                .map(recipe -> recipe.getResultItem(level.registryAccess()))
                .filter(result -> !result.isEmpty())
                .orElse(null);
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
        return props.corrupted() ? playCorrupted(ctx, props, radius) : playSmelt(ctx, props, radius);
    }

    private boolean playSmelt(DomainContext ctx, DomainProperties props, int radius) {
        var level = ctx.level();
        var entry = randomByChance(level.random);
        if (entry == null || !level.hasChunkAt(entry.pos())) return false;
        var result = smeltResult(level, entry.pos());
        if (result == null) { prune(level); return false; }
        int needed = (int) Math.max(1, Math.round(BASE_SMELT_TICKS / Math.max(props.effectAmplifier(), 0.25)));
        if (entry.increment() < needed) return false;
        if (result.getItem() instanceof BlockItem blockItem)
            level.setBlock(entry.pos(), blockItem.getBlock().defaultBlockState(), 3);
        else {
            level.destroyBlock(entry.pos(), false);
            var drop = new net.minecraft.world.entity.item.ItemEntity(level,
                    entry.pos().getX() + 0.5, entry.pos().getY() + 0.5, entry.pos().getZ() + 0.5, result.copy());
            level.addFreshEntity(drop);
        }
        entry.reset();
        prune(level);
        // Smelting merge: same tick + same type + same position collapse into one packet.
        if (DomainParticles.mergedThisTick(level, entry.pos(), PktDomainParticle.Types.CRUCIBLE))
            DomainParticles.broadcast(level, entry.pos(), 96.0, PktDomainParticle.Types.CRUCIBLE,
                    ctx.origin(), level.random.nextLong());
        return true;
    }

    private boolean playCorrupted(DomainContext ctx, DomainProperties props, int radius) {
        var level = ctx.level();
        var random = level.random;
        for (int attempt = 0; attempt < 8; attempt++) {
            var target = ctx.origin().offset(random.nextInt(radius * 2 + 1) - radius,
                    random.nextInt(radius * 2 + 1) - radius, random.nextInt(radius * 2 + 1) - radius);
            if (!level.hasChunkAt(target) || level.isOutsideBuildHeight(target)) continue;
            var state = level.getBlockState(target);
            if (state.is(Blocks.WATER)) { level.setBlock(target, Blocks.ICE.defaultBlockState(), 3); return true; }
            if (state.is(Blocks.LAVA)) { level.setBlock(target, Blocks.OBSIDIAN.defaultBlockState(), 3); return true; }
            if (state.is(Blocks.FIRE)) { level.removeBlock(target, false); return true; }
        }
        return false;
    }
}

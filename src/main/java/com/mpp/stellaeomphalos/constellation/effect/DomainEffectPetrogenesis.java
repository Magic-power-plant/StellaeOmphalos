package com.mpp.stellaeomphalos.constellation.effect;

import com.mpp.stellaeomphalos.constellation.domain.DomainContext;
import com.mpp.stellaeomphalos.constellation.domain.DomainParticles;
import com.mpp.stellaeomphalos.constellation.domain.DomainPositionCache;
import com.mpp.stellaeomphalos.constellation.domain.DomainPositionEntries.SimplePosEntry;
import com.mpp.stellaeomphalos.constellation.domain.DomainProperties;
import com.mpp.stellaeomphalos.constellation.sign.MajorSign;
import com.mpp.stellaeomphalos.network.toClient.PktDomainParticle;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * mineralis — petrogenesis. Converts cached replaceable blocks into ores; the cache cap is
 * deliberately 2 to keep conversion slow (anti farming). Corrupted: paves stone/ore under the feet
 * of entities in range.
 */
public final class DomainEffectPetrogenesis extends DomainPositionCache<SimplePosEntry> {
    private static final int CAP = 2;

    private record WeightedOre(Block block, int weight) {}
    private static final List<WeightedOre> ORES = List.of(
            new WeightedOre(Blocks.COAL_ORE, 40),
            new WeightedOre(Blocks.IRON_ORE, 25),
            new WeightedOre(Blocks.COPPER_ORE, 15),
            new WeightedOre(Blocks.GOLD_ORE, 10),
            new WeightedOre(Blocks.REDSTONE_ORE, 6),
            new WeightedOre(Blocks.LAPIS_ORE, 3),
            new WeightedOre(Blocks.DIAMOND_ORE, 1));

    public DomainEffectPetrogenesis(@Nullable MajorSign owner) {
        super(owner, CAP, pos -> true, SimplePosEntry::new);
    }

    private static boolean replaceable(BlockState state) {
        return state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE) || state.is(Blocks.NETHERRACK);
    }

    @Override
    protected boolean verify(ServerLevel level, BlockPos pos) {
        return replaceable(level.getBlockState(pos));
    }

    private static Block rollOre(net.minecraft.util.RandomSource random) {
        int total = ORES.stream().mapToInt(WeightedOre::weight).sum();
        int roll = random.nextInt(total);
        for (var ore : ORES) {
            roll -= ore.weight();
            if (roll < 0) return ore.block();
        }
        return ORES.get(0).block();
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
        if (props.corrupted()) return playCorrupted(ctx, props);
        int radius = (int) Math.max(1, Math.round(props.size()));
        if (size() < CAP) findNewPosition(level, ctx.origin(), radius);
        var entry = randomByChance(level.random);
        if (entry == null || !level.hasChunkAt(entry.pos())) return false;
        if (!verify(level, entry.pos())) { prune(level); return false; }
        level.setBlock(entry.pos(), rollOre(level.random).defaultBlockState(), 3);
        prune(level);
        if (DomainParticles.mergedThisTick(level, entry.pos(), PktDomainParticle.Types.PETROGENESIS))
            DomainParticles.broadcast(level, entry.pos(), 96.0, PktDomainParticle.Types.PETROGENESIS,
                    ctx.origin(), level.random.nextLong());
        return true;
    }

    private boolean playCorrupted(DomainContext ctx, DomainProperties props) {
        var level = ctx.level();
        var box = new AABB(ctx.origin()).inflate(props.size());
        boolean acted = false;
        for (var entity : level.getEntitiesOfClass(LivingEntity.class, box)) {
            var below = entity.blockPosition().below();
            if (!level.hasChunkAt(below) || level.isOutsideBuildHeight(below)) continue;
            if (!replaceable(level.getBlockState(below))) continue;
            level.setBlock(below, (level.random.nextFloat() < 0.1F ? rollOre(level.random) : Blocks.STONE)
                    .defaultBlockState(), 3);
            acted = true;
        }
        return acted;
    }
}

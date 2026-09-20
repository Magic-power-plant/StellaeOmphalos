package com.mpp.stellaeomphalos.constellation.effect;

import com.mpp.stellaeomphalos.constellation.domain.DomainContext;
import com.mpp.stellaeomphalos.constellation.domain.DomainParticles;
import com.mpp.stellaeomphalos.constellation.domain.DomainPositionCache;
import com.mpp.stellaeomphalos.constellation.domain.DomainPositionEntries.SimplePosEntry;
import com.mpp.stellaeomphalos.constellation.domain.DomainProperties;
import com.mpp.stellaeomphalos.constellation.sign.MajorSign;
import com.mpp.stellaeomphalos.core.util.world.PlantAdapter;
import com.mpp.stellaeomphalos.network.toClient.PktDomainParticle;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.phys.AABB;

/**
 * aevitas — growth. Caches up to 12 growable plants, at most 2 growth attempts per tick, refills
 * every 40 ticks. Corrupted: destroys crops and applies bleed-like debuffs (weakness / hunger /
 * mining fatigue) to living entities in range.
 */
public final class DomainEffectVerdance extends DomainPositionCache<SimplePosEntry> {
    private static final int CAP = 12;
    private static final int MAX_GROW_ATTEMPTS = 2;
    private static final int REFILL_INTERVAL = 40;

    public DomainEffectVerdance(@Nullable MajorSign owner) {
        super(owner, CAP, pos -> true, SimplePosEntry::new);
    }

    @Override
    protected boolean verify(ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        return state.getBlock() instanceof BonemealableBlock || state.getBlock() instanceof CropBlock
                || state.getBlock() instanceof NetherWartBlock;
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
        if (level.getGameTime() % REFILL_INTERVAL == 0 || size() == 0) {
            prune(level);
            findNewPosition(level, ctx.origin(), radius);
        }
        if (props.corrupted()) return playCorrupted(ctx, props, radius);
        int attempts = 0;
        for (int i = 0; i < MAX_GROW_ATTEMPTS; i++) {
            var entry = randomByChance(level.random);
            if (entry == null) break;
            if (!level.hasChunkAt(entry.pos())) continue;
            if (PlantAdapter.grow(level, entry.pos())) attempts++;
            else prune(level);
        }
        if (attempts > 0 && DomainParticles.mergedThisTick(level, ctx.origin(), PktDomainParticle.Types.VERDANCE))
            DomainParticles.broadcast(level, ctx.origin(), 96.0, PktDomainParticle.Types.VERDANCE,
                    null, level.random.nextLong());
        return attempts > 0;
    }

    private boolean playCorrupted(DomainContext ctx, DomainProperties props, int radius) {
        var level = ctx.level();
        boolean acted = false;
        var entry = randomByChance(level.random);
        if (entry != null && level.hasChunkAt(entry.pos())) {
            var state = level.getBlockState(entry.pos());
            if (state.getBlock() instanceof CropBlock || state.getBlock() instanceof NetherWartBlock) {
                level.destroyBlock(entry.pos(), true);
                acted = true;
            }
        }
        var box = new AABB(ctx.origin()).inflate(props.size());
        for (var entity : level.getEntitiesOfClass(LivingEntity.class, box)) {
            entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0));
            entity.addEffect(new MobEffectInstance(MobEffects.HUNGER, 200, 0));
            entity.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 200, 0));
        }
        return acted;
    }
}

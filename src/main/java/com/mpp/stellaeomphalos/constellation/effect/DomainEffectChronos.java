package com.mpp.stellaeomphalos.constellation.effect;

import com.mojang.logging.LogUtils;
import com.mpp.stellaeomphalos.constellation.domain.DomainContext;
import com.mpp.stellaeomphalos.constellation.domain.DomainParticles;
import com.mpp.stellaeomphalos.constellation.domain.DomainPositionCache;
import com.mpp.stellaeomphalos.constellation.domain.DomainPositionEntries.SimplePosEntry;
import com.mpp.stellaeomphalos.constellation.domain.DomainProperties;
import com.mpp.stellaeomphalos.constellation.sign.MajorSign;
import com.mpp.stellaeomphalos.lumen.transport.stasis.StasisFilter;
import com.mpp.stellaeomphalos.lumen.transport.stasis.StasisService;
import com.mpp.stellaeomphalos.network.toClient.PktDomainParticle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * horologium — chronos. Manually ticks whitelisted block entities 5-7 extra times per tick under a
 * hard 80-microsecond budget; the first exception blacklists the block entity class permanently.
 * Particle packets pass a fixed 10-tick interval limiter. Corrupted: raises a stasis zone instead
 * (reuses StasisService, plan 2.2.6.2 row 7).
 */
public final class DomainEffectChronos extends DomainPositionCache<SimplePosEntry> {
    private static final int CAP = 10;
    private static final long BUDGET_NANOS = 80_000L;
    private static final int PARTICLE_INTERVAL = 10;
    private static final long STASIS_DURATION = 100L;
    private static final int STASIS_REAPPLY_INTERVAL = 80;

    /** Extra whitelist hook; default accepts any block entity exposing a ticker. */
    private static volatile java.util.function.Predicate<ResourceLocation> whitelist = id -> true;
    private static final Set<ResourceLocation> BLACKLIST = ConcurrentHashMap.newKeySet();

    public DomainEffectChronos(@Nullable MajorSign owner) {
        super(owner, CAP, pos -> true, SimplePosEntry::new);
    }

    /** Assembly hook: restricts the acceleratable block entity type ids. */
    public static void setWhitelist(java.util.function.Predicate<ResourceLocation> predicate) {
        whitelist = java.util.Objects.requireNonNull(predicate);
    }

    public static boolean blacklisted(ResourceLocation blockEntityType) { return BLACKLIST.contains(blockEntityType); }

    @Override
    protected boolean verify(ServerLevel level, BlockPos pos) {
        var entity = level.getBlockEntity(pos);
        if (entity == null) return false;
        var id = ForgeRegistries.BLOCK_ENTITY_TYPES.getKey(entity.getType());
        if (id == null || BLACKLIST.contains(id) || !whitelist.test(id)) return false;
        return tickerOf(level, entity) != null;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static BlockEntityTicker<BlockEntity> tickerOf(ServerLevel level, BlockEntity entity) {
        var state = entity.getBlockState();
        var ticker = state.getTicker(level, entity.getType());
        return ticker == null ? null : (BlockEntityTicker<BlockEntity>) ticker;
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
        if (props.corrupted()) return playCorrupted(ctx, props);
        int radius = (int) Math.max(1, Math.round(props.size()));
        if (size() == 0) findNewPosition(level, ctx.origin(), radius);
        long deadline = System.nanoTime() + BUDGET_NANOS;
        boolean acted = false;
        int extraTicks = 5 + level.random.nextInt(3);
        while (System.nanoTime() < deadline) {
            var entry = randomByChance(level.random);
            if (entry == null) break;
            if (!level.hasChunkAt(entry.pos())) { prune(level); continue; }
            var entity = level.getBlockEntity(entry.pos());
            BlockEntityTicker<BlockEntity> ticker = entity == null ? null : tickerOf(level, entity);
            if (ticker == null) { prune(level); continue; }
            try {
                for (int i = 0; i < extraTicks && System.nanoTime() < deadline; i++)
                    ticker.tick(level, entry.pos(), entity.getBlockState(), entity);
                acted = true;
            } catch (RuntimeException exception) {
                var id = ForgeRegistries.BLOCK_ENTITY_TYPES.getKey(entity.getType());
                if (id != null) {
                    BLACKLIST.add(id);
                    LogUtils.getLogger().warn("Chronos blacklisted {} after ticker failure", id, exception);
                }
                prune(level);
            }
            break;   // one position per invocation; the budget guards the inner loop
        }
        if (acted && DomainParticles.intervalPassed(level, ctx.origin(), PktDomainParticle.Types.CHRONOS, PARTICLE_INTERVAL))
            DomainParticles.broadcast(level, ctx.origin(), 96.0, PktDomainParticle.Types.CHRONOS,
                    null, level.random.nextLong());
        return acted;
    }

    private boolean playCorrupted(DomainContext ctx, DomainProperties props) {
        var level = ctx.level();
        if (level.getGameTime() % STASIS_REAPPLY_INTERVAL != 0) return false;
        return StasisService.get(level.getServer()).activate(level, ctx.origin(), props.size(),
                new StasisFilter(StasisFilter.Mode.ALL_EXCEPT, owningPlayer(ctx), true),
                STASIS_DURATION, owningPlayer(ctx));
    }
}

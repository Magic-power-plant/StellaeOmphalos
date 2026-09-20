package com.mpp.stellaeomphalos.constellation.effect;

import com.mpp.stellaeomphalos.constellation.domain.DomainContext;
import com.mpp.stellaeomphalos.constellation.domain.DomainParticles;
import com.mpp.stellaeomphalos.constellation.domain.DomainPositionCache;
import com.mpp.stellaeomphalos.constellation.domain.DomainPositionEntries.SpawnEntry;
import com.mpp.stellaeomphalos.constellation.domain.DomainProperties;
import com.mpp.stellaeomphalos.constellation.sign.MajorSign;
import com.mpp.stellaeomphalos.network.toClient.PktDomainParticle;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.AABB;

/**
 * pelotrio — spawn. Warmup 30 ticks per entry, cache cap 5; halts entirely while the nearby entity
 * count (radius 24) reaches 40. {@code selectChance} gates the search for new positions. Warmup
 * particles obey the >= 5-tick interval rule. Corrupted: replaces animals with their monster
 * counterparts.
 */
public final class DomainEffectSpawn extends DomainPositionCache<SpawnEntry> {
    private static final int CAP = 5;
    private static final int WARMUP_TICKS = 30;
    private static final float SELECT_CHANCE = 0.05F;
    private static final int CROWD_RADIUS = 24;
    private static final int CROWD_THRESHOLD = 40;
    private static final int WARMUP_PARTICLE_INTERVAL = 5;

    /** Corrupted replacement table: animal -> monster version. */
    private static final Map<EntityType<?>, EntityType<?>> CORRUPTED_REPLACEMENTS = Map.of(
            EntityType.PIG, EntityType.ZOMBIE,
            EntityType.COW, EntityType.HUSK,
            EntityType.SHEEP, EntityType.SKELETON,
            EntityType.CHICKEN, EntityType.SPIDER);

    public DomainEffectSpawn(@Nullable MajorSign owner) {
        super(owner, CAP, pos -> true, SpawnEntry::new);
    }

    @Override
    protected boolean verify(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos.below()).isSolidRender(level, pos.below())
                && level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir();
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
        if (props.corrupted()) return playCorrupted(ctx, props);
        // Crowd check: stop outright when the area is saturated.
        var crowdBox = new AABB(ctx.origin()).inflate(CROWD_RADIUS);
        if (level.getEntitiesOfClass(Entity.class, crowdBox, entity -> !(entity instanceof net.minecraft.world.entity.player.Player))
                .size() >= CROWD_THRESHOLD) return false;
        int radius = (int) Math.max(1, Math.round(props.size()));
        if (size() < CAP && level.random.nextFloat() < SELECT_CHANCE * props.potency())
            findNewPosition(level, ctx.origin(), radius);
        boolean acted = false;
        for (var entry : entries()) {
            if (entry.increment() < WARMUP_TICKS) {
                if (DomainParticles.intervalPassed(level, entry.pos(), PktDomainParticle.Types.SPAWN, WARMUP_PARTICLE_INTERVAL))
                    DomainParticles.broadcast(level, entry.pos(), 96.0, PktDomainParticle.Types.SPAWN,
                            ctx.origin(), level.random.nextLong());
                continue;
            }
            entry.rollSpawnType(level, level.random);
            if (entry.trySpawn(level, level.random) != null) acted = true;
            entry.reset();
            entry.rollSpawnType(level, level.random);
        }
        return acted;
    }

    private boolean playCorrupted(DomainContext ctx, DomainProperties props) {
        var level = ctx.level();
        var box = new AABB(ctx.origin()).inflate(props.size());
        boolean acted = false;
        for (var animal : level.getEntitiesOfClass(Animal.class, box)) {
            var replacement = CORRUPTED_REPLACEMENTS.get(animal.getType());
            if (replacement == null) continue;
            var monster = replacement.create(level);
            if (monster == null) continue;
            monster.moveTo(animal.getX(), animal.getY(), animal.getZ(), animal.getYRot(), animal.getXRot());
            if (!level.noCollision(monster)) continue;
            animal.discard();
            level.addFreshEntity(monster);
            acted = true;
        }
        return acted;
    }
}

package com.mpp.stellaeomphalos.lumen.transport.stasis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * One stasis zone: WARMUP (0.5 s) -> ACTIVE -> FADING (0.5 s) -> removed.
 *
 * Block-entity freezing without Mixin: on park we snapshot every in-zone block entity with
 * saveWithFullMetadata; while ACTIVE each level tick re-validates ("block still there + same
 * block entity type") and rolls the block entity back to its snapshot via load(). The ticker
 * itself keeps running, so per-tick world side effects of a tick (e.g. a hopper push) are not
 * undone - only persisted state is pinned. [待验证] semantic equivalence per block entity type.
 *
 * Non-living entities (items, projectiles) have no cancellable tick event in Forge 47, so they
 * are compensated: position/motion pinned to the spot where the zone first saw them.
 */
public final class StasisZone {
    public enum Phase { WARMUP, ACTIVE, FADING }
    public static final long WARMUP_TICKS = 10;
    public static final long FADE_TICKS = 10;

    record ParkedBlockEntity(BlockEntity entity, Block block, CompoundTag snapshot) {}

    private final BlockPos center;
    private final double radius;
    private final StasisFilter filter;
    private final @Nullable UUID owner;
    private final Map<BlockPos, ParkedBlockEntity> parked = new LinkedHashMap<>();
    private final Map<UUID, Vec3> parkedEntities = new HashMap<>();
    private Phase phase = Phase.WARMUP;
    private long phaseTicks;
    private long remainingTicks;
    private int particleTier;

    public StasisZone(BlockPos center, double radius, StasisFilter filter, long durationTicks, @Nullable UUID owner) {
        this.center = center.immutable();
        this.radius = radius;
        this.filter = filter;
        this.owner = owner;
        this.remainingTicks = durationTicks;
    }

    /** Restore from storage: remaining ticks and filter strategy survive; block entities are re-parked fresh. */
    public static StasisZone restore(StasisData.ZoneRecord record, double maxRadius) {
        var filter = new StasisFilter(StasisFilter.Mode.bySerialized(record.filterMode()).orElse(StasisFilter.Mode.NO_PLAYERS),
                record.owner().orElse(null), record.targetPlayers());
        var zone = new StasisZone(BlockPos.of(record.center()), Mth.clamp(record.radius(), 1.0, maxRadius),
                filter, Math.max(1, record.remaining()), record.owner().orElse(null));
        zone.particleTier = record.particleTier();
        return zone;
    }

    public BlockPos center() { return center; }
    public double radius() { return radius; }
    public StasisFilter filter() { return filter; }
    public Phase phase() { return phase; }
    public long remainingTicks() { return remainingTicks; }
    public int particleTier() { return particleTier; }
    public int parkedCount() { return parked.size(); }

    public boolean contains(BlockPos pos) { return pos.distSqr(center) <= radius * radius; }
    public boolean contains(Entity entity) { return entity.distanceToSqr(Vec3.atCenterOf(center)) <= radius * radius; }
    public boolean freezesNow(Entity entity) { return phase == Phase.ACTIVE && filter.freezes(entity) && contains(entity); }
    public boolean freezesBlockEntityNow(BlockPos pos) { return phase == Phase.ACTIVE && parked.containsKey(pos); }

    void setParticleTier(int tier) { particleTier = tier; }

    /** Snapshot every block entity inside the zone, capped at maxParked. */
    void park(ServerLevel level, int maxParked) {
        int minChunkX = (int) Math.floor((center.getX() - radius) / 16.0);
        int maxChunkX = (int) Math.floor((center.getX() + radius) / 16.0);
        int minChunkZ = (int) Math.floor((center.getZ() - radius) / 16.0);
        int maxChunkZ = (int) Math.floor((center.getZ() + radius) / 16.0);
        for (int chunkX = minChunkX; chunkX <= maxChunkX && parked.size() < maxParked; chunkX++)
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ && parked.size() < maxParked; chunkZ++) {
                if (!level.hasChunk(chunkX, chunkZ)) continue;
                for (var entry : new ArrayList<>(level.getChunk(chunkX, chunkZ).getBlockEntities().entrySet())) {
                    if (parked.size() >= maxParked) break;
                    BlockPos pos = entry.getKey();
                    BlockEntity blockEntity = entry.getValue();
                    if (!contains(pos) || !filter.freezes(blockEntity) || parked.containsKey(pos)) continue;
                    parked.put(pos, new ParkedBlockEntity(blockEntity, blockEntity.getBlockState().getBlock(), blockEntity.saveWithFullMetadata()));
                }
            }
    }

    /** @return true when the zone finished fading and must be removed. */
    boolean tick(ServerLevel level) {
        phaseTicks++;
        switch (phase) {
            case WARMUP -> { if (phaseTicks >= WARMUP_TICKS) enter(Phase.ACTIVE); }
            case ACTIVE -> {
                rollbackParkedBlockEntities(level);
                pinNonLivingEntities(level);
                if (--remainingTicks <= 0) enter(Phase.FADING);
            }
            case FADING -> {
                if (phaseTicks >= FADE_TICKS) {
                    release();
                    return true;
                }
            }
        }
        return false;
    }

    /** Instant teardown (eviction of the oldest zone when the cap is exceeded). */
    void release() { parked.clear(); parkedEntities.clear(); }

    private void enter(Phase next) { phase = next; phaseTicks = 0; }

    private void rollbackParkedBlockEntities(ServerLevel level) {
        parked.values().removeIf(entry -> {
            BlockPos pos = entry.entity().getBlockPos();
            if (!level.hasChunkAt(pos)) return false;
            boolean valid = level.getBlockEntity(pos) == entry.entity()
                    && level.getBlockState(pos).is(entry.block());
            if (valid) entry.entity().load(entry.snapshot().copy());
            return !valid;
        });
    }

    private void pinNonLivingEntities(ServerLevel level) {
        var box = new AABB(center).inflate(radius);
        var seen = new java.util.HashSet<UUID>();
        for (Entity entity : level.getEntitiesOfClass(Entity.class, box,
                candidate -> !(candidate instanceof LivingEntity) && filter.freezes(candidate) && contains(candidate))) {
            seen.add(entity.getUUID());
            Vec3 parkedAt = parkedEntities.computeIfAbsent(entity.getUUID(), key -> entity.position());
            entity.setPos(parkedAt.x, parkedAt.y, parkedAt.z);
            entity.setDeltaMovement(Vec3.ZERO);
        }
        parkedEntities.keySet().retainAll(seen);
    }

    public StasisZoneView view() {
        return new StasisZoneView(center, radius, filter.mode(), Optional.ofNullable(owner), particleTier, phase, remainingTicks);
    }

    public StasisData.ZoneRecord toRecord() {
        return new StasisData.ZoneRecord(center.asLong(), (float) radius, filter.mode().serialized(),
                Optional.ofNullable(filter.owner()), filter.targetPlayers(), remainingTicks, particleTier);
    }
}

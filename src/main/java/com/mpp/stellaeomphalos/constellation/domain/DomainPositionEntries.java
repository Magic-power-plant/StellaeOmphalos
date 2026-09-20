package com.mpp.stellaeomphalos.constellation.domain;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * The five position-entry flavours (plan 2.2.6.3). NBT keys are PascalCase; entity ids serialize as
 * strings.
 */
public final class DomainPositionEntries {
    private DomainPositionEntries() {}

    /** Plain position slot (Chronos / Petrogenesis). */
    public static final class SimplePosEntry extends DomainPositionEntry {
        public SimplePosEntry(BlockPos pos) { super(pos); }
    }

    /** Position plus a mutable progress counter (Crucible smelting progress). */
    public static class CounterEntry extends DomainPositionEntry {
        private int counter;
        public CounterEntry(BlockPos pos) { super(pos); }
        public int counter() { return counter; }
        public int increment() { return ++counter; }
        public void add(int amount) { counter += amount; }
        public void reset() { counter = 0; }
        @Override public void write(CompoundTag tag) { super.write(tag); tag.putInt("Counter", counter); }
        @Override public void read(CompoundTag tag) { counter = tag.getInt("Counter"); }
    }

    /** Counter with an individual cap (Angling fishing progress). */
    public static final class CounterCapEntry extends CounterEntry {
        private int cap;
        public CounterCapEntry(BlockPos pos) { this(pos, 1); }
        public CounterCapEntry(BlockPos pos, int cap) { super(pos); this.cap = Math.max(cap, 1); }
        public int cap() { return cap; }
        /** Enforces cap >= floor (plan: maxFishTickTime >= minFishTickTime force fix). */
        public void setCap(int cap) { this.cap = Math.max(cap, 1); }
        /** @return true once the counter reached the cap (counter is left at the cap). */
        public boolean tick() {
            if (counter() < cap) increment();
            return counter() >= cap;
        }
        @Override public void write(CompoundTag tag) { super.write(tag); tag.putInt("Cap", cap); }
        @Override public void read(CompoundTag tag) { super.read(tag); if (tag.contains("Cap")) cap = Math.max(tag.getInt("Cap"), 1); }
    }

    /** Position plus a (key, value) NBT tuple; reserved extension point, simplified per plan. */
    public static final class TaggedTupleEntry extends DomainPositionEntry {
        private CompoundTag key = new CompoundTag();
        private CompoundTag value = new CompoundTag();
        public TaggedTupleEntry(BlockPos pos) { super(pos); }
        public CompoundTag key() { return key; }
        public CompoundTag value() { return value; }
        public void set(CompoundTag key, CompoundTag value) { this.key = key.copy(); this.value = value.copy(); }
        @Override public void write(CompoundTag tag) { super.write(tag); tag.put("Key", key); tag.put("Value", value); }
        @Override public void read(CompoundTag tag) { key = tag.getCompound("Key"); value = tag.getCompound("Value"); }
    }

    /**
     * Spawn slot (Spawn effect): position + warmup counter + the entity type chosen by the
     * day/night spawn table. Daytime picks a passive, night a monster.
     */
    public static final class SpawnEntry extends CounterEntry {
        public static final List<ResourceLocation> DAY_TABLE = List.of(
                net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.PIG),
                net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.COW),
                net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.SHEEP),
                net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.CHICKEN));
        public static final List<ResourceLocation> NIGHT_TABLE = List.of(
                net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.ZOMBIE),
                net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.SKELETON),
                net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.SPIDER),
                net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.CREEPER));

        private ResourceLocation entityType;

        public SpawnEntry(BlockPos pos) { this(pos, DAY_TABLE.get(0)); }
        public SpawnEntry(BlockPos pos, ResourceLocation entityType) { super(pos); this.entityType = entityType; }
        public ResourceLocation entityType() { return entityType; }

        /** Picks the spawn table by day/night, then a uniform entry. */
        public void rollSpawnType(ServerLevel level, RandomSource random) {
            var table = level.isDay() ? DAY_TABLE : NIGHT_TABLE;
            entityType = table.get(random.nextInt(table.size()));
        }

        /**
         * Spawn with condition and collision validation, then a second validation after the entity
         * landed in the world; returns the spawned entity or null when validation fails.
         */
        public @Nullable Entity trySpawn(ServerLevel level, RandomSource random) {
            var type = ForgeRegistries.ENTITY_TYPES.getValue(entityType);
            if (type == null) return null;
            var entity = type.create(level);
            if (entity == null) return null;
            var pos = pos();
            entity.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                    random.nextFloat() * 360.0F, 0.0F);
            // Spawn conditions + collision validation before joining the world.
            if (!level.noCollision(entity)) return null;
            if (entity instanceof Mob mob
                    && !mob.checkSpawnRules(level, MobSpawnType.SPAWNER)) return null;
            if (!level.addFreshEntity(entity)) return null;
            // Second validation after landing: despawn collisions/invalid placement.
            if (!entity.isAlive() || !level.noCollision(entity)) {
                entity.discard();
                return null;
            }
            if (entity instanceof Mob mob) {
                mob.finalizeSpawn(level, new DifficultyInstance(level.getDifficulty(), level.getDayTime(), 0L, 0.0F),
                        MobSpawnType.SPAWNER, null, null);
            }
            return entity;
        }

        @Override public void write(CompoundTag tag) { super.write(tag); tag.putString("Entity", entityType.toString()); }
        @Override public void read(CompoundTag tag) {
            super.read(tag);
            if (tag.contains("Entity")) {
                var parsed = ResourceLocation.tryParse(tag.getString("Entity"));
                if (parsed != null) entityType = parsed;
            }
        }
    }
}

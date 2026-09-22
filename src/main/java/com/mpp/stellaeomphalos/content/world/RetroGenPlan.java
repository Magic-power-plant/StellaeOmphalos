package com.mpp.stellaeomphalos.content.world;

import com.mpp.stellaeomphalos.OmphalosConfig;
import com.mpp.stellaeomphalos.content.world.capability.*;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;

import java.util.*;

/** No recursive loads. Missing neighbors defer a phase, successful phases alone receive stamps. */
public final class RetroGenPlan {
    private final ServerLevel level;
    private final QueueData saved;
    private final Map<ChunkPos, Integer> queue;

    /** Pending work is separate from the per-chunk completed-phase stamps. */
    public static final class QueueData extends net.minecraft.world.level.saveddata.SavedData {
        private final Map<ChunkPos, Integer> entries = new LinkedHashMap<>();
        public static QueueData load(net.minecraft.nbt.CompoundTag tag) {
            var data = new QueueData();
            for (var value : tag.getList("Pending", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
                if (data.entries.size() >= 16384) break;
                var entry = (net.minecraft.nbt.CompoundTag) value;
                data.entries.put(new ChunkPos(entry.getLong("Chunk")), Math.max(0, Math.min(40, entry.getInt("Retries"))));
            }
            return data;
        }
        @Override public net.minecraft.nbt.CompoundTag save(net.minecraft.nbt.CompoundTag tag) {
            var list = new net.minecraft.nbt.ListTag();
            entries.forEach((pos, retries) -> {
                var entry = new net.minecraft.nbt.CompoundTag();
                entry.putLong("Chunk", pos.toLong()); entry.putInt("Retries", retries); list.add(entry);
            });
            tag.put("Pending", list);
            return tag;
        }
    }
    private boolean running;
    private int processed, skipped;

    public RetroGenPlan(ServerLevel level) {
        this.level = level;
        saved = level.getDataStorage().computeIfAbsent(QueueData::load, QueueData::new, "stellaeomphalos_retrogen_queue");
        queue = saved.entries;
    }

    public void enqueue(ChunkPos p) {
        if (queue.size() < 16384 && queue.putIfAbsent(p, 0) == null) saved.setDirty();
    }

    public int queued() {
        return queue.size();
    }

    public int processed() {
        return processed;
    }

    public int skipped() {
        return skipped;
    }

    public void tick() {
        if (running || queue.isEmpty()) return;
        running = true;
        long deadline =
                System.nanoTime()
                        + OmphalosConfig.SERVER.integer("worldgen.retrogenMillis") * 1000000L;
        int budget = OmphalosConfig.SERVER.integer("worldgen.retrogenChunks");
        try {
            var candidates = new ArrayList<>(queue.keySet());
            candidates.sort(
                    Comparator.comparingDouble(
                            p ->
                                    level.players().stream()
                                            .mapToDouble(
                                                    player ->
                                                            player.distanceToSqr(
                                                                    p.getMiddleBlockX(),
                                                                    player.getY(),
                                                                    p.getMiddleBlockZ()))
                                            .min()
                                            .orElse(0)));
            for (var p : candidates) {
                if (budget <= 0 || System.nanoTime() >= deadline) break;
                var chunk = level.getChunkSource().getChunkNow(p.x, p.z);
                if (chunk == null) continue;
                var stamp = chunk.getCapability(WorldCapabilities.STAMP).orElse(null);
                if (stamp == null || !stamp.terrainPopulated() || !neighbors(p)) continue;
                budget--;
                int retries = queue.remove(p);
                saved.setDirty();
                boolean deferred = false;
                for (var phase : WorldGenPhase.values()) {
                    if (!stamp.missing(phase)) continue;
                    if ((phase == WorldGenPhase.STRUCTURES
                                    || phase == WorldGenPhase.ORES
                                    || phase == WorldGenPhase.SURFACE)
                            && !neighbors(p)) {
                        deferred = true;
                        continue;
                    }
                    if (System.nanoTime() >= deadline) {
                        deferred = true;
                        break;
                    }
                    try {
                        if (runPhase(p, phase)) {
                            stamp.complete(phase);
                        } else deferred = true;
                    } catch (RuntimeException ex) {
                        com.mojang.logging.LogUtils.getLogger()
                                .warn("Retrogen {} phase {} failed", p, phase, ex);
                        deferred = true;
                    }
                }
                chunk.setUnsaved(true);
                if (deferred && retries < 40) queue.put(p, retries + 1);
                else if (deferred) skipped++;
                else processed++;
            }
        } finally {
            running = false;
        }
    }

    private boolean neighbors(ChunkPos p) {
        for (int x = -1; x <= 1; x++)
            for (int z = -1; z <= 1; z++)
                if (level.getChunkSource().getChunkNow(p.x + x, p.z + z) == null) return false;
        return true;
    }

    private boolean runPhase(ChunkPos p, WorldGenPhase phase) {
        if (phase == WorldGenPhase.SPRING || phase == WorldGenPhase.POST) return true;
        var random =
                RandomSource.create(
                        SpringVeinHolder.seed(level.getSeed(), p.x, p.z) ^ phase.ordinal());
        if (phase == WorldGenPhase.STRUCTURES) {
            for (var set : level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET)) {
                if (!set.placement()
                        .isStructureChunk(level.getChunkSource().getGeneratorState(), p.x, p.z))
                    continue;
                for (var selected : set.structures())
                    if (selected.structure().value()
                            instanceof
                            com.mpp.stellaeomphalos.content.world.structure.TempleStructure
                                            temple) {
                        int y =
                                level.getHeight(
                                                net.minecraft.world.level.levelgen.Heightmap.Types
                                                        .WORLD_SURFACE,
                                                p.getMinBlockX() + 7,
                                                p.getMinBlockZ() + 7)
                                        - temple.depth()
                                        - 1;
                        var origin = new BlockPos(p.getMinBlockX(), y, p.getMinBlockZ());
                        if (!temple.biomes().contains(level.getBiome(origin))) continue;
                        if (AstrolabeLedger.get(level)
                                .nearest(
                                        temple.templateId(),
                                        net.minecraft.world.phys.Vec3.atCenterOf(origin),
                                        8)
                                .isPresent()) continue;
                        var template = level.getStructureManager().get(temple.templateId());
                        if (template.isEmpty()) continue;
                        var settings =
                                new net.minecraft.world.level.levelgen.structure.templatesystem
                                        .StructurePlaceSettings();
                        var box = template.get().getBoundingBox(settings, origin);
                        boolean safe = true;
                        for (var at :
                                BlockPos.betweenClosed(
                                        box.minX(),
                                        box.minY(),
                                        box.minZ(),
                                        box.maxX(),
                                        box.maxY(),
                                        box.maxZ()))
                            if (!level.hasChunkAt(at) || level.getBlockEntity(at) != null) {
                                safe = false;
                                break;
                            }
                        if (!safe) continue;
                        if (template.get().placeInWorld(level, origin, origin, settings, random, 2))
                            AstrolabeLedger.get(level).mark(temple.templateId(), origin);
                    }
            }
            return true;
        }
        var registry = level.registryAccess().registryOrThrow(Registries.PLACED_FEATURE);
        List<String> names =
                phase == WorldGenPhase.ORES
                        ? List.of(
                                "geode_ore", "astral_ore", "aquamarine_sand", "marble_vein")
                        : List.of("glowbloom_patch", "sky_crystal_cluster_patch", "prism_crystal_cluster_patch");
        for (var name : names) {
            var feature = registry.get(new ResourceLocation("stellaeomphalos", name));
            if (feature != null)
                feature.place(
                        level,
                        level.getChunkSource().getGenerator(),
                        random,
                        new BlockPos(p.getMinBlockX(), 0, p.getMinBlockZ()));
        }
        return true;
    }
}

package com.mpp.stellaeomphalos.structure.match;

import com.mpp.stellaeomphalos.OmphalosConfig;
import com.mpp.stellaeomphalos.structure.pattern.*;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.*;
import net.minecraftforge.event.level.*;

import java.util.*;

/** Per-level runtime ownership. Every world read is preceded by a loaded-chunk check. */
public final class StructureIntegrityHub {
    private static final Map<ServerLevel, StructureIntegrityHub> LEVELS = new IdentityHashMap<>();
    private final ServerLevel level;
    private final Map<BlockPos, StructureWatch> watches = new LinkedHashMap<>();
    private final Map<ChunkPos, Set<StructureWatch>> byChunk = new HashMap<>();
    private final Set<BlockPos> pending = new LinkedHashSet<>();
    private final Set<ChunkPos> dirty = new LinkedHashSet<>();
    private int epoch = -1, bulkDepth;

    private StructureIntegrityHub(ServerLevel level) {
        this.level = level;
    }

    public static StructureIntegrityHub of(ServerLevel level) {
        return LEVELS.computeIfAbsent(level, StructureIntegrityHub::new);
    }

    public static void attach() {
        var bus = MinecraftForge.EVENT_BUS;
        bus.addListener(StructureIntegrityHub::neighbor);
        bus.addListener(StructureIntegrityHub::place);
        bus.addListener(StructureIntegrityHub::broken);
        bus.addListener(StructureIntegrityHub::explode);
        bus.addListener(StructureIntegrityHub::chunkLoad);
        bus.addListener(StructureIntegrityHub::chunkUnload);
        bus.addListener(StructureIntegrityHub::tick);
        bus.addListener(StructureIntegrityHub::unload);
    }

    public StructureWatch observe(BlockPos origin, ResourceLocation id, PlacementTransform t) {
        reconcile();
        var current = watches.get(origin);
        if (current != null && current.blueprintId().equals(id) && current.transform() == t)
            return current;
        release(origin);
        var watch = new StructureWatch(level, origin, id, t);
        watches.put(origin.immutable(), watch);
        index(watch);
        return watch;
    }

    private void index(StructureWatch w) {
        w.chunks().forEach(c -> byChunk.computeIfAbsent(c, k -> new HashSet<>()).add(w));
    }

    public void release(BlockPos origin) {
        var watch = watches.remove(origin);
        if (watch == null) return;
        for (var c : watch.chunks()) {
            var set = byChunk.get(c);
            if (set != null) {
                set.remove(watch);
                if (set.isEmpty()) byChunk.remove(c);
            }
        }
        watch.close();
    }

    private void reconcile() {
        if (epoch == BlueprintRegistry.epoch()) return;
        epoch = BlueprintRegistry.epoch();
        byChunk.clear();
        for (var w : watches.values()) {
            w.refresh();
            index(w);
            w.publish();
        }
    }

    public void changed(BlockPos p) {
        var c = new ChunkPos(p);
        if (!byChunk.containsKey(c)) return;
        if (bulkDepth > 0 || pending.size() >= 64) {
            dirty.add(c);
        } else pending.add(p.immutable());
    }

    public StructureState query(BlockPos p, ResourceLocation id) {
        var w = observe(p, id, PlacementTransform.NONE);
        flush();
        w.publish();
        return w.state();
    }

    public void flush() {
        reconcile();
        for (var c : dirty)
            for (var w : List.copyOf(byChunk.getOrDefault(c, Set.of())))
                w.chunk(c, level.hasChunk(c.x, c.z));
        dirty.clear();
        for (var p : pending)
            for (var w : byChunk.getOrDefault(new ChunkPos(p), Set.of())) w.changed(p);
        pending.clear();
    }

    public AutoCloseable beginBulk() {
        bulkDepth++;
        return () -> {
            if (--bulkDepth == 0) flush();
        };
    }

    public int revalidate() {
        watches.values().forEach(StructureWatch::verify);
        return watches.size();
    }

    public Collection<StructureWatch> watches() {
        return List.copyOf(watches.values());
    }

    private void advance() {
        flush();
        int interval = OmphalosConfig.SERVER.integer("structure.reverifyInterval");
        for (var w : List.copyOf(watches.values())) {
            if (level.hasChunkAt(w.origin())
                    && !(level.getBlockEntity(w.origin()) instanceof StructureDependent)
                    && !level.getBlockState(w.origin()).hasBlockEntity()) {
                release(w.origin());
                continue;
            }
            w.tick(interval);
        }
    }

    private static void neighbor(BlockEvent.NeighborNotifyEvent e) {
        if (e.getLevel() instanceof ServerLevel s) {
            var h = of(s);
            h.changed(e.getPos());
            e.getNotifiedSides().forEach(d -> h.changed(e.getPos().relative(d)));
        }
    }

    private static void place(BlockEvent.EntityPlaceEvent e) {
        if (e.getLevel() instanceof ServerLevel s) of(s).changed(e.getPos());
    }

    private static void broken(BlockEvent.BreakEvent e) {
        if (e.getLevel() instanceof ServerLevel s) of(s).changed(e.getPos());
    }

    private static void explode(ExplosionEvent.Detonate e) {
        if (e.getLevel() instanceof ServerLevel s) e.getAffectedBlocks().forEach(of(s)::changed);
    }

    private static void chunkLoad(ChunkEvent.Load e) {
        if (e.getLevel() instanceof ServerLevel s) {
            var h = of(s);
            var c = e.getChunk().getPos();
            h.dirty.add(c);
        }
    }

    private static void chunkUnload(ChunkEvent.Unload e) {
        if (e.getLevel() instanceof ServerLevel s) {
            var h = of(s);
            var c = e.getChunk().getPos();
            for (var w : List.copyOf(h.byChunk.getOrDefault(c, Set.of()))) w.chunk(c, false);
        }
    }

    private static void tick(TickEvent.LevelTickEvent e) {
        if (e.phase == TickEvent.Phase.END && e.level instanceof ServerLevel s) of(s).advance();
    }

    private static void unload(LevelEvent.Unload e) {
        if (e.getLevel() instanceof ServerLevel s) {
            var h = LEVELS.remove(s);
            if (h != null) h.watches.values().forEach(StructureWatch::close);
        }
    }
}

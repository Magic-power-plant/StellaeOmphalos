package com.mpp.stellaeomphalos.player.boon.root;

import com.mpp.stellaeomphalos.player.boon.BoonProgress;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Aevitas root behavior: placing blocks grants experience. A rolling history of the last
 * {@value #HISTORY} placements (persisted in the root node's private data) scales the gain by
 * block-type diversity, decaying to a {@value #MIN_FACTOR} floor for monotone placement.
 * Re-placing a block at a position the player already placed at grants nothing, so
 * break-and-replace loops do not farm experience.
 */
public final class VerdanceRootBoon {

    public static final String SIGN = "aevitas";
    public static final long BASE_EXP = 4;
    public static final int HISTORY = 20;
    public static final double MIN_FACTOR = 0.4;
    private static final int POSITION_MEMORY = 256;

    private record Pos(ResourceKey<Level> dim, long pos) {}

    private static final Map<UUID, LinkedHashMap<Pos, Boolean>> AWARDED = new LinkedHashMap<>();

    private VerdanceRootBoon() {}

    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getLevel().isClientSide()) return;
        if (!RootExpSupport.active(player, SIGN)) return;
        var key = new Pos(player.level().dimension(), event.getPos().asLong());
        var memory = AWARDED.computeIfAbsent(player.getUUID(), id -> new LinkedHashMap<>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Pos, Boolean> eldest) {
                return size() > POSITION_MEMORY;
            }
        });
        if (memory.containsKey(key)) return;    // break-and-replace does not pay twice
        memory.put(key, Boolean.TRUE);

        var progress = BoonProgress.getServer(player);
        var data = progress.nodeData(RootExpSupport.rootId(SIGN));
        var history = readHistory(data.getList("Diversity", Tag.TAG_STRING));
        double factor = diversityFactor(history);
        var blockId = ForgeRegistries.BLOCKS.getKey(event.getPlacedBlock().getBlock());
        history.add(blockId == null ? "minecraft:air" : blockId.toString());
        while (history.size() > HISTORY) history.remove(0);
        var tag = new ListTag();
        history.forEach(entry -> tag.add(StringTag.valueOf(entry)));
        data.put("Diversity", tag);
        progress.persist(player);
        RootExpSupport.grant(player, SIGN, BASE_EXP * factor);
    }

    /** Diversity multiplier of a placement history: fully diverse pays 1.0, decaying to a 0.4 floor. */
    public static double diversityFactor(List<String> history) {
        if (history.isEmpty()) return 1.0;
        long unique = history.stream().distinct().count();
        return Math.max(MIN_FACTOR, unique / (double) history.size());
    }

    private static java.util.ArrayList<String> readHistory(ListTag tag) {
        var history = new java.util.ArrayList<String>();
        tag.forEach(entry -> history.add(entry.getAsString()));
        while (history.size() > HISTORY) history.remove(0);
        return history;
    }

    static void clear(UUID player) {
        AWARDED.remove(player);
    }

    static void clearAll() {
        AWARDED.clear();
    }
}

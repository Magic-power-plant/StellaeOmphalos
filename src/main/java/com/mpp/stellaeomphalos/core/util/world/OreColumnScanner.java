package com.mpp.stellaeomphalos.core.util.world;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** Resumable column scan; absent chunks are skipped without forcing a load. */
public final class OreColumnScanner {
    private final Iterator<BlockPos> columns;
    private final int minY;
    private final int maxY;
    private final TagKey<Block> ores;
    private final List<BlockPos> found = new ArrayList<>();
    private BlockPos column;
    private int y;
    public OreColumnScanner(BlockPos center, int radius, int minY, int maxY, TagKey<Block> ores) {
        if (minY > maxY || maxY - (long) minY > 4096 || radius > 128) throw new IllegalArgumentException("Invalid ore scan bounds");
        columns = new SpiralBlockWalker(center, radius).iterator(); this.minY = minY; this.maxY = maxY; this.ores = ores;
        column = columns.next(); y = minY;
    }
    public boolean advance(Level level, int budget) {
        if (budget < 1) throw new IllegalArgumentException("Invalid scan budget");
        for (int i = 0; i < budget && column != null; i++) {
            var pos = new BlockPos(column.getX(), y, column.getZ());
            if (level.hasChunkAt(pos) && !level.isOutsideBuildHeight(pos) && level.getBlockState(pos).is(ores)) found.add(pos);
            if (++y > maxY) { column = columns.hasNext() ? columns.next() : null; y = minY; }
        }
        return column == null;
    }
    public List<BlockPos> result() { return List.copyOf(found); }
}

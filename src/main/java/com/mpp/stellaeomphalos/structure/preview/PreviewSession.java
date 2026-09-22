package com.mpp.stellaeomphalos.structure.preview;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

/** Side-neutral preview contract; rendering is owned by client/structure. */
public final class PreviewSession {
    private final ResourceLocation blueprintId;
    private final BlockPos origin;
    private final long token;
    private int ttl;
    private int revision;
    public int revision(){return revision;}
    private final Map<Long, Integer> cells = new LinkedHashMap<>();

    public PreviewSession(ResourceLocation id, BlockPos origin, long token, int ttl) {
        blueprintId = id;
        this.origin = origin.immutable();
        this.token = token;
        this.ttl = ttl;
    }

    public ResourceLocation blueprintId() {
        return blueprintId;
    }

    public BlockPos origin() {
        return origin;
    }

    public long token() {
        return token;
    }

    public boolean tick() {
        return --ttl > 0;
    }

    public Map<Long, Integer> cells() {
        return Collections.unmodifiableMap(cells);
    }

    public void update(long token, boolean reset, List<Long> positions, List<Integer> states) {
        if (this.token != token) return;
        revision++;
        if (reset) cells.clear();
        for (int i = 0; i < positions.size() && cells.size() < 4096; i++) {
            if (states.get(i) < 0) cells.remove(positions.get(i));
            else cells.put(positions.get(i), states.get(i));
        }
    }
}

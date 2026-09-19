package com.mpp.stellaeomphalos.core.util.world;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Scope-bound drop interception. The caller must consume captured stacks before closing. */
public final class BlockDropCollector implements AutoCloseable {
    private final ServerLevel level;
    private final AABB bounds;
    private final List<ItemStack> captured = new ArrayList<>();
    private boolean closed;
    public BlockDropCollector(ServerLevel level, AABB bounds) {
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Capture off server thread");
        this.level = level; this.bounds = bounds; MinecraftForge.EVENT_BUS.register(this);
    }
    @SubscribeEvent public void capture(EntityJoinLevelEvent event) {
        if (!closed && !event.isCanceled() && event.getLevel() == level && event.getEntity() instanceof ItemEntity item && bounds.contains(item.position())) {
            captured.add(item.getItem().copy()); event.setCanceled(true);
        }
    }
    public List<ItemStack> drain() { var result = captured.stream().map(ItemStack::copy).toList(); captured.clear(); return result; }
    @Override public void close() {
        if (closed) return;
        closed = true; MinecraftForge.EVENT_BUS.unregister(this);
        var center = bounds.getCenter();
        for (var stack : captured) level.addFreshEntity(new ItemEntity(level, center.x, center.y, center.z, stack));
        captured.clear();
    }
}

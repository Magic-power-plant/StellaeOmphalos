package com.mpp.stellaeomphalos.core.bootstrap;

import com.mpp.stellaeomphalos.Omphalos;
import com.mpp.stellaeomphalos.core.util.world.BlockChangeNotice;
import com.mpp.stellaeomphalos.core.util.world.DimensionPos;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Captures pre-change state then publishes the actual outcome at the next server tick. */
@Mod.EventBusSubscriber(modid = Omphalos.MODID)
public final class BlockChangeDispatcher {
    private BlockChangeDispatcher() {}
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void broken(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level) enqueue(level, event.getPos(), event.getState());
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void placed(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (event instanceof BlockEvent.EntityMultiPlaceEvent multiple) {
            multiple.getReplacedBlockSnapshots().forEach(snapshot -> enqueue(level, snapshot.getPos(), snapshot.getReplacedBlock()));
        } else enqueue(level, event.getPos(), event.getBlockSnapshot().getReplacedBlock());
    }
    private static void enqueue(ServerLevel level, BlockPos pos, BlockState before) {
        var address = new DimensionPos(level.dimension(), pos);
        var server = level.getServer();
        RuntimeServices.current().scheduler().schedule(0, () -> {
            var current = server.getLevel(address.dimension());
            if (current == null || !current.hasChunkAt(address.position())) return;
            var after = current.getBlockState(address.position());
            if (!before.equals(after)) MinecraftForge.EVENT_BUS.post(new BlockChangeNotice(address, before, after));
        });
    }
}

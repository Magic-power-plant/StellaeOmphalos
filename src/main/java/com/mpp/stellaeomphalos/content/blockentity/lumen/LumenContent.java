package com.mpp.stellaeomphalos.content.blockentity.lumen;

import com.mpp.stellaeomphalos.Omphalos;
import com.mpp.stellaeomphalos.core.bootstrap.RegistrationGuard;
import com.mpp.stellaeomphalos.core.registry.ModBlockEntities;
import com.mpp.stellaeomphalos.core.registry.ModBlocks;
import com.mpp.stellaeomphalos.core.registry.ModItems;
import com.mpp.stellaeomphalos.lumen.capability.LumenNode;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.common.Mod;

/**
 * Demonstration/content landing for the lumen module: a sky-fed source (collector), a relay and a
 * buffered sink (battery), plus their BlockItems. Registrations self-attach through the mod-bus
 * subscriber annotation; textures are intentionally absent (resource order R-2.x covers them).
 */
@Mod.EventBusSubscriber(modid = Omphalos.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class LumenContent {
    public static final long COLLECTOR_BASE_OUTPUT = 1000;
    public static final long BATTERY_CAPACITY = 100000;

    public static final RegistrationGuard<CollectorBlock> COLLECTOR_BLOCK =
            ModBlocks.ENTRIES.declare("lumen_collector", CollectorBlock::new);
    public static final RegistrationGuard<BlockItem> COLLECTOR_ITEM =
            ModItems.ENTRIES.declare("lumen_collector", () -> new BlockItem(COLLECTOR_BLOCK.get(), new Item.Properties()));
    public static final RegistrationGuard<BlockEntityType<Collector>> COLLECTOR =
            ModBlockEntities.ENTRIES.declare("lumen_collector",
                    () -> BlockEntityType.Builder.of(Collector::new, COLLECTOR_BLOCK.get()).build(null));

    public static final RegistrationGuard<RelayBlock> RELAY_BLOCK =
            ModBlocks.ENTRIES.declare("lumen_relay", RelayBlock::new);
    public static final RegistrationGuard<BlockItem> RELAY_ITEM =
            ModItems.ENTRIES.declare("lumen_relay", () -> new BlockItem(RELAY_BLOCK.get(), new Item.Properties()));
    public static final RegistrationGuard<BlockEntityType<Relay>> RELAY =
            ModBlockEntities.ENTRIES.declare("lumen_relay",
                    () -> BlockEntityType.Builder.of(Relay::new, RELAY_BLOCK.get()).build(null));

    public static final RegistrationGuard<BatteryBlock> BATTERY_BLOCK =
            ModBlocks.ENTRIES.declare("lumen_battery", BatteryBlock::new);
    public static final RegistrationGuard<BlockItem> BATTERY_ITEM =
            ModItems.ENTRIES.declare("lumen_battery", () -> new BlockItem(BATTERY_BLOCK.get(), new Item.Properties()));
    public static final RegistrationGuard<BlockEntityType<Battery>> BATTERY =
            ModBlockEntities.ENTRIES.declare("lumen_battery",
                    () -> BlockEntityType.Builder.of(Battery::new, BATTERY_BLOCK.get()).build(null));

    private LumenContent() {}

    private abstract static class LumenNodeBlock extends Block implements EntityBlock {
        LumenNodeBlock() { super(BlockBehaviour.Properties.of().strength(1.5f)); }
        @Override
        public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
            if (level.getBlockEntity(pos) instanceof LumenNode node) node.onNeighborChanged(level, fromPos);
        }
    }

    public static final class CollectorBlock extends LumenNodeBlock {
        @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new Collector(pos, state); }
        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            return LumenTickBlockEntity.ticker();
        }
    }

    public static final class RelayBlock extends LumenNodeBlock {
        @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new Relay(pos, state); }
        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            return LumenTickBlockEntity.ticker();
        }
    }

    public static final class BatteryBlock extends LumenNodeBlock {
        @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new Battery(pos, state); }
    }

    /** Demo source: produces while it sees the sky, neutral (sign-less) distribution channel. */
    public static final class Collector extends LumenSourceBlockEntity {
        public Collector(BlockPos pos, BlockState state) {
            super(COLLECTOR.get(), pos, state, new ResourceLocation(Omphalos.MODID, "lumen_collector"), COLLECTOR_BASE_OUTPUT);
        }
    }

    /** Demo relay: pure transit. */
    public static final class Relay extends LumenRelayBlockEntity {
        public Relay(BlockPos pos, BlockState state) { super(RELAY.get(), pos, state); }
    }

    /** Demo sink: 100000 LU long buffer, stored amount synced via the client state channel. */
    public static final class Battery extends LumenSinkBlockEntity {
        public Battery(BlockPos pos, BlockState state) { super(BATTERY.get(), pos, state, BATTERY_CAPACITY); }
    }
}

package com.mpp.stellaeomphalos.content.blockentity;

import com.mpp.stellaeomphalos.Omphalos;
import com.mpp.stellaeomphalos.core.bootstrap.RegistrationGuard;
import com.mpp.stellaeomphalos.core.registry.ModBlockEntities;
import com.mpp.stellaeomphalos.core.registry.ModBlocks;
import com.mpp.stellaeomphalos.core.registry.ModItems;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Omphalos.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.DEDICATED_SERVER)
public final class FoundationTestContent {
    public static final RegistrationGuard<ProbeBlock> BLOCK = ModBlocks.ENTRIES.declare("foundation_probe", ProbeBlock::new);
    public static final RegistrationGuard<BlockItem> ITEM = ModItems.ENTRIES.declare("foundation_probe", () -> new BlockItem(BLOCK.get(), new Item.Properties()));
    public static final RegistrationGuard<BlockEntityType<ProbeInventory>> TYPE = ModBlockEntities.ENTRIES.declare("foundation_probe",
            () -> BlockEntityType.Builder.of(ProbeInventory::new, BLOCK.get()).build(null));
    private FoundationTestContent() {}
    public static final class ProbeBlock extends Block implements EntityBlock {
        public ProbeBlock() { super(BlockBehaviour.Properties.of().strength(1)); }
        @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ProbeInventory(pos, state); }
    }
    public static final class ProbeInventory extends InventoryBlockEntity {
        public ProbeInventory(BlockPos pos, BlockState state) { super(TYPE.get(), pos, state, 2, EnumSet.of(Direction.UP)); }
        @Override protected boolean acceptsItem(int slot, ItemStack stack) { return stack.is(Items.DIAMOND); }
        @Override protected void writeMachineState(CompoundTag tag) { tag.putInt("MachineValue", 7); }
        @Override protected void readMachineState(CompoundTag tag) {}
        @Override protected void writeClientState(CompoundTag tag) { tag.putInt("DisplayValue", 1); }
        @Override protected void readClientState(CompoundTag tag) {}
        @Override protected void serverTick() {}
        public int overflowCount() { return overflow().stream().mapToInt(ItemStack::getCount).sum(); }
    }
}

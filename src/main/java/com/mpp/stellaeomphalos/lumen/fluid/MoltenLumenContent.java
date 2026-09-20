package com.mpp.stellaeomphalos.lumen.fluid;

import com.mpp.stellaeomphalos.core.bootstrap.RegistrationGuard;
import com.mpp.stellaeomphalos.core.registry.ModBlocks;
import com.mpp.stellaeomphalos.core.registry.ModFluids;
import com.mpp.stellaeomphalos.core.registry.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.fluids.ForgeFlowingFluid;

/** Content declarations for molten lumen; class loading must happen before registry events fire. */
public final class MoltenLumenContent {
    public static final RegistrationGuard<MoltenLumenFluidType> TYPE =
            ModFluids.TYPES.declare("molten_lumen", MoltenLumenFluidType::new);
    public static final RegistrationGuard<MoltenLumenFluid.Source> STILL =
            ModFluids.FLUIDS.declare("molten_lumen", () -> new MoltenLumenFluid.Source(FluidProperties.INSTANCE));
    public static final RegistrationGuard<MoltenLumenFluid.Flowing> FLOWING =
            ModFluids.FLUIDS.declare("molten_lumen_flowing", () -> new MoltenLumenFluid.Flowing(FluidProperties.INSTANCE));
    public static final RegistrationGuard<MoltenLumenBlock> BLOCK =
            ModBlocks.ENTRIES.declare("molten_lumen", () -> new MoltenLumenBlock(STILL));
    public static final RegistrationGuard<BucketItem> BUCKET =
            ModItems.ENTRIES.declare("molten_lumen_bucket",
                    () -> new BucketItem(STILL, new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
    public static final RegistrationGuard<Item> GEODE_SHARD =
            ModItems.ENTRIES.declare("geode_shard", () -> new Item(new Item.Properties()));
    public static final RegistrationGuard<RotatedPillarBlock> INFUSED_LOG =
            ModBlocks.ENTRIES.declare("infused_log",
                    () -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LOG)));
    public static final RegistrationGuard<BlockItem> INFUSED_LOG_ITEM =
            ModItems.ENTRIES.declare("infused_log", () -> new BlockItem(INFUSED_LOG.get(), new Item.Properties()));

    /** Lazily initialized so the guards above exist before the fluid factories resolve. */
    private static final class FluidProperties {
        private static final ForgeFlowingFluid.Properties INSTANCE =
                new ForgeFlowingFluid.Properties(TYPE, STILL, FLOWING).block(BLOCK).bucket(BUCKET);
    }

    private MoltenLumenContent() {}

    /** Forces class loading so the deferred declarations above reach the registry events. */
    public static void init() {}
}

package com.mpp.stellaeomphalos.core.bootstrap;

import com.mpp.stellaeomphalos.Omphalos;
import com.mpp.stellaeomphalos.OmphalosConfig;
import com.mpp.stellaeomphalos.core.platform.api.EnchantmentQueryEvent;
import com.mpp.stellaeomphalos.data.loader.DataBootstrap;
import com.mpp.stellaeomphalos.lumen.fluid.PrecisionTank;
import com.mpp.stellaeomphalos.lumen.fluid.SingleTank;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Omphalos.MODID)
@PrefixGameTestTemplate(false)
public final class FoundationGameTests {
    @GameTest(template = "foundation_empty")
    public static void server_bootstrap_and_data(GameTestHelper helper) {
        helper.assertTrue(LifecycleOrchestrator.phase() == LifecycleOrchestrator.Phase.LOADED, "Lifecycle incomplete");
        helper.assertTrue(RegistryBootstrap.families().size() == 23, "Registry family missing");
        helper.assertTrue(OmphalosConfig.COMMON.spec().isLoaded() && OmphalosConfig.SERVER.spec().isLoaded(), "Configs not loaded");
        helper.assertTrue(DataBootstrap.TABLES.report().isEmpty(), "Unexpected data load errors");
        helper.assertTrue(!DataBootstrap.TABLES.entries(DataBootstrap.VISIBLE_DIMENSIONS).isEmpty(), "Policy data not loaded");
        helper.assertTrue(RuntimeServices.current().scheduler().currentTick() >= 0, "Server services not ready");
        helper.succeed();
    }
    @GameTest(template = "foundation_empty")
    public static void fluid_storage_simulation_and_roundtrip(GameTestHelper helper) {
        var tank = new SingleTank(1000, () -> {});
        helper.assertTrue(tank.fill(new FluidStack(Fluids.WATER, 1500), FluidAction.SIMULATE) == 1000 && tank.storedUnits() == 0, "Simulation mutated state");
        helper.assertTrue(tank.fill(new FluidStack(Fluids.WATER, 1500), FluidAction.EXECUTE) == 1000, "Tank capacity wrong");
        helper.assertTrue(tank.fill(new FluidStack(Fluids.LAVA, 100), FluidAction.EXECUTE) == 0, "Mixed fluids");
        var restored = new SingleTank(1000, () -> {}); restored.restore(tank.save());
        helper.assertTrue(restored.drain(300, FluidAction.EXECUTE).getAmount() == 300 && restored.storedUnits() == 700, "Tank restore/drain failed");
        var precise = new PrecisionTank(1, () -> {});
        precise.fillSubunits(new FluidStack(Fluids.WATER, 1), 999999, FluidAction.EXECUTE);
        helper.assertTrue(precise.drain(1, FluidAction.EXECUTE).isEmpty(), "Fractional fluid exposed as whole mB");
        helper.assertTrue(precise.fill(new FluidStack(Fluids.WATER, 1), FluidAction.EXECUTE) == 0, "Whole-unit fill must not consume partial space");
        helper.assertTrue(precise.storedUnits() == 999999, "Whole-unit fill silently stored unreported fluid");
        helper.succeed();
    }
    @GameTest(template = "foundation_empty")
    public static void enchantment_extension_is_live_and_does_not_mutate_item(GameTestHelper helper) {
        var subscriber = new EnchantmentSubscriber(); MinecraftForge.EVENT_BUS.register(subscriber);
        try {
            var stack = new ItemStack(Items.DIAMOND_PICKAXE); stack.enchant(Enchantments.UNBREAKING, 1);
            helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.UNBREAKING, stack) == 3, "Single-level hook missing");
            helper.assertTrue(EnchantmentHelper.getEnchantments(stack).get(Enchantments.UNBREAKING) == 3, "Map hook missing");
            helper.assertTrue(EnchantmentHelper.getTagEnchantmentLevel(Enchantments.UNBREAKING, stack) == 1, "Hook modified persistent NBT");
        } finally { MinecraftForge.EVENT_BUS.unregister(subscriber); }
        helper.succeed();
    }
    public static final class EnchantmentSubscriber {
        @SubscribeEvent public void query(EnchantmentQueryEvent event) { event.setLevel(Enchantments.UNBREAKING, 3); }
    }
    @GameTest(template = "foundation_empty")
    public static void config_reload_publishes_a_new_snapshot(GameTestHelper helper) {
        var config = net.minecraftforge.fml.config.ConfigTracker.INSTANCE.configSets().get(net.minecraftforge.fml.config.ModConfig.Type.SERVER)
                .stream().filter(entry -> entry.getSpec() == OmphalosConfig.SERVER.spec()).findFirst().orElseThrow();
        net.minecraftforge.common.ForgeConfigSpec.IntValue value = OmphalosConfig.SERVER.spec().getValues().get(java.util.List.of("progression", "maxBoonLevel"));
        int oldValue = value.get(); int revision = OmphalosConfig.serverRevision();
        var container = net.minecraftforge.fml.ModList.get().getModContainerById(Omphalos.MODID).orElseThrow();
        try {
            value.set(oldValue + 1);
            container.dispatchConfigEvent(new net.minecraftforge.fml.event.config.ModConfigEvent.Reloading(config));
            helper.assertTrue(OmphalosConfig.SERVER.integer("progression.maxBoonLevel") == oldValue + 1, "Stale server config snapshot");
            helper.assertTrue(OmphalosConfig.serverRevision() > revision, "Config revision did not change");
        } finally {
            value.set(oldValue);
            container.dispatchConfigEvent(new net.minecraftforge.fml.event.config.ModConfigEvent.Reloading(config));
        }
        helper.succeed();
    }
    @GameTest(template = "foundation_empty")
    public static void frame_changes_keep_original_state_and_growth_scope_restores(GameTestHelper helper) {
        var origin = helper.absolutePos(net.minecraft.core.BlockPos.ZERO);
        var level = helper.getLevel();
        var before = level.getBlockState(origin);
        var changes = new com.mpp.stellaeomphalos.structure.match.FrameChangeSet();
        changes.add(net.minecraft.core.BlockPos.ZERO, before, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        changes.add(net.minecraft.core.BlockPos.ZERO, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState());
        helper.assertTrue(changes.snapshot().size() == 1 && changes.snapshot().get(0).before().equals(before)
                && changes.snapshot().get(0).after().is(net.minecraft.world.level.block.Blocks.DIRT), "Change coalescing lost original state");
        try (var capture = new com.mpp.stellaeomphalos.core.util.world.SaplingCaptureManager(level, origin)) {
            level.setBlock(origin, net.minecraft.world.level.block.Blocks.OAK_LOG.defaultBlockState(), 3);
            helper.assertTrue(capture.snapshot().size() == 1, "Growth capture empty");
        }
        helper.assertTrue(level.getBlockState(origin).equals(before) && !level.captureBlockSnapshots && !level.restoringBlockSnapshots, "Growth capture leaked state");
        helper.succeed();
    }
    @GameTest(template = "foundation_empty")
    public static void deferred_cross_registry_factory_and_inventory_lifecycle(GameTestHelper helper) {
        var pos = helper.absolutePos(net.minecraft.core.BlockPos.ZERO); var level = helper.getLevel();
        var block = com.mpp.stellaeomphalos.content.blockentity.FoundationTestContent.BLOCK.get();
        helper.assertTrue(com.mpp.stellaeomphalos.content.blockentity.FoundationTestContent.ITEM.get().getBlock() == block,
                "Deferred cross-registry reference did not resolve");
        level.setBlock(pos, block.defaultBlockState(), 3);
        var entity = (com.mpp.stellaeomphalos.content.blockentity.FoundationTestContent.ProbeInventory) level.getBlockEntity(pos);
        var cap = entity.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER, net.minecraft.core.Direction.UP);
        var inventory = cap.orElseThrow(() -> new IllegalStateException("Missing item capability"));
        helper.assertTrue(!entity.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER, net.minecraft.core.Direction.DOWN).isPresent(), "Disallowed face exposed inventory");
        helper.assertTrue(inventory.insertItem(0, new ItemStack(Items.DIRT), false).getCount() == 1, "Item filter bypassed");
        helper.assertTrue(inventory.insertItem(0, new ItemStack(Items.DIAMOND, 3), false).isEmpty(), "Valid item rejected");
        var saved = entity.saveWithFullMetadata();
        helper.assertTrue(saved.contains("Inventory") && !entity.getUpdateTag().contains("Inventory"), "Inventory leaked into client update tag");
        var extra = new ItemStack(Items.DIAMOND, 5).save(new net.minecraft.nbt.CompoundTag()); extra.putInt("Slot", 4);
        saved.getCompound("Inventory").getList("Items", net.minecraft.nbt.Tag.TAG_COMPOUND).add(extra);
        entity.load(saved);
        helper.assertTrue(inventory.getStackInSlot(0).getCount() == 3 && entity.overflowCount() == 5, "Slot migration lost items");
        entity.invalidateCaps(); helper.assertTrue(!cap.isPresent(), "Capability survived invalidation");
        entity.reviveCaps(); helper.assertTrue(entity.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER).isPresent(), "Capability failed to revive");
        helper.succeed();
    }
}

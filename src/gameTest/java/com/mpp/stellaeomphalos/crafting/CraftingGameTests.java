package com.mpp.stellaeomphalos.crafting;

import com.google.gson.*;
import com.mpp.stellaeomphalos.Omphalos;
import com.mpp.stellaeomphalos.content.blockentity.crafting.*;
import com.mpp.stellaeomphalos.core.platform.RecipeDefinition;
import com.mpp.stellaeomphalos.crafting.altar.menu.*;
import com.mpp.stellaeomphalos.crafting.altar.recipe.*;
import com.mpp.stellaeomphalos.crafting.grinding.*;
import com.mpp.stellaeomphalos.crafting.infusion.*;
import com.mpp.stellaeomphalos.crafting.special.*;
import com.mpp.stellaeomphalos.crafting.transmutation.*;
import com.mpp.stellaeomphalos.data.codec.*;
import com.mpp.stellaeomphalos.data.registry.*;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.*;
import net.minecraftforge.items.ItemStackHandler;

import java.util.*;

@GameTestHolder(Omphalos.MODID)
@PrefixGameTestTemplate(false)
public final class CraftingGameTests {
    private static JsonObject json(String text) {
        return JsonParser.parseString(text).getAsJsonObject();
    }

    private static ResourceLocation id(String name) {
        return new ResourceLocation("part_three_test", name);
    }

    private static JsonObject altarJson(int duration) {
        return json(
                "{\"type\":\"stellaeomphalos:asterism_crafting\",\"tier\":\"discovery\",\"lumen\":0,\"duration\":"
                        + duration
                        + ",\"grid\":{\"pattern\":[\"N\"],\"key\":{\"N\":{\"item\":\"minecraft:nether_star\"}}},\"result\":{\"kind\":\"static\",\"stack\":{\"item\":\"minecraft:diamond\"}}}");
    }

    private static AsterismRecipeInput input(AsterismTier tier, ItemStack stack) {
        var stacks = new ArrayList<ItemStack>(Collections.nCopies(26, ItemStack.EMPTY));
        stacks.set(4, stack);
        return new AsterismRecipeInput(tier, 16000, null, key -> false, stacks);
    }

    @GameTest(template = "foundation_empty", batch = "crafting")
    public static void generated_catalogue_loads_every_recipe_and_network_codec(GameTestHelper h) {
        int custom = 0;
        var manager = h.getLevel().getRecipeManager();
        for (var entry : CraftingDataProvider.recipes().entrySet()) {
            var name = new ResourceLocation(Omphalos.MODID, entry.getKey());
            var recipe =
                    manager.byKey(name)
                            .orElseThrow(
                                    () -> new AssertionError("Missing generated recipe " + name));
            if (recipe instanceof RecipeDefinition) {
                custom++;
                for (var catalyst : ((RecipeDefinition) recipe).catalysts())
                    h.assertTrue(
                            net.minecraftforge.registries.ForgeRegistries.ITEMS.containsKey(
                                    catalyst),
                            "Unknown display catalyst: " + catalyst);
                roundtrip(recipe, h);
            }
        }
        h.assertTrue(custom >= 130, "Need >=130 custom recipes, found " + custom);
        h.assertTrue(RecipeCatalog.families().size() == 9, "Nine registered families");
        h.assertTrue(ResultOpRegistry.ids().size() >= 11, "Eleven result operations");
        h.succeed();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void roundtrip(Recipe recipe, GameTestHelper h) {
        var buffer = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            recipe.getSerializer().toNetwork(buffer, recipe);
            var copy = recipe.getSerializer().fromNetwork(recipe.getId(), buffer);
            h.assertTrue(
                    ((RecipeDefinition) recipe).contentHash()
                            == ((RecipeDefinition) copy).contentHash(),
                    "Network recipe differs");
        } finally {
            buffer.release();
        }
    }

    @GameTest(template = "foundation_empty", batch = "crafting")
    public static void strict_grid_auxiliary_focus_night_and_advancement_gates(GameTestHelper h) {
        var data = altarJson(10);
        var recipe = new AsterismRecipe(id("strict"), data);
        var input = input(AsterismTier.TRAIT, new ItemStack(Items.NETHER_STAR));
        h.assertTrue(recipe.matches(input, h.getLevel()), "Initial match");
        input.setItem(24, new ItemStack(Items.DIRT));
        h.assertTrue(!recipe.matches(input, h.getLevel()), "Unconfigured trait slot must be empty");
        input.setItem(24, ItemStack.EMPTY);
        data.addProperty("focus_sign", "stellaeomphalos:aevitas");
        h.assertTrue(
                !new AsterismRecipe(id("focus"), data).matches(input, h.getLevel()),
                "Focus cannot be spoofed");
        data.remove("focus_sign");
        data.addProperty("required_advancement", "minecraft:story/root");
        h.assertTrue(
                !new AsterismRecipe(id("gate"), data).matches(input, h.getLevel()),
                "Advancement is server gate");
        data.remove("required_advancement");
        data.add("flags", JsonParser.parseString("[\"night_only\"]"));
        var night = new AsterismRecipe(id("night"), data);
        long previous = h.getLevel().getDayTime();
        h.getLevel().setDayTime(6000);
        h.getLevel().updateSkyBrightness();
        h.assertTrue(!night.matches(input, h.getLevel()), "Daytime rejected");
        h.getLevel().setDayTime(18000);
        h.getLevel().updateSkyBrightness();
        h.assertTrue(night.matches(input, h.getLevel()), "Night accepted");
        h.getLevel().setDayTime(previous);
        h.getLevel().updateSkyBrightness();
        h.succeed();
    }

    @GameTest(template = "foundation_empty", batch = "crafting")
    public static void three_consumption_modes_and_atomic_rollback(GameTestHelper h) {
        var items = new ItemStackHandler(3);
        items.setStackInSlot(0, new ItemStack(Items.DIAMOND, 2));
        items.setStackInSlot(1, new ItemStack(Items.WATER_BUCKET));
        items.setStackInSlot(2, new ItemStack(Items.LAVA_BUCKET));
        var diamond = MaterialSpec.parse(json("{\"item\":\"minecraft:diamond\"}"));
        var water = MaterialSpec.parse(json("{\"item\":\"minecraft:water_bucket\"}"));
        var lava = MaterialSpec.parse(json("{\"fluid\":\"minecraft:lava\",\"amount\":1000}"));
        var plan =
                List.of(
                        AsterismConsumption.simulate(items, 0, diamond, true),
                        AsterismConsumption.simulate(items, 1, water, true),
                        AsterismConsumption.simulate(items, 2, lava, true));
        h.assertTrue(items.getStackInSlot(0).getCount() == 2, "Simulation consumed input");
        h.assertTrue(AsterismConsumption.apply(plan), "Commit");
        h.assertTrue(
                items.getStackInSlot(0).getCount() == 1
                        && items.getStackInSlot(1).is(Items.BUCKET)
                        && items.getStackInSlot(2).is(Items.BUCKET),
                "Three consumption modes");
        h.assertTrue(!AsterismConsumption.apply(plan), "Stale plan cannot duplicate output");
        var bad =
                new ItemStackHandler(2) {
                    private boolean fail = true;

                    @Override
                    public void setStackInSlot(int slot, ItemStack stack) {
                        super.setStackInSlot(slot, stack);
                        if (slot == 1 && stack.isEmpty() && fail) {
                            fail = false;
                            throw new IllegalStateException("Injected failure");
                        }
                    }
                };
        bad.setStackInSlot(0, new ItemStack(Items.DIAMOND));
        bad.setStackInSlot(1, new ItemStack(Items.DIAMOND));
        var rollback =
                List.of(
                        AsterismConsumption.simulate(bad, 0, diamond, true),
                        AsterismConsumption.simulate(bad, 1, diamond, true));
        h.assertTrue(!AsterismConsumption.apply(rollback), "Failure must roll back");
        h.assertTrue(
                bad.getStackInSlot(0).is(Items.DIAMOND) && bad.getStackInSlot(1).is(Items.DIAMOND),
                "All slots restored");
        h.succeed();
    }

    @GameTest(template = "foundation_empty", batch = "crafting")
    public static void dynamic_results_preserve_or_reset_nbt_and_shifted_source(GameTestHelper h) {
        var source = new ItemStack(Items.NETHER_STAR);
        source.getOrCreateTag().putString("CustomName", "Keep");
        var data = altarJson(1);
        data.add(
                "result",
                json(
                        "{\"kind\":\"inherit\",\"source_slot\":\"upper_left\",\"fallback\":{\"item\":\"minecraft:nether_star\"},\"ops\":[{\"op\":\"stellaeomphalos:set_boolean\",\"key\":\"Upgraded\",\"value\":true}]}"));
        var recipe = new AsterismRecipe(id("inherit"), data);
        var output =
                recipe.assemble(
                        input(AsterismTier.DISCOVERY, source), h.getLevel().registryAccess());
        h.assertTrue(
                output.getTag().getString("CustomName").equals("Keep")
                        && output.getTag().getBoolean("Upgraded"),
                "Mapped source must retain NBT");
        h.assertTrue(!source.getTag().contains("Upgraded"), "Assembly modified input");
        var fresh =
                new ResultSpec(
                        json(
                                "{\"kind\":\"static\",\"stack\":{\"item\":\"minecraft:stick\"},\"ops\":[{\"op\":\"stellaeomphalos:set_sign\",\"sign\":\"stellaeomphalos:aevitas\"}]}"));
        var wand = fresh.assemble(i -> source);
        h.assertTrue(!wand.getTag().contains("CustomName"), "Static output resets source NBT");
        h.succeed();
    }

    @GameTest(template = "foundation_empty", batch = "crafting")
    public static void task_completion_and_menu_cannot_duplicate_output(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(0, 2, 0));
        level.setBlock(pos, CraftingContent.ALTAR.get().defaultBlockState(), 3);
        var altar = (AsterismAltarBlockEntity) level.getBlockEntity(pos);
        var player =
                net.minecraftforge.common.util.FakePlayerFactory.get(
                        h.getLevel(),
                        new com.mojang.authlib.GameProfile(UUID.randomUUID(), "PartThree"));
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        var recipeId = id("complete");
        CraftingBootstrap.hub(level.getServer()).add(recipeId, altarJson(3));
        altar.inventory().setStackInSlot(4, new ItemStack(Items.NETHER_STAR));
        h.assertTrue(altar.action(player, 0), "Start");
        altar.serverTick();
        h.assertTrue(
                altar.inventory().getStackInSlot(4).is(Items.NETHER_STAR), "No early consumption");
        altar.serverTick();
        altar.serverTick();
        h.assertTrue(
                altar.pendingOutput().is(Items.DIAMOND)
                        && altar.inventory().getStackInSlot(4).isEmpty(),
                "Finish atomically consumes and produces");
        var menu = new AsterismMenu(71, player.getInventory(), altar);
        player.containerMenu = menu;
        h.assertTrue(!menu.clickMenuButton(player, 99), "Forged action rejected");
        h.assertTrue(menu.clickMenuButton(player, 2), "First collect");
        h.assertTrue(!menu.clickMenuButton(player, 2), "Duplicate collect");
        h.assertTrue(player.getInventory().countItem(Items.DIAMOND) == 1, "One output only");
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        h.assertTrue(!menu.stillValid(player), "Replaced entity invalidates menu");
        CraftingBootstrap.hub(level.getServer()).remove(recipeId);
        h.succeed();
    }

    @GameTest(template = "foundation_empty", batch = "crafting")
    public static void altar_upgrade_keeps_entity_and_hidden_slots_are_inaccessible(
            GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(0, 3, 0));
        level.setBlock(pos, CraftingContent.ALTAR.get().defaultBlockState(), 3);
        var altar = (AsterismAltarBlockEntity) level.getBlockEntity(pos);
        var player =
                net.minecraftforge.common.util.FakePlayerFactory.get(
                        h.getLevel(),
                        new com.mojang.authlib.GameProfile(UUID.randomUUID(), "PartThree"));
        player.setPos(pos.getX(), pos.getY(), pos.getZ());
        var menu = new AsterismMenu(72, player.getInventory(), altar);
        h.assertTrue(menu.slots.size() == 45, "Discovery exposes only 9 + 36 slots");
        h.assertTrue(
                !((ItemStackHandler) altar.inventory()).isItemValid(24, new ItemStack(Items.DIRT)),
                "Automation rejects hidden slot");
        level.setBlock(
                pos,
                level.getBlockState(pos).setValue(CraftingContent.TIER, AsterismTier.RESONANCE),
                3);
        h.assertTrue(level.getBlockEntity(pos) == altar, "Upgrade must retain BE instance");
        h.assertTrue(altar.lumenCapacity() == 2000, "Capacity follows tier");
        h.assertTrue(!menu.stillValid(player), "Old tier menu must close");
        level.removeBlock(pos, false);
        h.succeed();
    }

    @GameTest(template = "foundation_empty", batch = "crafting")
    public static void input_index_priority_script_mutations_and_reset(GameTestHelper h) {
        var manager = new RecipeManager();
        var low = new AsterismRecipe(id("low"), altarJson(10));
        var highJson = altarJson(10);
        highJson.addProperty("tier", "sign");
        var high = new AsterismRecipe(id("high"), highJson);
        manager.replaceRecipes(List.of(low, high));
        var hub = new RecipeHub(manager, new RecipeBaseline(), RecipeCatalog::parse);
        var family = CraftingBootstrap.id("asterism_crafting");
        h.assertTrue(
                hub.byInput(family, List.of(new ItemStack(Items.NETHER_STAR))).get(0) == high,
                "Higher tier first");
        hub.scale(low.getId(), "duration", 2);
        h.assertTrue(
                ((AsterismRecipe) hub.byId(low.getId()).orElseThrow()).displayDuration() == 20,
                "Scale");
        hub.remove(high.getId());
        h.assertTrue(hub.byId(high.getId()).isEmpty(), "Remove");
        hub.disable(family);
        h.assertTrue(hub.all(family).isEmpty(), "Disable");
        hub.resetToBaseline();
        h.assertTrue(
                hub.all(family).size() == 2
                        && ((AsterismRecipe) hub.byId(low.getId()).orElseThrow()).displayDuration()
                                == 10,
                "Reset from datapack manager");
        h.succeed();
    }

    @GameTest(template = "foundation_empty", batch = "crafting")
    public static void thousand_recipe_hash_lookup_benchmark(GameTestHelper h) {
        var values =
                net.minecraftforge.registries.ForgeRegistries.ITEMS.getValues().stream()
                        .filter(i -> i != Items.AIR)
                        .limit(1000)
                        .toList();
        var recipes = new ArrayList<Recipe<?>>();
        for (int i = 0; i < values.size(); i++) {
            var data =
                    json(
                            "{\"type\":\"stellaeomphalos:grindwheel\",\"input\":{\"item\":\""
                                    + net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(
                                            values.get(i))
                                    + "\"},\"result\":{\"item\":\"minecraft:diamond\"}}");
            recipes.add(new GrindwheelRecipe(id("bench_" + i), data));
        }
        var index = new RecipeIndex(recipes);
        var query = List.of(new ItemStack(values.get(500)));
        for (int i = 0; i < 100; i++) index.byInput(query);
        long start = System.nanoTime();
        for (int i = 0; i < 1000; i++)
            h.assertTrue(index.byInput(query).size() == 1, "One candidate bucket");
        long average = (System.nanoTime() - start) / 1000;
        h.assertTrue(average < 1000000, "Lookup mean ns: " + average);
        com.mojang.logging.LogUtils.getLogger()
                .info("Part 3: 1000 recipe lookup average {} ns", average);
        h.succeed();
    }

    @GameTest(template = "foundation_empty", batch = "crafting")
    public static void infusion_resolves_chalices_each_tick_and_removes_replaced_handlers(
            GameTestHelper h) {
        var level = h.getLevel();
        var origin = h.absolutePos(new BlockPos(0, 4, 0));
        var pos = InfusionSolvent.positions(origin).get(0);
        level.setBlock(pos, CraftingContent.CHALICE.get().defaultBlockState(), 3);
        var chalice = (LumenChaliceBlockEntity) level.getBlockEntity(pos);
        chalice.tank().fill(new FluidStack(Fluids.WATER, 2000), IFluidHandler.FluidAction.EXECUTE);
        var recipe =
                new LumenInfusionRecipe(
                        id("water"),
                        json(
                                "{\"type\":\"stellaeomphalos:lumen_infusion\",\"input\":{\"item\":\"minecraft:diamond\"},\"duration\":200,\"solvent\":{\"fluid\":\"minecraft:water\",\"amount\":400,\"chance\":1},\"result\":{\"kind\":\"static\",\"stack\":{\"item\":\"minecraft:emerald\"}}}"));
        var supplies = InfusionSolvent.resolve(level, List.of(pos), recipe);
        h.assertTrue(
                supplies.size() == 1 && recipe.acceleration() == 0.3, "Resolve and speed factor");
        var plan = InfusionSolvent.plan(supplies, recipe, level.random).orElseThrow();
        h.assertTrue(
                InfusionSolvent.apply(plan) && chalice.tank().getFluidInTank(0).getAmount() == 1600,
                "Exact 400 mB");
        level.removeBlock(pos, false);
        h.assertTrue(
                InfusionSolvent.resolve(level, List.of(pos), recipe).isEmpty(),
                "Stale chalice removed");
        h.succeed();
    }

    @GameTest(template = "foundation_empty", batch = "crafting")
    public static void world_tables_exact_states_and_furnace_fallback(GameTestHelper h) {
        var recipe =
                new LightTransmutationRecipe(
                        id("exact"),
                        json(
                                "{\"type\":\"stellaeomphalos:light_transmutation\",\"input\":{\"block_state\":{\"Name\":\"minecraft:oak_log\",\"Properties\":{\"axis\":\"y\"}}},\"result\":{\"block\":\"minecraft:stone\"},\"cost\":100}"));
        var vertical = Blocks.OAK_LOG.defaultBlockState();
        h.assertTrue(recipe.matches(vertical, 100, Set.of()), "Exact state");
        h.assertTrue(
                !recipe.matches(
                        vertical.setValue(
                                net.minecraft.world.level.block.RotatedPillarBlock.AXIS,
                                Direction.Axis.X),
                        100,
                        Set.of()),
                "Wrong axis");
        h.assertTrue(!recipe.matches(vertical, 99, Set.of()), "Insufficient light");
        var index = WorldRecipeIndex.of(h.getLevel());
        var melt = index.melting(Blocks.SAND.defaultBlockState()).orElseThrow();
        var pos = h.absolutePos(new BlockPos(0, 5, 0));
        h.getLevel().setBlock(pos, Blocks.SAND.defaultBlockState(), 3);
        h.assertTrue(
                melt.apply(h.getLevel(), pos, Blocks.SAND.defaultBlockState()), "Furnace fallback");
        h.assertTrue(h.getLevel().getBlockState(pos).is(Blocks.GLASS), "Block output priority");
        h.assertTrue(
                !melt.apply(h.getLevel(), pos, Blocks.SAND.defaultBlockState()),
                "Stale source rejected");
        h.getLevel().removeBlock(pos, false);
        h.succeed();
    }

    @GameTest(template = "foundation_empty", batch = "crafting")
    public static void grinding_probabilities_alterations_and_fluid_rejection(GameTestHelper h) {
        var recipe =
                new GrindwheelRecipe(
                        id("grind"),
                        json(
                                "{\"input\":{\"item\":\"minecraft:stone\"},\"result\":{\"item\":\"minecraft:gravel\"},\"chance\":1,\"bonus_chance\":1}"));
        h.assertTrue(
                recipe.grind(new ItemStack(Items.STONE), h.getLevel().random).output().getCount()
                        == 2,
                "Guaranteed double");
        var hone =
                new GrindwheelRecipe(
                        id("hone"), json("{\"alteration\":\"stellaeomphalos:hone\",\"chance\":1}"));
        var sword = new ItemStack(Items.IRON_SWORD);
        h.assertTrue(
                hone.hidden() && hone.matches(new SimpleContainer(sword), h.getLevel()),
                "Pseudo recipe predicate");
        h.assertTrue(
                hone.grind(sword, h.getLevel().random).output().getTag().getBoolean("Honed"),
                "Hone NBT");
        boolean rejected = false;
        try {
            new GrindwheelRecipe(
                    id("bad"),
                    json(
                            "{\"input\":{\"fluid\":\"minecraft:water\",\"amount\":1000},\"result\":{\"item\":\"minecraft:diamond\"}}"));
        } catch (RuntimeException e) {
            rejected = true;
        }
        h.assertTrue(rejected, "Fluid grinding rejected");
        h.succeed();
    }

    @GameTest(template = "foundation_empty", batch = "crafting")
    public static void inventory_grid_cannot_use_light_proximity_recipe(GameTestHelper h) {
        var recipe =
                new LightProximityRecipe(
                        id("proximity"),
                        json(
                                "{\"pattern\":[\"Q\"],\"key\":{\"Q\":{\"item\":\"minecraft:quartz\"}},\"result\":{\"item\":\"minecraft:diamond\"},\"min_lumen\":0}"));
        var player =
                net.minecraftforge.common.util.FakePlayerFactory.get(
                        h.getLevel(),
                        new com.mojang.authlib.GameProfile(UUID.randomUUID(), "PartThree"));
        var menu = player.inventoryMenu;
        var grid = new net.minecraft.world.inventory.TransientCraftingContainer(menu, 2, 2);
        grid.setItem(0, new ItemStack(Items.QUARTZ));
        h.assertTrue(
                !recipe.matches(grid, h.getLevel()),
                "2x2 rejected even with zero light requirement");
        h.succeed();
    }

    @GameTest(template = "foundation_empty", batch = "crafting")
    public static void all_result_operators_copy_traits_and_keep_input_immutable(GameTestHelper h) {
        var source = new ItemStack(CraftingContent.ITEMS.get("raw_crystal").get());
        var traits = new CompoundTag();
        traits.putInt("Size", 240);
        traits.putInt("Purity", 70);
        traits.putInt("Collect", 50);
        source.getOrCreateTag().put("CrystalTraits", traits);
        source.getOrCreateTag().putInt("RevertCounter", 9);
        var spec =
                new ResultSpec(
                        json(
                                "{\"kind\":\"static\",\"stack\":{\"item\":\"stellaeomphalos:star_lens\"},\"ops\":[{\"op\":\"stellaeomphalos:preserve_nbt\"},{\"op\":\"stellaeomphalos:copy_crystal_traits\"},{\"op\":\"stellaeomphalos:set_tool_traits\"},{\"op\":\"stellaeomphalos:merge_crystal_traits\"},{\"op\":\"stellaeomphalos:scale_count_by_traits\",\"divisor\":80},{\"op\":\"stellaeomphalos:set_boolean\",\"key\":\"Upgraded\"},{\"op\":\"stellaeomphalos:unlock_upgrade\",\"upgrade\":\"stellaeomphalos:precision\"},{\"op\":\"stellaeomphalos:set_sign\",\"sign\":\"stellaeomphalos:aevitas\"},{\"op\":\"stellaeomphalos:set_trait_sign\",\"sign\":\"stellaeomphalos:gelu\"},{\"op\":\"stellaeomphalos:set_lens_color\",\"color\":42},{\"op\":\"stellaeomphalos:clear_revert_counter\"}]}"));
        var result = spec.assemble(slot -> slot == 4 ? source : ItemStack.EMPTY);
        var tag = result.getTag();
        h.assertTrue(result.getCount() == 3, "Trait count");
        h.assertTrue(tag.getCompound("CrystalTraits").equals(traits), "Crystal copy");
        h.assertTrue(
                tag.getCompound("ToolTraits").getInt("MaxCollect") >= 100, "Tool capacity floor");
        h.assertTrue(
                tag.getBoolean("Upgraded")
                        && tag.getInt("LensColor") == 42
                        && tag.getList("Upgrades", 8).size() == 1,
                "Mutation operators");
        h.assertTrue(
                tag.getString("TraitSignId").equals("stellaeomphalos:gelu")
                        && !tag.contains("RevertCounter"),
                "Trait sign and revert counter");
        h.assertTrue(
                source.getTag().getInt("RevertCounter") == 9
                        && !source.getTag().contains("ToolTraits"),
                "Input immutable");
        h.succeed();
    }

    @GameTest(template = "foundation_empty", batch = "crafting")
    public static void saved_altar_resumes_and_network_conversion_obeys_simulate(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(0, 6, 0));
        level.setBlock(pos, CraftingContent.ALTAR.get().defaultBlockState(), 3);
        var altar = (AsterismAltarBlockEntity) level.getBlockEntity(pos);
        var player =
                net.minecraftforge.common.util.FakePlayerFactory.get(
                        level, new com.mojang.authlib.GameProfile(UUID.randomUUID(), "PartThree"));
        player.setPos(pos.getX(), pos.getY(), pos.getZ());
        h.assertTrue(
                altar.acceptLumen(level, 1, true) == 1 && altar.lumenStored() == 0,
                "Network simulation is pure");
        h.assertTrue(
                altar.acceptLumen(level, 1, false) == 1 && altar.lumenStored() == 200,
                "200x network conversion");
        var recipeId = id("resume");
        CraftingBootstrap.hub(level.getServer()).add(recipeId, altarJson(10));
        altar.inventory().setStackInSlot(4, new ItemStack(Items.NETHER_STAR));
        h.assertTrue(altar.action(player, 0), "Start saved craft");
        altar.serverTick();
        altar.serverTick();
        var tag = altar.saveWithFullMetadata();
        altar.load(tag);
        h.assertTrue(altar.activeTask().orElseThrow().ticks() == 2, "Saved progress");
        var changed = altarJson(20);
        CraftingBootstrap.hub(level.getServer()).replace(recipeId, changed);
        altar.serverTick();
        h.assertTrue(
                altar.activeTask().orElseThrow().ticks() == 5,
                "Reload scales 2/10 to 4/20 then advances");
        CraftingBootstrap.hub(level.getServer()).remove(recipeId);
        altar.serverTick();
        h.assertTrue(
                altar.activeTask().orElseThrow().state() == CraftState.ORPHANED
                        && altar.inventory().getStackInSlot(4).is(Items.NETHER_STAR),
                "Orphan keeps input");
        level.removeBlock(pos, false);
        h.succeed();
    }

    @GameTest(template = "foundation_empty", batch = "crafting")
    public static void actual_upgrade_transaction_preserves_entity_identity(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(0, 7, 0));
        level.setBlock(pos, CraftingContent.ALTAR.get().defaultBlockState(), 3);
        var altar = (AsterismAltarBlockEntity) level.getBlockEntity(pos);
        var player =
                net.minecraftforge.common.util.FakePlayerFactory.get(
                        level, new com.mojang.authlib.GameProfile(UUID.randomUUID(), "PartThree"));
        player.setPos(pos.getX(), pos.getY(), pos.getZ());
        var data = altarJson(1);
        data.addProperty("type", "stellaeomphalos:asterism_upgrade");
        data.remove("tier");
        data.remove("result");
        data.addProperty("from_tier", "discovery");
        data.addProperty("to_tier", "resonance");
        data.add("flags", JsonParser.parseString("[\"night_only\",\"no_item_output\"]"));
        data.add("result_display", json("{\"item\":\"stellaeomphalos:asterism_altar_resonance\"}"));
        var recipeId = id("upgrade");
        CraftingBootstrap.hub(level.getServer()).add(recipeId, data);
        long previous = level.getDayTime();
        level.setDayTime(18000);
        level.updateSkyBrightness();
        altar.inventory().setStackInSlot(4, new ItemStack(Items.NETHER_STAR));
        h.assertTrue(altar.action(player, 0), "Upgrade started");
        altar.serverTick();
        h.assertTrue(
                level.getBlockEntity(pos) == altar && altar.tier() == AsterismTier.RESONANCE,
                "Upgrade keeps BE");
        h.assertTrue(
                altar.inventory().getStackInSlot(4).isEmpty() && altar.pendingOutput().isEmpty(),
                "Upgrade consumes without duplicate block item");
        level.setDayTime(previous);
        level.updateSkyBrightness();
        CraftingBootstrap.hub(level.getServer()).remove(recipeId);
        level.removeBlock(pos, false);
        h.succeed();
    }

    @GameTest(template = "foundation_empty", batch = "crafting")
    public static void three_documented_mutation_examples_execute_and_reset(GameTestHelper h)
            throws Exception {
        var hub =
                new RecipeHub(
                        h.getLevel().getRecipeManager(),
                        new RecipeBaseline(),
                        RecipeCatalog::parse);
        for (String name : List.of("01-add", "02-replace-scale", "03-remove-disable-reset")) {
            var file =
                    java.nio.file.Path.of(
                            System.getProperty("stellaeomphalos.projectRoot", ".."),
                            "docs/examples/part3/" + name + ".json");
            var data =
                    JsonParser.parseString(java.nio.file.Files.readString(file)).getAsJsonObject();
            for (var operation : data.getAsJsonArray("operations"))
                RecipeScriptBridge.apply(hub, operation.getAsJsonObject());
        }
        h.assertTrue(
                hub.byId(new ResourceLocation("example", "quartz_grinding")).isPresent(),
                "Add example");
        h.assertTrue(
                ((AsterismRecipe)
                                        hub.byId(CraftingBootstrap.id("altar/star_sextant"))
                                                .orElseThrow())
                                .displayDuration()
                        == 50,
                "Scale example");
        h.assertTrue(hub.byId(CraftingBootstrap.id("grinding/gravel")).isEmpty(), "Remove example");
        h.assertTrue(
                !hub.all(CraftingBootstrap.id("fluid_interaction")).isEmpty(),
                "Family reset example");
        hub.resetToBaseline();
        h.assertTrue(
                hub.byId(new ResourceLocation("example", "quartz_grinding")).isEmpty()
                        && hub.byId(CraftingBootstrap.id("grinding/gravel")).isPresent(),
                "Baseline reset");
        h.succeed();
    }

    @GameTest(template = "foundation_empty", batch = "crafting")
    public static void partial_fluid_container_drain_keeps_the_remainder(GameTestHelper h) {
        var stack = new ItemStack(CraftingContent.ITEMS.get("lumen_flask").get());
        var cap = net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER_ITEM;
        var handler =
                stack.getCapability(cap)
                        .orElseThrow(() -> new AssertionError("Probe fluid capability"));
        handler.fill(new FluidStack(Fluids.WATER, 1800), IFluidHandler.FluidAction.EXECUTE);
        var data = new JsonObject();
        data.addProperty("fluid", "minecraft:water");
        data.addProperty("amount", 400);
        var material = MaterialSpec.parse(data);
        h.assertTrue(material.test(stack), "Tank matches exact fluid demand");
        var drained = material.consumeOne(stack);
        h.assertTrue(
                handler.getFluidInTank(0).getAmount() == 1800,
                "Simulation must not drain original");
        h.assertTrue(
                drained.getCapability(cap)
                                .orElseThrow(() -> new AssertionError("Remainder capability"))
                                .getFluidInTank(0)
                                .getAmount()
                        == 1400,
                "Keep precise 1400 mB remainder");
        data.addProperty("amount", 1801);
        h.assertTrue(!MaterialSpec.parse(data).test(stack), "Insufficient fluid rejects");
        h.succeed();
    }

    @GameTest(template = "foundation_empty", batch = "crafting")
    public static void optional_crafttweaker_samples_are_loaded_when_present(GameTestHelper h) {
        if (!net.minecraftforge.fml.ModList.get().isLoaded("crafttweaker")) {
            h.succeed();
            return;
        }
        var manager = h.getLevel().getRecipeManager();
        for (String name : List.of("part3_altar", "part3_infusion", "part3_grinding"))
            h.assertTrue(
                    manager.byKey(new ResourceLocation("crafttweaker", name)).isPresent(),
                    "CraftTweaker script recipe missing: " + name);
        h.assertTrue(
                manager.byKey(new ResourceLocation("crafttweaker", "part3_temporary")).isEmpty(),
                "CraftTweaker removeByName sample");
        h.succeed();
    }
}

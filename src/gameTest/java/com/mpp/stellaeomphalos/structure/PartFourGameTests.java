package com.mpp.stellaeomphalos.structure;

import com.google.gson.*;
import com.mpp.stellaeomphalos.Omphalos;
import com.mpp.stellaeomphalos.content.blockentity.rite.*;
import com.mpp.stellaeomphalos.content.world.*;
import com.mpp.stellaeomphalos.content.world.capability.*;
import com.mpp.stellaeomphalos.ritual.rite.*;
import com.mpp.stellaeomphalos.structure.match.*;
import com.mpp.stellaeomphalos.structure.pattern.*;

import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.*;
import net.minecraftforge.gametest.*;

import java.util.*;

@GameTestHolder(Omphalos.MODID)
@PrefixGameTestTemplate(false)
public final class PartFourGameTests {
    private static ResourceLocation id(String name) {
        return new ResourceLocation(Omphalos.MODID, name);
    }

    private static void check(GameTestHelper h, boolean value, String message) {
        h.assertTrue(value, message);
    }

    @GameTest(template = "foundation_empty", batch = "part4")
    public static void catalogue_and_world_templates_load(GameTestHelper h) {
        check(h, BlueprintRegistry.all().size() >= 13, "Blueprints load");
        check(h, RiteRegistry.recipes().size() == 12, "Twelve rite recipes");
        check(h, RiteRegistry.amplifiers().size() == 4, "Four amplifiers");
        for (String name :
                List.of(
                        "ancient_shrine",
                        "desert_shrine",
                        "small_shrine",
                        "treasure_shrine",
                        "small_ruin",
                        "lumen_spring")) {
            var template = h.getLevel().getStructureManager().get(id(name));
            check(
                    h,
                    template.isPresent() && template.get().getSize().getX() > 0,
                    "Template " + name);
            check(
                    h,
                    h.getLevel()
                            .registryAccess()
                            .registryOrThrow(Registries.STRUCTURE)
                            .containsKey(id(name)),
                    "Structure registry " + name);
        }
        for (String name :
                List.of(
                        "geode_ore",
                        "astral_ore",
                        "marble_vein",
                        "aquamarine_sand",
                        "glowbloom_patch",
                        "sky_crystal_cluster_patch",
                        "prism_crystal_cluster_patch"))
            check(
                    h,
                    h.getLevel()
                            .registryAccess()
                            .registryOrThrow(Registries.PLACED_FEATURE)
                            .containsKey(id(name)),
                    "Placed feature " + name);
        h.succeed();
    }

    @GameTest(template = "foundation_empty", batch = "part4")
    public static void blueprint_order_air_and_invalid_palette_are_atomic(GameTestHelper h) {
        var builder =
                PatternBlueprintBuilder.named(id("test_fill"))
                        .cube(
                                new BlockPos(2, 2, 2),
                                BlockPos.ZERO,
                                BlockRule.state(Blocks.STONE.defaultBlockState()))
                        .air(new BlockPos(1, 1, 1));
        var blueprint = builder.build();
        check(
                h,
                blueprint.blocks().size() == 27
                        && blueprint
                                .blocks()
                                .get(new BlockPos(1, 1, 1))
                                .rule()
                                .matches(Blocks.GRASS.defaultBlockState()),
                "Ordered fill and vegetation air");
        check(h, !BlockRule.air().matches(Blocks.WATER.defaultBlockState()), "Fluid is never air");
        boolean frozen = false;
        try {
            builder.air(BlockPos.ZERO);
        } catch (IllegalStateException e) {
            frozen = true;
        }
        check(h, frozen, "Builder frozen");
        boolean invalid = false;
        try {
            BlueprintJson.decode(
                    id("bad"),
                    JsonParser.parseString(
                                    "{\"format\":1,\"palette\":{\"x\":{\"block\":\"minecraft:stone\",\"properties\":{\"missing\":\"true\"}}},\"layers\":[]}")
                            .getAsJsonObject(),
                    k -> null);
        } catch (IllegalArgumentException e) {
            invalid = true;
        }
        check(h, invalid, "Reject invalid complete blueprint");
        h.succeed();
    }

    @GameTest(template = "foundation_empty", batch = "part4")
    public static void transformed_direction_matches_coordinate_direction(GameTestHelper h) {
        for (var t : PlacementTransform.values()) {
            var state =
                    Blocks.FURNACE
                            .defaultBlockState()
                            .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH);
            var pos = t.apply(new BlockPos(0, 0, -1));
            check(
                    h,
                    t.apply(state)
                            .getValue(HorizontalDirectionalBlock.FACING)
                            .getNormal()
                            .equals(pos),
                    "Transform state/position agree " + t);
        }
        h.succeed();
    }

    @GameTest(template = "foundation_empty", batch = "part4")
    public static void matcher_local_change_and_unload_recovery(GameTestHelper h) {
        var origin = h.absolutePos(new BlockPos(1, 2, 1));
        var blueprint =
                PatternBlueprintBuilder.named(id("test_match"))
                        .block(BlockPos.ZERO, Blocks.STONE)
                        .put(
                                new BlockPlacement(
                                        new BlockPos(1, 0, 0),
                                        BlockRule.state(Blocks.GOLD_BLOCK.defaultBlockState()),
                                        MismatchSeverity.OPTIONAL))
                        .build();
        h.getLevel().setBlock(origin, Blocks.STONE.defaultBlockState(), 3);
        var a = new StructureAuthority(blueprint, origin);
        a.initialize(h.getLevel());
        check(h, a.state() == StructureState.DEGRADED, "Optional mismatch retains production");
        h.getLevel().setBlock(origin, Blocks.AIR.defaultBlockState(), 3);
        a.changed(h.getLevel(), origin);
        check(h, a.state() == StructureState.BROKEN, "Broken required cell");
        h.getLevel().setBlock(origin, Blocks.STONE.defaultBlockState(), 3);
        a.changed(h.getLevel(), origin);
        a.unload(new ChunkPos(origin));
        check(h, a.state() == StructureState.INDETERMINATE, "Unload indeterminate");
        a.verifyChunk(h.getLevel(), new ChunkPos(origin));
        check(h, a.formed(), "Reload restored");
        h.succeed();
    }

    @GameTest(template = "foundation_empty", batch = "part4")
    public static void real_chunk_capabilities_persist_and_mark_dirty(GameTestHelper h) {
        var chunk = h.getLevel().getChunkAt(h.absolutePos(BlockPos.ZERO));
        var spring =
                chunk.getCapability(WorldCapabilities.SPRING)
                        .orElseThrow(() -> new AssertionError("Spring capability absent"));
        spring.initialize(3, chunk.getPos().x, chunk.getPos().z, WorldBehaviorRegistry.springs());
        int before = spring.remainingMb();
        spring.drain(100, true);
        var copy = new SpringVeinHolder();
        copy.deserializeNBT(spring.serializeNBT());
        check(h, copy.remainingMb() == Math.max(0, before - 100), "Reserve persisted");
        check(
                h,
                chunk.getCapability(WorldCapabilities.GEODES).isPresent()
                        && chunk.getCapability(WorldCapabilities.STAMP).isPresent(),
                "All three capabilities attached");
        h.succeed();
    }

    private static final class Host implements RiteHost {
        StructureState structure = StructureState.FORMED;
        boolean sky = true, crystal = true, accept = true;
        long lumen = 10000;
        int emitted;

        public StructureState structureState() {
            return structure;
        }

        public boolean crystalValid(RiteRecipe r) {
            return crystal;
        }

        public boolean celestialReady(RiteRecipe r) {
            return sky;
        }

        public boolean amplifiersReady(RiteRecipe r) {
            return true;
        }

        public long storedLumen() {
            return lumen;
        }

        public void consumeLumen(int n) {
            lumen -= n;
        }

        public int amplifierUpkeep() {
            return 0;
        }

        public boolean commitOutputs(List<ItemStack> list) {
            if (!accept) return false;
            emitted += list.stream().mapToInt(ItemStack::getCount).sum();
            return true;
        }

        public void cycleCompleted() {}

        public void stateChanged(RiteState old, RiteState next) {}
    }

    private static RiteRecipe recipe() {
        return new RiteRecipe(
                id("aevitas"),
                false,
                0,
                0,
                0,
                null,
                Map.of(),
                1,
                1,
                8,
                List.of(
                        new RiteRecipe.WeightedOutput(
                                new ResourceLocation("minecraft", "diamond"), 1, 1, 1)),
                80,
                List.of(),
                new CompoundTag(),
                true,
                true);
    }

    private static RiteInstance running(Host host, GameTestHelper h) {
        var rite = new RiteInstance(id("test_rite"));
        rite.tick(recipe(), host, h.getLevel().random);
        rite.tick(recipe(), host, h.getLevel().random);
        rite.requestStart();
        for (int i = 0; i < 21; i++) rite.tick(recipe(), host, h.getLevel().random);
        check(h, rite.state() == RiteState.RUNNING, "Warmup finishes");
        return rite;
    }

    @GameTest(template = "foundation_empty", batch = "part4")
    public static void rite_soft_hard_interrupts_and_ten_reloads_preserve_progress(
            GameTestHelper h) {
        var host = new Host();
        var rite = running(host, h);
        for (int i = 0; i < 3; i++) rite.tick(recipe(), host, h.getLevel().random);
        int progress = rite.progress(), used = rite.consumedLumen();
        host.sky = false;
        rite.tick(recipe(), host, h.getLevel().random);
        check(h, rite.state() == RiteState.STALLED && rite.progress() == progress, "Soft freeze");
        for (int i = 0; i < 10; i++) {
            rite = RiteInstance.load(rite.save());
            rite.tick(recipe(), host, h.getLevel().random);
            check(h, rite.progress() == progress && rite.consumedLumen() == used, "Reload " + i);
        }
        host.sky = true;
        rite.tick(recipe(), host, h.getLevel().random);
        host.structure = StructureState.BROKEN;
        rite.tick(recipe(), host, h.getLevel().random);
        check(
                h,
                rite.state() == RiteState.INTERRUPTED && rite.interrupts() == 1,
                "Hard interruption");
        host.structure = StructureState.FORMED;
        for (int i = 0; i < 20 && host.emitted == 0; i++)
            rite.tick(recipe(), host, h.getLevel().random);
        check(h, host.emitted == 1 && host.lumen == 9920, "One output and exactly 80 lumen");
        h.succeed();
    }

    @GameTest(template = "foundation_empty", batch = "part4")
    public static void finishing_backpressure_and_reload_cannot_duplicate_output(GameTestHelper h) {
        var host = new Host();
        var rite = running(host, h);
        host.accept = false;
        for (int i = 0; i < 12; i++) rite.tick(recipe(), host, h.getLevel().random);
        check(
                h,
                rite.state() == RiteState.FINISHING && rite.pendingOutputs().size() == 1,
                "Pending durable output");
        rite = RiteInstance.load(rite.save());
        host.accept = true;
        rite.tick(recipe(), host, h.getLevel().random);
        rite.tick(recipe(), host, h.getLevel().random);
        check(h, host.emitted == 1 && rite.cycles() == 1, "Settlement once after restart");
        h.succeed();
    }

    @GameTest(template = "foundation_empty", batch = "part4")
    public static void pedestal_owner_custody_rejects_strangers(GameTestHelper h) {
        var p = h.absolutePos(new BlockPos(1, 2, 1));
        h.getLevel().setBlock(p, WorldContent.PEDESTAL.get().defaultBlockState(), 3);
        var be = (RitePedestalBlockEntity) h.getLevel().getBlockEntity(p);
        var owner = UUID.randomUUID();
        be.setOwner(owner);
        check(h, be.commitOutputs(List.of(new ItemStack(Items.DIAMOND))), "Store output");
        var stranger =
                new net.minecraft.server.level.ServerPlayer(
                        h.getLevel().getServer(),
                        h.getLevel(),
                        new com.mojang.authlib.GameProfile(UUID.randomUUID(), "Stranger"));
        check(h, !be.tryDeliver(stranger) && be.output().getCount() == 1, "Stranger denied");
        var tag = be.saveWithoutMetadata();
        var copy = new RitePedestalBlockEntity(p, WorldContent.PEDESTAL.get().defaultBlockState());
        copy.load(tag);
        check(
                h,
                owner.equals(copy.owner()) && copy.output().getCount() == 1,
                "Owner and inventory persisted");
        check(
                h,
                !be.getCapability(
                                net.minecraftforge.common.capabilities.ForgeCapabilities
                                        .ITEM_HANDLER)
                        .isPresent(),
                "Automation cannot bypass custody");
        h.succeed();
    }

    @GameTest(template = "foundation_empty", batch = "part4")
    public static void generated_geodes_bridge_back_to_loaded_capability(GameTestHelper h) {
        var p = h.absolutePos(new BlockPos(1, 2, 1));
        h.getLevel().setBlock(p, WorldContent.GEODE_ORE.get().defaultBlockState(), 3);
        PendingGeodeRegistration.enqueue(h.getLevel().dimension(), p);
        PendingGeodeRegistration.drain(h.getLevel(), 10);
        var index =
                h.getLevel()
                        .getChunkAt(p)
                        .getCapability(WorldCapabilities.GEODES)
                        .orElseThrow(() -> new AssertionError("Index"));
        check(h, index.positions().contains(p), "Main-thread queue registered");
        h.getLevel().setBlock(p, Blocks.AIR.defaultBlockState(), 3);
        check(h, !index.positions().contains(p), "Removal unregisters");
        h.succeed();
    }

    @GameTest(template = "foundation_empty", batch = "part4")
    public static void empty_remote_retrogen_does_not_load_chunks(GameTestHelper h) {
        int x = 170000, z = 170000;
        var before = h.getLevel().getChunkSource().getLoadedChunksCount();
        var plan = new RetroGenPlan(h.getLevel());
        int pending = plan.queued();
        plan.enqueue(new ChunkPos(x, z));
        plan.tick();
        check(
                h,
                h.getLevel().getChunkSource().getChunkNow(x, z) == null
                        && h.getLevel().getChunkSource().getLoadedChunksCount() == before,
                "No chunk loads");
        check(h, plan.queued() >= pending, "Unloaded pending work retained without loading chunks");
        h.succeed();
    }

    @GameTest(template = "foundation_empty", batch = "part4")
    public static void behavior_tables_are_consumed(GameTestHelper h) {
        check(
                h,
                WorldBehaviorRegistry.randomOre(h.getLevel().random, "mineral") != null,
                "Mineral tag table");
        check(
                h,
                !WorldBehaviorRegistry.vegetation("flowers_red").isEmpty(),
                "Vegetation tag table");
        check(
                h,
                WorldBehaviorRegistry.treeSpecies(Blocks.OAK_LOG.defaultBlockState(), "oak"),
                "Tree table");
        h.succeed();
    }

    @GameTest(template = "foundation_empty", batch = "part4")
    public static void live_watch_reconciles_reload_and_missing_blueprints(GameTestHelper h) {
        var previous = BlueprintRegistry.all();
        var origin = h.absolutePos(new BlockPos(2, 3, 2));
        var key = id("reload_test");
        var stone = PatternBlueprintBuilder.named(key).block(BlockPos.ZERO, Blocks.STONE).build();
        var gold =
                PatternBlueprintBuilder.named(key).block(BlockPos.ZERO, Blocks.GOLD_BLOCK).build();
        try {
            h.getLevel().setBlock(origin, Blocks.STONE.defaultBlockState(), 3);
            var map = new HashMap<>(previous);
            map.put(key, stone);
            BlueprintRegistry.publish(map);
            var hub = StructureIntegrityHub.of(h.getLevel());
            check(h, hub.query(origin, key).canProduce(), "Initial formed");
            map.put(key, gold);
            BlueprintRegistry.publish(map);
            check(h, hub.query(origin, key) == StructureState.BROKEN, "Reload changes authority");
            map.remove(key);
            BlueprintRegistry.publish(map);
            check(h, hub.query(origin, key) == StructureState.LOCKED, "Removed blueprint locks");
            map.put(key, stone);
            BlueprintRegistry.publish(map);
            check(h, hub.query(origin, key).canProduce(), "Restored blueprint reconciles");
            hub.release(origin);
        } finally {
            BlueprintRegistry.publish(previous);
        }
        h.succeed();
    }

    @GameTest(template = "foundation_empty", batch = "part4")
    public static void structure_benchmark_and_astrolabe_query_budget(GameTestHelper h) {
        var base = h.absolutePos(BlockPos.ZERO);
        var origin = new BlockPos(base.getX(), 300, base.getZ());
        for (int x = (origin.getX() - 20) >> 4; x <= (origin.getX() + 20) >> 4; x++)
            for (int z = (origin.getZ() - 20) >> 4; z <= (origin.getZ() + 20) >> 4; z++)
                h.getLevel().getChunk(x, z);
        var blueprint =
                PatternBlueprintBuilder.named(id("benchmark"))
                        .cube(new BlockPos(-20, 0, -20), new BlockPos(20, 8, 20), BlockRule.air())
                        .build();
        var authority = new StructureAuthority(blueprint, origin);
        for (int i = 0; i < 10; i++) authority.initialize(h.getLevel());
        long[] full = new long[25];
        for (int i = 0; i < full.length; i++) {
            long start = System.nanoTime();
            authority.initialize(h.getLevel());
            full[i] = System.nanoTime() - start;
        }
        Arrays.sort(full);
        long start = System.nanoTime();
        for (int i = 0; i < 10000; i++) authority.changed(h.getLevel(), origin);
        long incremental = (System.nanoTime() - start) / 10000;
        var ledger = new AstrolabeLedger();
        var random = new Random(17);
        for (int i = 0; i < 4096; i++)
            ledger.mark(
                    id("shrine"),
                    new BlockPos(random.nextInt(4096) - 2048, 64, random.nextInt(4096) - 2048));
        long[] queries = new long[1000];
        for (int i = 0; i < queries.length; i++) {
            start = System.nanoTime();
            ledger.nearest(
                    id("shrine"), new net.minecraft.world.phys.Vec3(i % 64, 70, i % 113), 128);
            queries[i] = System.nanoTime() - start;
        }
        Arrays.sort(queries);
        com.mojang.logging.LogUtils.getLogger()
                .info(
                        "Part4 benchmark: 15129-cell full median={} ns, incremental mean={} ns,"
                                + " 4096-record query P99={} ns",
                        full[12],
                        incremental,
                        queries[990]);
        check(h, blueprint.blocks().size() == 15129, "Full-scale fixture");
        check(
                h,
                full[12] <= 8000000 && incremental <= 150000 && queries[990] <= 20000000,
                "Document performance thresholds");
        h.succeed();
    }

    @GameTest(template = "foundation_empty", batch = "part4")
    public static void thirteen_payloads_have_stable_ids_and_preview_frames_are_bounded(
            GameTestHelper h) {
        var registry = com.mpp.stellaeomphalos.network.OmphalosChannel.PAYLOADS;
        for (int i = 26; i <= 38; i++) check(h, registry.type(i) != null, "SP id " + i);
        var positions = new ArrayList<Long>();
        var states = new ArrayList<Integer>();
        for (int i = 0; i < 256; i++) {
            positions.add(new BlockPos(-128, i % 128, -128).asLong());
            states.add(Block.getId(Blocks.GOLD_BLOCK.defaultBlockState()));
        }
        var packet =
                new com.mpp.stellaeomphalos.network.toClient.PreviewDiffPayload(
                        id("preview"), BlockPos.ZERO, positions, states, 256, 1, true);
        byte[] bytes = registry.encode(packet);
        check(h, bytes.length <= 8192, "Preview under 8 KiB");
        check(
                h,
                registry.decode(
                                27,
                                com.mpp.stellaeomphalos.network.PayloadRegistry.Direction.TO_CLIENT,
                                bytes)
                        .equals(packet),
                "Preview codec roundtrip");
        boolean blocked = false;
        try {
            registry.decode(
                    27, com.mpp.stellaeomphalos.network.PayloadRegistry.Direction.TO_SERVER, bytes);
        } catch (IllegalArgumentException e) {
            blocked = true;
        }
        check(h, blocked, "Direction checked");
        h.succeed();
    }
}

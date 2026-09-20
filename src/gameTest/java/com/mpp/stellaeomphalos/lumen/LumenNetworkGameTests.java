package com.mpp.stellaeomphalos.lumen;

import com.mpp.stellaeomphalos.Omphalos;
import com.mpp.stellaeomphalos.content.blockentity.lumen.LumenContent;
import com.mpp.stellaeomphalos.lumen.transport.LumenNetworks;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Omphalos.MODID)
@PrefixGameTestTemplate(false)
public final class LumenNetworkGameTests {
    private record Chain(BlockPos collector, BlockPos relay, BlockPos battery) {}

    /**
     * Places battery/relay/collector as a vertical stack on a fixed per-test Y band. The shared
     * GameTest dimension persists between runs, so heightmap-relative placement would stack on top
     * of leftovers from previous runs and eventually exceed the build height; fixed bands plus a
     * pre-cleared column keep every run identical. yLift keeps every test's chain more than one
     * link range (16) away from the other tests' chains in the shared dimension network.
     */
    private static final int BASE_Y = 150;

    private static Chain placeChain(GameTestHelper helper, int yLift) {
        var level = helper.getLevel();
        var base = helper.absolutePos(new BlockPos(0, 1, 0));
        int y = BASE_Y + yLift * 3;
        var chain =
                new Chain(
                        new BlockPos(base.getX(), y + 2, base.getZ()),
                        new BlockPos(base.getX(), y + 1, base.getZ()),
                        new BlockPos(base.getX(), y, base.getZ()));
        var air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++)
                for (int dy = -12; dy <= 12; dy++)
                    level.setBlock(chain.relay().offset(dx, dy, dz), air, 3);
        level.setBlock(chain.battery(), LumenContent.BATTERY_BLOCK.get().defaultBlockState(), 3);
        level.setBlock(chain.relay(), LumenContent.RELAY_BLOCK.get().defaultBlockState(), 3);
        level.setBlock(
                chain.collector(), LumenContent.COLLECTOR_BLOCK.get().defaultBlockState(), 3);
        helper.assertTrue(
                level.canSeeSky(chain.collector().above()), "Collector has no sky access");
        return chain;
    }

    private static void clearChain(GameTestHelper helper, Chain chain) {
        var level = helper.getLevel();
        var air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        level.setBlock(chain.collector(), air, 3);
        level.setBlock(chain.relay(), air, 3);
        level.setBlock(chain.battery(), air, 3);
    }

    private static LumenContent.Battery battery(GameTestHelper helper, Chain chain) {
        var entity = helper.getLevel().getBlockEntity(chain.battery());
        helper.assertTrue(entity instanceof LumenContent.Battery, "Battery block entity missing");
        return (LumenContent.Battery) entity;
    }

    @GameTest(template = "foundation_empty")
    public static void collector_chain_converges_and_charges(GameTestHelper helper) {
        var level = helper.getLevel();
        var chain = placeChain(helper, 1);
        helper.runAfterDelay(
                2,
                () -> {
                    var network = LumenNetworks.of(level);
                    helper.assertTrue(
                            network.connectionsOf(chain.collector()).contains(chain.relay()),
                            "Topology did not converge within 2 ticks");
                    helper.assertTrue(
                            network.connectionsOf(chain.relay()).contains(chain.battery()),
                            "Relay not linked to battery");
                });
        helper.succeedWhen(
                () -> {
                    helper.assertTrue(
                            battery(helper, chain).lumenStored() > 0,
                            "Battery did not start charging");
                    clearChain(helper, chain);
                });
    }

    @GameTest(template = "foundation_empty")
    public static void relay_removal_breaks_the_chain(GameTestHelper helper) {
        var level = helper.getLevel();
        var chain = placeChain(helper, 22);
        helper.runAfterDelay(
                10,
                () -> {
                    var battery = battery(helper, chain);
                    helper.assertTrue(battery.lumenStored() > 0, "Chain never charged");
                    level.removeBlock(chain.relay(), false);
                    helper.runAfterDelay(
                            2,
                            () -> {
                                var network = LumenNetworks.of(level);
                                helper.assertTrue(
                                        network.ioAt(chain.relay()).isEmpty(),
                                        "Relay node still present 2 ticks after removal");
                                helper.assertTrue(
                                        !network.connectionsOf(chain.collector())
                                                .contains(chain.relay()),
                                        "Relay link survived removal");
                                long frozen = battery.lumenStored();
                                helper.runAfterDelay(
                                        3,
                                        () -> {
                                            helper.assertTrue(
                                                    battery.lumenStored() == frozen,
                                                    "Link stayed live after relay removal");
                                            clearChain(helper, chain);
                                            helper.succeed();
                                        });
                            });
                });
    }

    @GameTest(template = "foundation_empty")
    public static void chunk_suspend_and_resume_heals_without_ghosts(GameTestHelper helper) {
        var chain = placeChain(helper, 43);
        var network = LumenNetworks.of(helper.getLevel());
        helper.runAfterDelay(
                10,
                () -> {
                    var battery = battery(helper, chain);
                    helper.assertTrue(battery.lumenStored() > 0, "Chain never charged");
                    long chunkKey =
                            ChunkPos.asLong(
                                    chain.collector().getX() >> 4, chain.collector().getZ() >> 4);
                    var ownedNodes =
                            java.util.Set.of(chain.collector(), chain.relay(), chain.battery());
                    helper.assertTrue(
                            network.entriesInChunk(chunkKey).containsAll(ownedNodes),
                            "Chain not registered");
                    network.suspendChunk(chunkKey);
                    long[] frozen = {battery.lumenStored()};
                    helper.runAfterDelay(
                            3,
                            () -> {
                                helper.assertTrue(
                                        battery.lumenStored() == frozen[0],
                                        "Suspended chunk kept delivering");
                                helper.assertTrue(
                                        network.entriesInChunk(chunkKey).stream()
                                                        .filter(ownedNodes::contains)
                                                        .count()
                                                == ownedNodes.size(),
                                        "Suspend dropped nodes");
                                network.resumeChunk(chunkKey);
                                helper.runAfterDelay(
                                        3,
                                        () -> {
                                            helper.assertTrue(
                                                    network.entriesInChunk(chunkKey).stream()
                                                                    .filter(ownedNodes::contains)
                                                                    .count()
                                                            == ownedNodes.size(),
                                                    "Ghost node appeared after resume");
                                            helper.assertTrue(
                                                    battery.lumenStored() > frozen[0],
                                                    "Topology did not heal after resume");
                                            clearChain(helper, chain);
                                            helper.succeed();
                                        });
                            });
                });
    }
}

package com.mpp.stellaeomphalos.constellation.domain;

import com.mpp.stellaeomphalos.Omphalos;
import com.mpp.stellaeomphalos.constellation.effect.DomainEffectChronos;
import com.mpp.stellaeomphalos.constellation.effect.DomainEffectLumina;
import com.mpp.stellaeomphalos.constellation.effect.DomainEffectSoar;
import com.mpp.stellaeomphalos.constellation.effect.DomainEffectVerdance;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Omphalos.MODID)
@PrefixGameTestTemplate(false)
public final class DomainGameTests {
    private static DomainContext ctx(GameTestHelper helper, BlockPos origin) {
        return new DomainContext(helper.getLevel(), helper.absolutePos(origin), null, 0);
    }

    private static DomainProperties props(boolean corrupted) {
        return new DomainProperties(6.0, 1.0, 1.0, corrupted, 0.0, 1.0);
    }

    /** Verdance: normal branch bonemeals the crop; the corrupted branch destroys it. */
    @GameTest(template = "foundation_empty", timeoutTicks = 400)
    public static void verdance_grows_then_corrupted_destroys_crop(GameTestHelper helper) {
        var level = helper.getLevel();
        var effect = new DomainEffectVerdance(null);
        var origin = new BlockPos(1, 2, 1);
        var crop = new BlockPos(2, 2, 2);
        helper.setBlock(crop, Blocks.WHEAT);
        var context = ctx(helper, origin);
        effect.focus(context);
        helper.assertTrue(effect.offer(level, helper.absolutePos(crop)), "Crop position refused by the cache");

        int ageBefore = level.getBlockState(helper.absolutePos(crop)).getValue(CropBlock.AGE);
        for (int i = 0; i < 80; i++) effect.play(context, 1.0F, props(false));
        int ageAfter = level.getBlockState(helper.absolutePos(crop)).getValue(CropBlock.AGE);
        helper.assertTrue(ageAfter > ageBefore, "Verdance never grew the crop: " + ageBefore + " -> " + ageAfter);

        boolean destroyed = false;
        for (int i = 0; i < 120 && !destroyed; i++) {
            effect.play(context, 1.0F, props(true));
            destroyed = level.getBlockState(helper.absolutePos(crop)).isAir();
        }
        helper.assertTrue(destroyed, "Corrupted Verdance kept the crop");
        helper.succeed();
    }

    /** Chronos: a whitelisted furnace is ticked 5-7 extra times per play; CookTime advances far past vanilla speed. */
    @GameTest(template = "foundation_empty", timeoutTicks = 400)
    public static void chronos_accelerates_furnace_ticks(GameTestHelper helper) {
        var level = helper.getLevel();
        var effect = new DomainEffectChronos(null);
        var furnaceRel = new BlockPos(2, 2, 2);
        helper.setBlock(furnaceRel, Blocks.FURNACE);
        var furnacePos = helper.absolutePos(furnaceRel);
        var furnace = (AbstractFurnaceBlockEntity) level.getBlockEntity(furnacePos);
        furnace.setItem(0, new ItemStack(Items.RAW_IRON, 16));
        furnace.setItem(1, new ItemStack(Items.COAL, 16));
        var context = ctx(helper, new BlockPos(1, 2, 1));
        effect.focus(context);
        helper.assertTrue(effect.offer(level, furnacePos), "Furnace refused by the chronos cache");

        // Vanilla baseline: the world ticks the furnace on its own during runAfterDelay.
        helper.runAfterDelay(10, () -> {
            int baseline = furnace.saveWithoutMetadata().getInt("CookTime");
            for (int i = 0; i < 60; i++) effect.play(context, 1.0F, props(false));
            int accelerated = furnace.saveWithoutMetadata().getInt("CookTime");
            helper.assertTrue(accelerated > baseline + 60,
                    "Chronos did not accelerate the furnace: " + baseline + " -> " + accelerated);
            helper.succeed();
        });
    }

    /** Lumina: the status branch renews a spawn-deny token covering the whole domain radius. */
    @GameTest(template = "foundation_empty", timeoutTicks = 400)
    public static void lumina_renews_spawn_deny_token(GameTestHelper helper) {
        var level = helper.getLevel();
        var effect = new DomainEffectLumina(null);
        var context = ctx(helper, new BlockPos(1, 2, 1));
        helper.assertTrue(effect.isActive(context), "Lumina status channel inactive while enabled");
        // Renew happens every 80 ticks; poll until the token lands.
        helper.runAfterDelay(1, () -> pollToken(helper, effect, context, 0));
    }

    private static void pollToken(GameTestHelper helper, DomainEffectLumina effect, DomainContext context, int attempts) {
        effect.play(context, 1.0F, new DomainProperties(64.0, 1.0, 1.0, false, 0.0, 1.0));
        if (DomainSpawnDeny.denies(helper.getLevel(), context.origin())
                && DomainSpawnDeny.denies(helper.getLevel(), context.origin().offset(40, 0, 40))) {
            helper.succeed();
            return;
        }
        helper.assertTrue(attempts < 200, "Lumina never raised its spawn-deny token");
        helper.runAfterDelay(1, () -> pollToken(helper, effect, context, attempts + 1));
    }

    /** Registry assembly: the twelve built-ins plus ownerless client renderers; spawn tables differ by day/night. */
    @GameTest(template = "foundation_empty", timeoutTicks = 200)
    public static void registry_initializes_twelve_effects(GameTestHelper helper) {
        DomainEffectRegistry.initialize();
        DomainEffectRegistry.bindToSigns();   // no-ops for absent signs; must not throw
        var effects = DomainEffectRegistry.effects();
        helper.assertTrue(effects.size() == 12, "Expected 12 domain effects, got " + effects.size());
        for (String sign : new String[]{"aevitas", "armara", "bootes", "discidia", "evorsio", "fornax",
                "horologium", "lucerna", "mineralis", "octans", "pelotrio", "vicio"}) {
            var id = new net.minecraft.resources.ResourceLocation(Omphalos.MODID, sign);
            helper.assertTrue(effects.containsKey(id), "Missing effect for " + sign);
            helper.assertTrue(DomainEffectRegistry.clientRenderer(id) != null, "Missing client renderer for " + sign);
        }
        var spawnTablesDiffer = !DomainPositionEntries.SpawnEntry.DAY_TABLE.equals(DomainPositionEntries.SpawnEntry.NIGHT_TABLE);
        helper.assertTrue(spawnTablesDiffer, "Day and night spawn tables must differ");
        helper.succeed();
    }

    /** Soar: flight grant/revoke seam on a survival player (grant → mayfly, revoke → cleared). */
    @GameTest(template = "foundation_empty", timeoutTicks = 400)
    public static void soar_grants_and_revokes_flight(GameTestHelper helper) {
        var level = helper.getLevel();
        // The player is deliberately NOT placed in the level: PlayerList.placeNewPlayer reaches
        // NetworkHooks.sendMCRegistryPackets, which NPEs on channel-less test connections. The
        // grant/revoke seam does not require world membership.
        var player = new net.minecraft.server.level.ServerPlayer(level.getServer(), level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "soar-test-player")) {
            @Override public boolean isSpectator() { return false; }
            @Override public boolean isCreative() { return false; }
        };
        helper.assertTrue(!player.getAbilities().mayfly, "Test player unexpectedly starts with flight");
        helper.assertTrue(DomainEffectSoar.grantFlight(player), "Soar refused to grant flight");
        helper.assertTrue(player.getAbilities().mayfly, "Flight not granted");
        helper.assertTrue(!DomainEffectSoar.grantFlight(player), "Re-grant must be a no-op");
        DomainEffectSoar.revokeFlight(player);
        helper.assertTrue(!player.getAbilities().mayfly && !player.getAbilities().flying, "Flight not revoked");
        DomainEffectSoar.revokeFlight(player);   // idempotent
        helper.assertTrue(!player.getAbilities().mayfly, "Second revoke changed state");
        helper.succeed();
    }
}

package com.mpp.stellaeomphalos.player.charge;

import com.mpp.stellaeomphalos.Omphalos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Omphalos.MODID)
@PrefixGameTestTemplate(false)
public final class ChargeGameTests {
    @GameTest(template = "foundation_empty", timeoutTicks = 100)
    public static void charge_drain_simulate_does_not_consume(GameTestHelper helper) {
        ChargeBootstrap.attach(null);
        var level = helper.getLevel();
        // Bare ServerPlayer (same pattern as vanilla's mock players): no connection, no login
        // event, survival semantics forced so the creative bypass cannot mask deductions.
        var player = new net.minecraft.server.level.ServerPlayer(level.getServer(), level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "charge-test-player")) {
            @Override public boolean isSpectator() { return false; }
            @Override public boolean isCreative() { return false; }
        };
        var service = StarlightChargeService.get(level.getServer());
        try {
            helper.assertTrue(service.charge(player) == 1.0F, "First access must initialize and report full charge");
            helper.assertTrue(service.hasAtLeast(player, 0.9F), "hasAtLeast wrong at full charge");
            helper.assertTrue(service.drain(player, 0.4F, true), "Simulated drain refused");
            helper.assertTrue(service.charge(player) == 1.0F, "Simulated drain consumed charge");
            helper.assertTrue(service.drain(player, 0.4F, false), "Drain refused");
            helper.assertTrue(Math.abs(service.charge(player) - 0.6F) < 1.0E-6, "Drain did not deduct: " + service.charge(player));
            helper.assertTrue(!service.drain(player, 0.7F, true), "Overdraft probe accepted");
            helper.assertTrue(Math.abs(service.charge(player) - 0.6F) < 1.0E-6, "Failed probe changed charge");
            service.onDisconnect(player.getUUID());
            helper.assertTrue(service.charge(player) == 1.0F, "Disconnect cleanup must reset the ledger entry");
        } finally {
            service.onDisconnect(player.getUUID());
        }
        helper.succeed();
    }
}

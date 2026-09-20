package com.mpp.stellaeomphalos.player.boon.root;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.level.BlockEvent;

/**
 * Evorsio root behavior: breaking blocks grants experience at sqrt(hardness * 0.15) — the square
 * root compresses high-hardness blocks so late-game mining does not flood the curve. Fake players
 * (machines) never generate experience.
 */
public final class UpheavalRootBoon {

    public static final String SIGN = "evorsio";
    public static final double FACTOR = 0.15;

    private UpheavalRootBoon() {}

    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || player instanceof FakePlayer) return;
        if (event.getLevel().isClientSide()) return;
        if (!RootExpSupport.active(player, SIGN)) return;
        float hardness = event.getState().getDestroySpeed(event.getLevel(), event.getPos());
        if (hardness <= 0) return;
        RootExpSupport.grant(player, SIGN, compress(hardness));
    }

    /** Square-root compression of the raw hardness-scaled gain. */
    public static double compress(double hardness) {
        return Math.sqrt(Math.max(0.0, hardness) * FACTOR);
    }
}

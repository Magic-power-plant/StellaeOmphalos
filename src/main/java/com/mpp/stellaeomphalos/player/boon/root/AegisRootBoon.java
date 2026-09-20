package com.mpp.stellaeomphalos.player.boon.root;

import com.mpp.stellaeomphalos.OmphalosConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;

/**
 * Armara root behavior: taking damage grants experience, scaled by damage kind
 * (lava 0.01 / fire 0.2 / starve 0.1 / drown 0.05 / cactus 0.01 / mob attacks 1.3).
 * Inactive players (no movement or rotation change within {@code gameplay.inactivityThresholdMs})
 * gain nothing, filtering AFK damage farms.
 */
public final class AegisRootBoon {

    public static final String SIGN = "armara";

    /** Damage categories with their experience factors. */
    public enum DamageCategory {
        LAVA(0.01), FIRE(0.2), STARVE(0.1), DROWN(0.05), CACTUS(0.01), MOB(1.3), OTHER(0.0);

        public final double factor;

        DamageCategory(double factor) {
            this.factor = factor;
        }
    }

    private static final Map<UUID, Long> LAST_ACTIVE_MS = new HashMap<>();
    private static final Map<UUID, double[]> LAST_POSE = new HashMap<>();

    private AegisRootBoon() {}

    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        var pose = new double[] { player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot() };
        var previous = LAST_POSE.put(player.getUUID(), pose);
        if (previous == null || moved(previous, pose)) LAST_ACTIVE_MS.put(player.getUUID(), System.currentTimeMillis());
    }

    private static boolean moved(double[] a, double[] b) {
        for (int i = 0; i < a.length; i++) if (Math.abs(a[i] - b[i]) > 1.0E-4) return true;
        return false;
    }

    public static void onDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide) return;
        var category = categorize(event.getSource());
        if (category.factor <= 0) return;
        if (!RootExpSupport.active(player, SIGN)) return;
        long lastActive = LAST_ACTIVE_MS.getOrDefault(player.getUUID(), 0L);
        long idleMs = System.currentTimeMillis() - lastActive;
        if (lastActive != 0 && idleMs > OmphalosConfig.SERVER.integer("gameplay.inactivityThresholdMs")) return;
        RootExpSupport.grant(player, SIGN, event.getAmount() * category.factor);
    }

    /** Maps a damage source onto its experience category; the first matching band wins. */
    public static DamageCategory categorize(DamageSource source) {
        if (source.is(DamageTypes.LAVA)) return DamageCategory.LAVA;
        if (source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.ON_FIRE)) return DamageCategory.FIRE;
        if (source.is(DamageTypes.STARVE)) return DamageCategory.STARVE;
        if (source.is(DamageTypes.DROWN)) return DamageCategory.DROWN;
        if (source.is(DamageTypes.CACTUS)) return DamageCategory.CACTUS;
        if (source.getEntity() instanceof LivingEntity) return DamageCategory.MOB;
        return DamageCategory.OTHER;
    }

    static void clear(UUID player) {
        LAST_ACTIVE_MS.remove(player);
        LAST_POSE.remove(player);
    }

    static void clearAll() {
        LAST_ACTIVE_MS.clear();
        LAST_POSE.clear();
    }
}

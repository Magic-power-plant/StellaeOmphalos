package com.mpp.stellaeomphalos.constellation.starmap;

import com.mojang.logging.LogUtils;
import com.mpp.stellaeomphalos.Omphalos;
import com.mpp.stellaeomphalos.constellation.sign.Sign;
import com.mpp.stellaeomphalos.constellation.sign.SignRegistry;
import com.mpp.stellaeomphalos.core.bootstrap.RuntimeServices;
import com.mpp.stellaeomphalos.core.util.CooldownGuard;
import com.mpp.stellaeomphalos.data.loader.DataBootstrap;
import com.mpp.stellaeomphalos.data.loader.DataTable;
import com.mpp.stellaeomphalos.network.toServer.PktImprintEngrave;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * Assembly hooks for starmap engraving. Declares the sign_imprint_effects data table (declared at
 * class load, inside the table-declaration window), attach() initializes registry content and
 * subscribes the cheat-death listener plus the datapack reload hook; the C2S engrave handler is
 * registered per server session (automatically on ServerStartedEvent, or explicitly via
 * attachServer() if the integrator prefers to wire it).
 */
public final class StarmapBootstrap {
    public static final DataTable<SignImprintEffects.Entry> SIGN_IMPRINT_EFFECTS =
            DataBootstrap.TABLES.declare(new DataTable<>(new ResourceLocation(Omphalos.MODID, "sign_imprint_effects"),
                    SignImprintEffects.Entry.CODEC, SignImprintEffects::validate));

    private static final AtomicBoolean ATTACHED = new AtomicBoolean();
    private static final CooldownGuard ENGRAVE_COOLDOWN = new CooldownGuard();
    private static final ResourceLocation ENGRAVE_ABILITY = new ResourceLocation(Omphalos.MODID, "imprint_engrave");
    private static final long ENGRAVE_COOLDOWN_TICKS = 10;

    private StarmapBootstrap() {}

    public static void attach(IEventBus modBus) {
        if (!ATTACHED.compareAndSet(false, true)) return;
        StarmapContent.initialize();
        var bus = MinecraftForge.EVENT_BUS;
        bus.addListener(StarmapBootstrap::onLivingDeath);
        bus.addListener(StarmapBootstrap::onServerStarted);
        bus.addListener(StarmapBootstrap::dataReloaded);
    }

    /** Registers the C2S handler into the current server session; duplicate registration is tolerated. */
    public static void attachServer() { registerServerHandler(); }

    /**
     * Runs after the foundation DataTableLoader and after SignBootstrap's sign rebuild (listener
     * registration order: DataBootstrap -> SignBootstrap -> StarmapBootstrap), so the table
     * snapshot and the sign registry are both fresh when the imprint effects are rebuilt.
     */
    private static void dataReloaded(AddReloadListenerEvent event) {
        event.addListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) manager -> rebuildImprintEffects());
    }

    public static void rebuildImprintEffects() {
        rebuildImprintEffects(DataBootstrap.TABLES.entries(SIGN_IMPRINT_EFFECTS));
    }

    /**
     * Rebuilds the datapack-driven portion of {@link SignImprintEffectRegistry}. The framework
     * rolled the whole table back if any file errored, so the snapshot here is always consistent;
     * entries whose sign is still unknown after the sign rebuild are dropped with a warning.
     */
    public static void rebuildImprintEffects(Map<ResourceLocation, SignImprintEffects.Entry> entries) {
        var rebuilt = new LinkedHashMap<ResourceLocation, SignImprintEffectRegistry.Entry>();
        entries.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(file -> {
            var sign = file.getValue().sign();
            if (SignRegistry.byId(sign) == null) {
                LogUtils.getLogger().warn("Imprint effects {} reference unregistered sign {}", file.getKey(), sign);
                return;
            }
            var converted = SignImprintEffects.convert(file.getValue());
            if (converted != null) rebuilt.put(sign, converted);
        });
        SignImprintEffectRegistry.replaceDataDriven(rebuilt);
    }

    private static void onServerStarted(ServerStartedEvent event) { registerServerHandler(); }

    private static void registerServerHandler() {
        try {
            RuntimeServices.current().handlers().register(PktImprintEngrave.class, StarmapBootstrap::onEngrave);
        } catch (IllegalArgumentException duplicate) {
            if (duplicate.getMessage() == null || !duplicate.getMessage().startsWith("Duplicate payload handler")) throw duplicate;
        }
    }

    private static void onEngrave(ServerPlayer player, PktImprintEngrave packet) {
        long now = player.serverLevel().getGameTime();
        ENGRAVE_COOLDOWN.expire(now);
        if (!ENGRAVE_COOLDOWN.acquire(player.getUUID(), ENGRAVE_ABILITY, now, ENGRAVE_COOLDOWN_TICKS)) return;
        if (packet.strokes().isEmpty()) return;
        var drawn = new ArrayList<SignDrawn>();
        int maxAnchor = StarPoint.GRID - SignDrawn.DRAW_SIZE;
        for (var stroke : packet.strokes()) {
            Sign sign;
            try {
                sign = SignRegistry.byNumericId(stroke.signId());
            } catch (RuntimeException unknown) {
                return;
            }
            if (sign == null) return;
            if (stroke.gridX() < 0 || stroke.gridX() > maxAnchor || stroke.gridZ() < 0 || stroke.gridZ() > maxAnchor) return;
            drawn.add(new SignDrawn(sign.id(), stroke.gridX(), stroke.gridZ()));
        }
        var discovery = ImprintDiscoverySource.of(player);
        for (var stroke : drawn) if (!discovery.knowsSign(stroke.sign())) return;
        // Server-side coverage recompute: clients only send strokes, never proportions.
        var imprint = SignImprintCompiler.compile(drawn);
        var stack = player.getMainHandItem();
        if (stack.isEmpty()) return;
        imprint.write(stack);
        imprint.applyEnchantments(stack, player.getRandom(), discovery);
        imprint.applyMobEffects(stack, player.getRandom());
    }

    /** One-shot cheat death: cancel the death, heal to a sliver, consume the effect. */
    private static void onLivingDeath(LivingDeathEvent event) {
        var entity = event.getEntity();
        if (entity.level().isClientSide) return;
        var effect = StarmapContent.DEATH_PROTECTION.get();
        if (!entity.hasEffect(effect)) return;
        event.setCanceled(true);
        entity.setHealth(Math.max(1.0F, entity.getMaxHealth() * 0.1F));
        entity.removeEffect(effect);
    }
}

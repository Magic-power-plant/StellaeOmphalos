package com.mpp.stellaeomphalos.player.boon;

import com.mojang.logging.LogUtils;
import com.mpp.stellaeomphalos.Omphalos;
import com.mpp.stellaeomphalos.constellation.attribute.BoonTranslator;
import com.mpp.stellaeomphalos.constellation.attribute.BoonValueBridge;
import com.mpp.stellaeomphalos.constellation.boon.BoonTree;
import com.mpp.stellaeomphalos.constellation.boon.BoonTreeTables;
import com.mpp.stellaeomphalos.constellation.starmap.ImprintDiscoverySource;
import com.mpp.stellaeomphalos.player.boon.root.AegisRootBoon;
import com.mpp.stellaeomphalos.player.boon.root.RootBehaviorCleanup;
import com.mpp.stellaeomphalos.player.boon.root.SeveranceRootBoon;
import com.mpp.stellaeomphalos.player.boon.root.SoarRootBoon;
import com.mpp.stellaeomphalos.player.boon.root.UpheavalRootBoon;
import com.mpp.stellaeomphalos.player.boon.root.VerdanceRootBoon;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Assembly point of the boon module. The integrator calls {@link #attach(IEventBus)} from the
 * Omphalos constructor, after AttributeBootstrap and SignBootstrap (the tree reload listener must
 * run after theirs so attributes and signs are fresh). Listeners live on the Forge bus; there is
 * no C2S payload this milestone — unlocking is server-authoritative through the op command
 * (GUI arrives in a later milestone). The client hook is
 * com.mpp.stellaeomphalos.client.boon.BoonMirror.attach().
 */
public final class BoonBootstrap {

    private static final AtomicBoolean ATTACHED = new AtomicBoolean();
    private static final ResourceLocation UNLOCK_COOLDOWN = new ResourceLocation(Omphalos.MODID, "boon_unlock_command");

    private BoonBootstrap() {}

    public static void attach(IEventBus modBus) {
        if (!ATTACHED.compareAndSet(false, true)) return;
        // Force table declaration inside the class-loading window (tolerates late GameTest attach).
        var declared = BoonTreeTables.BOON_TREE;
        BoonValueBridge.registerProvider(new BoonBridgeProvider());
        ImprintDiscoverySource.registerProvider(BoonProgress::getServer);
        BoonTranslator.setNodeGridLookup(nodeId -> {
            if (!BoonTree.ready()) return null;
            var node = BoonTree.get().node(nodeId);
            return node == null ? null : new int[] { node.gridX(), node.gridZ() };
        });

        freezeBootstrapTree();
        if (modBus != null) modBus.addListener(BoonBootstrap::commonSetup);
        var forge = MinecraftForge.EVENT_BUS;
        forge.addListener(BoonBootstrap::addReloadListener);
        forge.addListener(BoonBootstrap::login);
        forge.addListener(BoonBootstrap::logout);
        forge.addListener(BoonBootstrap::respawn);
        forge.addListener(BoonBootstrap::death);
        forge.addListener(BoonBootstrap::serverTick);
        forge.addListener(BoonBootstrap::serverStop);
        forge.addListener(BoonBootstrap::commands);
        forge.addListener(VerdanceRootBoon::onBlockPlace);
        forge.addListener(UpheavalRootBoon::onBlockBreak);
        forge.addListener(AegisRootBoon::onDamage);
        forge.addListener(SeveranceRootBoon::onDamage);
        forge.addListener(AegisRootBoon::onPlayerTick);
        forge.addListener(SoarRootBoon::onPlayerTick);
        LogUtils.getLogger().debug("Boon module attached (table {})", declared.id());
    }

    /** Freezes the (empty) bootstrap tree; real content arrives with the server data reload. */
    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(BoonBootstrap::freezeBootstrapTree);
    }

    private static synchronized void freezeBootstrapTree() {
        if (BoonTree.ready()) return;
        var empty = new BoonTree();
        empty.freeze();
        BoonTree.rebuild(empty);
    }

    /** Runs after the foundation DataTableLoader; attach order places it after the sign rebuild. */
    private static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) manager ->
                BoonTreeTables.rebuildFromTables());
    }

    private static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) BoonEffectDispatcher.onLogin(player);
    }

    private static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RootBehaviorCleanup.onLogout(player.getUUID());
            BoonEffectDispatcher.onLogout(player);
        }
    }

    private static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) BoonEffectDispatcher.onRespawn(player);
    }

    private static void death(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) BoonEffectDispatcher.onDeath(player);
    }

    private static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer() == null) return;
        BoonEffectDispatcher.onServerTick(event.getServer(), event.getServer().getTickCount());
    }

    private static void serverStop(ServerStoppingEvent event) {
        BoonEffectDispatcher.onServerStop();
        RootBehaviorCleanup.onServerStop();
    }

    /** Op-only server command surface until the GUI milestone: unlock / reset / discover / attune. */
    private static void commands(RegisterCommandsEvent event) {
        var boon = Commands.literal("boon")
                .then(Commands.literal("unlock")
                        .then(Commands.argument("node", ResourceLocationArgument.id())
                                .executes(context -> {
                                    var player = context.getSource().getPlayerOrException();
                                    var node = ResourceLocationArgument.getId(context, "node");
                                    if (!BoonCooldownTable.get().acquire(player.getUUID(), UNLOCK_COOLDOWN, 10)) {
                                        context.getSource().sendFailure(Component.translatable("stellaeomphalos.command.boon_cooldown"));
                                        return 0;
                                    }
                                    boolean ok = BoonProgress.getServer(player).unlock(player, node);
                                    if (ok) context.getSource().sendSuccess(() ->
                                            Component.translatable("stellaeomphalos.command.boon_unlocked", node.toString()), true);
                                    else context.getSource().sendFailure(Component.translatable("stellaeomphalos.command.boon_denied", node.toString()));
                                    return ok ? 1 : 0;
                                })))
                .then(Commands.literal("reset").executes(context -> {
                    var player = context.getSource().getPlayerOrException();
                    BoonProgress.getServer(player).reset(player);
                    context.getSource().sendSuccess(() -> Component.translatable("stellaeomphalos.command.boon_reset"), true);
                    return 1;
                }))
                .then(Commands.literal("discover")
                        .then(Commands.argument("sign", ResourceLocationArgument.id()).executes(context -> {
                            var player = context.getSource().getPlayerOrException();
                            var sign = ResourceLocationArgument.getId(context, "sign");
                            BoonProgress.getServer(player).discover(player, sign);
                            context.getSource().sendSuccess(() ->
                                    Component.translatable("stellaeomphalos.command.boon_discovered", sign.toString()), true);
                            return 1;
                        })))
                .then(Commands.literal("attune")
                        .then(Commands.argument("sign", ResourceLocationArgument.id()).executes(context -> {
                            var player = context.getSource().getPlayerOrException();
                            var sign = ResourceLocationArgument.getId(context, "sign");
                            BoonProgress.getServer(player).setAttuned(player, sign);
                            context.getSource().sendSuccess(() ->
                                    Component.translatable("stellaeomphalos.command.boon_attuned", sign.toString()), true);
                            return 1;
                        })));
        event.getDispatcher().register(Commands.literal(Omphalos.MODID).requires(source -> source.hasPermission(2)).then(boon));
    }
}

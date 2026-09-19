package com.mpp.stellaeomphalos.client;

import com.mojang.logging.LogUtils;
import com.mpp.stellaeomphalos.Omphalos;
import com.mpp.stellaeomphalos.client.event.ClientReadyEvent;
import com.mpp.stellaeomphalos.client.event.ClientSessionCleaner;
import com.mpp.stellaeomphalos.client.screen.ConfigOverviewScreen;
import com.mpp.stellaeomphalos.core.util.tick.ClientTickScheduler;
import com.mpp.stellaeomphalos.network.*;
import com.mpp.stellaeomphalos.network.sync.MirrorDataHub;
import com.mpp.stellaeomphalos.network.toClient.*;
import com.mpp.stellaeomphalos.network.toServer.*;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = Omphalos.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class OmphalosClient {
    private static final ClientSyncGate GATE = new ClientSyncGate();
    private static final MirrorDataHub MIRRORS = new MirrorDataHub();
    private static final PayloadHandlers<Minecraft> HANDLERS = new PayloadHandlers<>();
    private static ClientTickScheduler scheduler = scheduler();
    private static boolean announced;
    private static int configVersion;
    private static long debugUntil;
    private OmphalosClient() {}
    private static ClientTickScheduler scheduler() { return new ClientTickScheduler(error -> LogUtils.getLogger().error("Client task failed", error)); }
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            OmphalosChannel.setClientReceiver(OmphalosClient::receive);
            ClientSessionCleaner.register("sync_gate", GATE::reset);
            ClientSessionCleaner.register("dataset_mirrors", MIRRORS::clear);
            ClientSessionCleaner.register("fragment_assembler", OmphalosChannel::resetClient);
            ClientSessionCleaner.register("client_tasks", () -> { scheduler.close(); scheduler = scheduler(); });
            ClientSessionCleaner.register("session_metadata", () -> { announced = false; configVersion = 0; debugUntil = 0; });
            MinecraftForge.EVENT_BUS.addListener(OmphalosClient::tick);
            MinecraftForge.EVENT_BUS.addListener(OmphalosClient::logout);
            MinecraftForge.EVENT_BUS.addListener(OmphalosClient::login);
            MinecraftForge.EVENT_BUS.addListener(OmphalosClient::debugClick);
            ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new ConfigOverviewScreen(parent)));
        });
    }
    private static void login(ClientPlayerNetworkEvent.LoggingIn event) { ClientSessionCleaner.clear(); }
    private static void logout(ClientPlayerNetworkEvent.LoggingOut event) { ClientSessionCleaner.clear(); }
    private static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        OmphalosChannel.tickClient(); GATE.expire(System.nanoTime()); scheduler.advance(1024);
        var minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;
        if (!announced) { announced = true; MinecraftForge.EVENT_BUS.post(new ClientReadyEvent()); }
        if (GATE.ready(System.nanoTime())) OmphalosChannel.sendToServer(new PktClientSyncReady());
    }
    private static void receive(OmphalosPayload payload) {
        if (payload instanceof PktSyncGateOpen packet) GATE.begin(ProtocolVersion.parse(packet.protocol()), System.nanoTime());
        else if (payload instanceof PktSyncGateClose) { GATE.reset(); MIRRORS.clear(); }
        else if (payload instanceof PktSyncDataset packet) {
            if (!MIRRORS.apply(packet)) sendDependent(new PktDatasetRequest(packet.dataset(), MIRRORS.version(packet.dataset())));
        } else if (payload instanceof PktConfigVersion packet) configVersion = packet.version();
        else if (payload instanceof PktThrottleNotice packet) LogUtils.getLogger().debug("Throttled payload {}: {} dropped", packet.payloadId(), packet.dropped());
        else if (payload instanceof PktNetworkDebugDump packet) {
            if (packet.data().getBoolean("SessionStarted")) debugUntil = scheduler.currentTick() + 400;
            else Minecraft.getInstance().gui.getChat().addMessage(net.minecraft.network.chat.Component.literal(packet.position().toShortString() + ": " + packet.data()));
        } else if (payload instanceof PktMigrationReport packet) {
            var chat = Minecraft.getInstance().gui.getChat();
            chat.addMessage(net.minecraft.network.chat.Component.translatable("stellaeomphalos.command.migration_report", packet.reportId(), packet.issues().size()));
            packet.issues().forEach(issue -> chat.addMessage(net.minecraft.network.chat.Component.literal(issue)));
        }
        else if (!HANDLERS.dispatch(Minecraft.getInstance(), payload)) throw new IllegalArgumentException("No client payload handler");
    }
    public static boolean sendDependent(OmphalosPayload payload) {
        if (!GATE.allows(OmphalosChannel.PAYLOADS.type(payload).minimumMinor(), System.nanoTime())) return false;
        OmphalosChannel.sendToServer(payload);
        return true;
    }
    private static void debugClick(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide && event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND
                && debugUntil > scheduler.currentTick()) sendDependent(new PktNetworkDebugDumpRequest(event.getPos()));
    }
    public static MirrorDataHub mirrors() { return MIRRORS; }
    public static PayloadHandlers<Minecraft> handlers() { return HANDLERS; }
    public static int configVersion() { return configVersion; }
}

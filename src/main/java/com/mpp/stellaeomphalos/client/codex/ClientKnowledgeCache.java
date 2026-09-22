package com.mpp.stellaeomphalos.client.codex;

import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import com.mpp.stellaeomphalos.client.OmphalosClient;
import com.mpp.stellaeomphalos.client.event.ClientSessionCleaner;
import com.mpp.stellaeomphalos.core.platform.*;
import com.mpp.stellaeomphalos.knowledge.codex.*;
import com.mpp.stellaeomphalos.network.toClient.*;
import com.mpp.stellaeomphalos.network.toServer.PktKnowledgeQuery;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

/** One disposable world-session mirror; deltas require an exact revision match. */
public final class ClientKnowledgeCache {
    public static final CodexNavigator NAVIGATOR = new CodexNavigator();
    public static final StudyCanvas CANVAS = new StudyCanvas();
    public static final CodexSearchIndex SEARCH = new CodexSearchIndex();
    private static CompoundTag state = new CompoundTag(),
            gauges = new CompoundTag(),
            projections = new CompoundTag(),
            blueprints = new CompoundTag();
    private static KnowledgeSnapshot record = KnowledgeSnapshot.empty();
    private static Map<ResourceLocation, CodexPage> pages = Map.of();
    private static final Map<UUID, CompoundTag> mantles = new HashMap<>();
    private static final Set<ResourceLocation> missing = new HashSet<>();
    private static int session = Integer.MIN_VALUE;
    private static long revision, epoch = -1;
    private static float hudFade;
    private static Boolean codexOverview;
    private static String codexOverlay = "", codexOverlayTarget = "";
    private static String lastShard = "";
    private static net.minecraft.network.chat.Component notice = net.minecraft.network.chat.Component.empty();
    public static net.minecraft.network.chat.Component notice() { return notice; }
    public static CompoundTag mantle(UUID player) { return mantles.getOrDefault(player, new CompoundTag()).copy(); }
    private static CompoundTag signs = new CompoundTag(), signIds = new CompoundTag();

    private ClientKnowledgeCache() {}

    public static void attach() {
        ClientSessionCleaner.register("knowledge", ClientKnowledgeCache::clear);
        KnowledgeBridge.installOpener(
                p -> Minecraft.getInstance().setScreen(new CodexScreen("")));
        var handlers = OmphalosClient.handlers();
        handlers.register(PktStarRecord.class, (mc, p) -> apply(p));
        handlers.register(
                PktStarRecordDelta.class,
                (mc, p) -> {
                    if (!apply(p)) OmphalosClient.sendDependent(new PktKnowledgeQuery("record"));
                });
        handlers.register(
                PktCodexDefinitions.class,
                (mc, p) -> {
                    var next = new LinkedHashMap<ResourceLocation, CodexPage>();
                    var data = p.data();
                    for (var key : data.getAllKeys()) {
                        if (key.equals("SignGeometry")) { com.mpp.stellaeomphalos.client.sign.SignDefinitionMirror.replace(data.getCompound(key));continue; }
                        if (key.equals("SignIds")) { signIds = data.getCompound(key).copy(); continue; }
                        if (key.equals("Signs")) {
                            signs = data.getCompound(key).copy();
                            continue;
                        }
                        if (key.equals("Blueprints")) {
                            blueprints = data.getCompound(key).copy();
                            continue;
                        }
                        var id = ResourceLocation.tryParse(key);
                        if (id == null) continue;
                        try {
                            CodexPage.CODEC
                                    .parse(
                                            JsonOps.INSTANCE,
                                            JsonParser.parseString(data.getString(key)))
                                    .result()
                                    .ifPresent(page -> next.put(id, page));
                        } catch (RuntimeException invalid) {
                            LogUtils.getLogger().warn("Invalid synchronized codex page {}", key);
                        }
                    }
                    pages = Map.copyOf(next);
                    epoch = p.epoch();
                    SEARCH.clear();
                    missing.clear();
                    refresh();
                });
        handlers.register(
                PktOpenCodex.class,
                (mc, p) -> {
                    if (mc.player != null) mc.setScreen(new CodexScreen(p.route()));
                });
        handlers.register(
                PktShardRevealed.class,
                (mc, p) -> {
                    lastShard = p.shard();
                    com.mpp.stellaeomphalos.client.sound.UiSounds.play("shard_reveal");
                    notice = net.minecraft.network.chat.Component.translatable("stellaeomphalos.codex." +
                            (p.result().equals("NEW") ? "new" : p.result().equals("DUPLICATE") ? "duplicate" : "rejected"));
                    hudFade = 1;
                    if (mc.player != null)
                        mc.player.displayClientMessage(
                                net.minecraft.network.chat.Component.translatable(
                                        "stellaeomphalos.codex."
                                                + switch (p.result()) {
                                                    case "NEW" -> "new";
                                                    case "DUPLICATE" -> "duplicate";
                                                    default -> "rejected";
                                                }),
                                true);
                    if (mc.screen instanceof CodexScreen screen && !lastShard.isEmpty())
                        screen.shard(lastShard);
                });
        handlers.register(
                PktCodexUnlock.class,
                (mc, p) -> {
                    hudFade = 1;
                    notice = net.minecraft.network.chat.Component.translatable("stellaeomphalos.codex.unlocked", p.nodes().size());
                    SEARCH.clear();
                });
        handlers.register(
                PktMantleState.class,
                (mc, p) -> {
                    try {
                        if (mantles.size() < 256 || mantles.containsKey(UUID.fromString(p.player())))
                            mantles.put(UUID.fromString(p.player()), p.data());
                    } catch (IllegalArgumentException ignored) {
                    }
                });
        handlers.register(PktReaderProjection.class, (mc, p) -> projections = p.data());
        handlers.register(
                PktGaugeReadings.class,
                (mc, p) -> {
                    gauges = p.data();
                    refresh();
                });
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(
                ClientKnowledgeCache::clonePlayer);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(ClientKnowledgeCache::tick);
        net.minecraft.client.renderer.item.ItemProperties.register(
                com.mpp.stellaeomphalos.content.item.KnowledgeContent.SHARD.get(),
                new ResourceLocation("stellaeomphalos:gated"),
                (stack, level, entity, seed) -> {
                    if (!ready() || !stack.hasTag()) return 0;
                    var id = ResourceLocation.tryParse(stack.getTag().getString("ShardId"));
                    if (id == null) return 1;
                    return KnowledgeCatalog.SHARDS
                                    .find(id)
                                    .filter(
                                            shard ->
                                                    shard.visibility()
                                                            .evaluate(
                                                                    context(
                                                                            level == null
                                                                                    ? new ResourceLocation(
                                                                                            "minecraft:overworld")
                                                                                    : level.dimension()
                                                                                            .location(),
                                                                            0))
                                                            .active())
                                    .isPresent()
                            ? 0
                            : 1;
                });
        MantleVisuals.attach();
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(GameplayHud::render);
    }

    private static void clonePlayer(
            net.minecraftforge.client.event.ClientPlayerNetworkEvent.Clone e) {
        clear();
    }

    private static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent e) {
        if (e.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        hudFade = Math.max(0, hudFade - .01F);
        var mc = Minecraft.getInstance();
        if (mc.player != null && session == Integer.MIN_VALUE && mc.player.tickCount % 20 == 0)
            OmphalosClient.sendDependent(new PktKnowledgeQuery("record"));
    }

    public static boolean apply(PktStarRecord p) {
        if (session != Integer.MIN_VALUE && p.session() != session || p.revision() < revision)
            return false;
        session = p.session();
        revision = p.revision();
        state = p.data();
        record = KnowledgeSnapshot.from(state, revision);
        SEARCH.clear();
        refresh();
        return true;
    }

    public static boolean apply(PktStarRecordDelta p) {
        if (p.session() != session || p.base() != revision || p.revision() <= revision)
            return false;
        var next = state.copy();
        p.changes().getAllKeys().forEach(k -> next.put(k, p.changes().get(k).copy()));
        state = next;
        revision = p.revision();
        record = KnowledgeSnapshot.from(state, revision);
        SEARCH.clear();
        refresh();
        return true;
    }

    private static void refresh() {
        var mc = Minecraft.getInstance();
        if (mc.screen instanceof CodexScreen screen) screen.refresh();
    }

    public static boolean ready() {
        return session != Integer.MIN_VALUE;
    }

    public static int signId(ResourceLocation id) {
        return signIds.contains(id.toString()) ? signIds.getInt(id.toString()) : -1;
    }

    public static CompoundTag signs() {
        return signs.copy();
    }

    public static com.mpp.stellaeomphalos.knowledge.research.GateContext context(
            ResourceLocation dimension, long tick) {
        var stages = new HashSet<String>();
        state.getList("GateStages", Tag.TAG_STRING).forEach(t -> stages.add(t.getAsString()));
        var tags = new HashSet<String>();
        state.getList("GateTags", Tag.TAG_STRING).forEach(t -> tags.add(t.getAsString()));
        return new com.mpp.stellaeomphalos.knowledge.research.GateContext(
                record, dimension, stages, tags, state.getBoolean("GateStageProvider"), tick);
    }

    public static KnowledgeSnapshot record() {
        return record;
    }

    public static Map<ResourceLocation, CodexPage> pages() {
        return pages.isEmpty() && session == Integer.MIN_VALUE
                ? KnowledgeCatalog.PAGES.defaults()
                : pages;
    }

    public static Optional<CodexPage> page(ResourceLocation id) {
        var page = pages().get(id);
        if (page == null && missing.add(id)) LogUtils.getLogger().warn("codex_page_missing {}", id);
        return Optional.ofNullable(page);
    }

    public static CompoundTag gauges() {
        return gauges.copy();
    }

    public static CompoundTag projections() {
        return projections.copy();
    }

    public static CompoundTag blueprints() {
        return blueprints.copy();
    }

    public static long epoch() {
        return epoch;
    }

    /** Live codex view state for this session; overview flag empty until the book is closed once. */
    public static Optional<Boolean> codexOverview() {
        return Optional.ofNullable(codexOverview);
    }

    public static String codexOverlay() {
        return codexOverlay;
    }

    public static String codexOverlayTarget() {
        return codexOverlayTarget;
    }

    public static void codexView(boolean overview, String overlay, String overlayTarget) {
        codexOverview = overview;
        codexOverlay = overlay;
        codexOverlayTarget = overlayTarget;
    }

    public static float hudFade() {
        return hudFade;
    }

    public static void clear() {
        com.mpp.stellaeomphalos.client.sign.SignDefinitionMirror.clear();
        state = new CompoundTag();
        gauges = new CompoundTag();
        projections = new CompoundTag();
        blueprints = new CompoundTag();
        signs = new CompoundTag();
        signIds = new CompoundTag();
        record = KnowledgeSnapshot.empty();
        pages = Map.of();
        mantles.clear();
        missing.clear();
        session = Integer.MIN_VALUE;
        revision = 0;
        epoch = -1;
        hudFade = 0;
        codexOverview = null;
        codexOverlay = "";
        codexOverlayTarget = "";
        lastShard = "";
        notice = net.minecraft.network.chat.Component.empty();
        NAVIGATOR.reset();
        CANVAS.reset();
        SEARCH.clear();
    }
}

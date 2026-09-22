package com.mpp.stellaeomphalos.content.item.knowledge;

import com.mpp.stellaeomphalos.constellation.attribute.BoonGaugeRegistry;
import com.mpp.stellaeomphalos.content.item.*;
import com.mpp.stellaeomphalos.core.bootstrap.RuntimeServices;
import com.mpp.stellaeomphalos.core.platform.*;
import com.mpp.stellaeomphalos.knowledge.codex.*;
import com.mpp.stellaeomphalos.knowledge.research.*;
import com.mpp.stellaeomphalos.knowledge.shard.ShardPool;
import com.mpp.stellaeomphalos.network.*;
import com.mpp.stellaeomphalos.network.toClient.*;
import com.mpp.stellaeomphalos.network.toServer.*;
import com.mpp.stellaeomphalos.player.boon.BoonProgress;
import com.mpp.stellaeomphalos.player.progress.*;

import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;

import java.util.*;

/** Wire adapters own validation; client-supplied ids never grant knowledge. */
public final class KnowledgeProtocol {
    private static final Map<UUID, CompoundTag> LAST = new HashMap<>();
    private static final Map<UUID, Long> VERSIONS = new HashMap<>(),
            REVEAL = new HashMap<>(),
            QUERY = new HashMap<>();
    private static final Map<UUID, Long> DEFINITION_EPOCH = new HashMap<>();
    private static int session;
    private static final Map<UUID, GateContext> CONTEXTS = new HashMap<>();

    private KnowledgeProtocol() {}

    public static void register() {
        session++;
        var services = RuntimeServices.current();
        services.throttlePolicy(PktRevealShard.class, 42, 1, .25);
        services.throttlePolicy(PktCodexRead.class, 47, 4, .5);
        services.throttlePolicy(PktKnowledgeQuery.class, 49, 2, .1);
        services.throttlePolicy(PktBoonAction.class, 51, 2, .1);
        services.throttlePolicy(PktCodexPreview.class, 52, 1, .1);
        services.throttlePolicy(PktObserveSign.class, 53, 1, .1);
        services.throttlePolicy(PktChartDraw.class, 55, 1, .1);
        var handlers = services.handlers();
        handlers.register(PktObserveSign.class, (player, packet) -> ObservationProtocol.observe(player, packet));
        handlers.register(PktChartDraw.class, (player, packet) -> {
            if (player.containerMenu instanceof com.mpp.stellaeomphalos.content.menu.PartSixMenus.StarChartTableMenu menu
                    && menu.containerId==packet.container() && menu.pos().equals(packet.origin()) && menu.stillValid(player)
                    && player.level().getBlockEntity(packet.origin()) instanceof com.mpp.stellaeomphalos.content.block.PartSixBlocks.MachineBlockEntity machine)
                machine.drawChart(player, packet.strokes());
        });
        handlers.register(
                PktRevealShard.class,
                (p, packet) ->
                        reveal(
                                p,
                                packet.hand() == 0
                                        ? InteractionHand.MAIN_HAND
                                        : InteractionHand.OFF_HAND));
        handlers.register(
                PktCodexPreview.class,
                (p, packet) -> {
                    if (!(p instanceof FakePlayer))
                        com.mpp.stellaeomphalos.content.world.WorldProtocol.preview(
                                p, packet.blueprint(), p.blockPosition());
                });
        handlers.register(PktCodexRead.class, (p, packet) -> read(p, packet.route()));
        handlers.register(
                PktKnowledgeQuery.class,
                (p, packet) -> {
                    long now = p.serverLevel().getGameTime();
                    if (p instanceof FakePlayer
                            || now - QUERY.getOrDefault(p.getUUID(), -100L) < 10) return;
                    QUERY.put(p.getUUID(), now);
                    switch (packet.kind()) {
                        case "record" -> {
                            LAST.remove(p.getUUID());
                            DEFINITION_EPOCH.remove(p.getUUID());
                            synchronize(p);
                        }
                        case "gauges" -> gauges(p);
                        default -> {}
                    }
                });
        handlers.register(
                PktBoonAction.class,
                (p, packet) -> {
                    if (p instanceof FakePlayer || !StarRecords.get(p).valid()) return;
                    var progress = BoonProgress.getServer(p);
                    switch (packet.action()) {
                        case "unlock" -> progress.unlock(p, packet.node());
                        case "seal" -> progress.setSealed(p, packet.node(), true);
                        case "unseal" -> progress.setSealed(p, packet.node(), false);
                        case "socket" -> socketHeld(p, packet.node());
                        case "unsocket" -> progress.unsocket(p, packet.node());
                        default -> {}
                    }
                });
    }

    /** Consume exactly one server-held gem only after validation; occupied sockets reject replacement. */
    public static boolean socketHeld(net.minecraft.server.level.ServerPlayer player, net.minecraft.resources.ResourceLocation nodeId) {
        var progress = BoonProgress.getServer(player);
        if (player instanceof FakePlayer || !StarRecords.get(player).valid()
                || !com.mpp.stellaeomphalos.constellation.boon.BoonTree.ready()
                || !progress.hasNode(nodeId) || progress.isSealed(nodeId)) return false;
        var node = com.mpp.stellaeomphalos.constellation.boon.BoonTree.get().node(nodeId);
        if (!(node instanceof com.mpp.stellaeomphalos.constellation.boon.SocketBoonNode socket)
                || !socket.contained(progress.nodeData(nodeId)).isEmpty()) return false;
        var held = player.getMainHandItem();
        if (!socket.accepts(held)) return false;
        if (!progress.socket(player, nodeId, held)) return false;
        held.shrink(1);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        return true;
    }

    public static void clear() {
        LAST.clear();
        CONTEXTS.clear();
        VERSIONS.clear();
        REVEAL.clear();
        QUERY.clear();
        DEFINITION_EPOCH.clear();
    }

    public static void logout(UUID id) {
        LAST.remove(id);
        CONTEXTS.remove(id);
        VERSIONS.remove(id);
        REVEAL.remove(id);
        QUERY.remove(id);
        DEFINITION_EPOCH.remove(id);
    }

    public static GateContext context(ServerPlayer p) {
        var current = CONTEXTS.get(p.getUUID());
        long tick = p.server.getTickCount();
        if (current != null
                && current.tick() == tick
                && current.dimension().equals(p.level().dimension().location())) return current;
        var extra = KnowledgeGateEnvironment.read(p);
        var next =
                new GateContext(
                        KnowledgeSnapshot.copy(StarRecords.get(p)),
                        p.level().dimension().location(),
                        extra.stages(),
                        extra.tags(),
                        extra.stagesPresent(),
                        tick);
        CONTEXTS.put(p.getUUID(), next);
        return next;
    }

    public static ItemStack createShard(ServerPlayer player) {
        var pool = KnowledgeCatalog.SHARDS.eligible(context(player), KnowledgeCatalog::localized);
        if (pool.isEmpty()) return ItemStack.EMPTY;
        var store = StarRecords.store(player.server);
        boolean publicPool =
                com.mpp.stellaeomphalos.OmphalosConfig.SERVER
                                .snapshot()
                                .get("progression.shardPoolMode")
                        == com.mpp.stellaeomphalos.OmphalosConfig.ShardPoolMode.PUBLIC_POOL;
        if (publicPool) {
            var remaining = pool.stream().filter(shard -> !store.claimed(shard.id())).toList();
            if (remaining.isEmpty()) store.renewPool(pool.stream().map(s -> s.id()).toList());
            else pool = remaining;
        }
        var stack = new ItemStack(KnowledgeContent.SHARD.get());
        long seed =
                player.getUUID().getMostSignificantBits()
                        ^ player.getUUID().getLeastSignificantBits()
                        ^ store.poolSeed()
                        ^ store.nextSeed() * 0x9E3779B97F4A7C15L;
        var shard = ShardPool.resolve(seed, pool).orElseThrow();
        if (publicPool) store.claim(shard.id());
        stack.getOrCreateTag().putLong("ShardSeed", seed);
        // Pin the server-selected identity so advancement, trades and reloads cannot reroll this
        // item.
        stack.getOrCreateTag().putString("ShardId", shard.id().toString());
        return stack;
    }

    public static boolean reveal(ServerPlayer player, InteractionHand hand) {
        if (player instanceof FakePlayer || !StarRecords.get(player).valid()) return false;
        long now = player.serverLevel().getGameTime();
        if (now - REVEAL.getOrDefault(player.getUUID(), -100L) < 4) return false;
        REVEAL.put(player.getUUID(), now);
        var stack = player.getItemInHand(hand);
        int slot = hand == InteractionHand.MAIN_HAND ? 0 : 1;
        if (!stack.is(KnowledgeContent.SHARD.get()) || !LoreShardItem.seeded(stack)) {
            com.mpp.stellaeomphalos.network.SafeDispatch.send(
                    player, new PktShardRevealed("", slot, "REJECTED"));
            return false;
        }
        var pool = KnowledgeCatalog.SHARDS.eligible(context(player), KnowledgeCatalog::localized);
        var rawPinned = stack.getTag().getString("ShardId");
        var pinned = rawPinned.isBlank() ? null : ResourceLocation.tryParse(rawPinned);
        var selected =
                pinned == null
                        ? ShardPool.resolve(stack.getTag().getLong("ShardSeed"), pool)
                        : KnowledgeCatalog.SHARDS.find(pinned).filter(s -> pool.contains(s));
        if (selected.isEmpty()) {
            com.mpp.stellaeomphalos.network.SafeDispatch.send(
                    player, new PktShardRevealed("", slot, "REJECTED"));
            return false;
        }
        var shard = selected.get();
        boolean fresh = StarRecords.ACCESS.collectShard(player, shard.id());
        stack.shrink(1);
        com.mpp.stellaeomphalos.network.SafeDispatch.send(
                player,
                new PktShardRevealed(shard.id().toString(), slot, fresh ? "NEW" : "DUPLICATE"));
        if (fresh)
            MinecraftForge.EVENT_BUS.post(
                    new ProgressMilestoneEvent(
                            player,
                            "shard",
                            shard.id(),
                            StarRecords.get(player).unlockedShards().size()));
        return true;
    }

    public static boolean read(ServerPlayer player, String encoded) {
        if (player instanceof FakePlayer) return false;
        var route = CodexRoute.parse(encoded);
        if (route.isEmpty()) return false;
        var node = KnowledgeCatalog.NODES.find(route.get().node());
        if (node.isEmpty()
                || !node.get().visibility(context(player)).level().readable()
                || route.get().page() >= node.get().pages().size()) return false;
        var page = node.get().pages().get(route.get().page());
        if (KnowledgeCatalog.PAGES
                .find(page)
                .filter(p -> p.visibleWhen().evaluate(context(player)).level().readable())
                .isEmpty()) return false;
        return StarRecords.ACCESS.readPage(player, page, encoded);
    }

    public static void synchronize(ServerPlayer player) {
        if (player instanceof FakePlayer) return;
        var record = StarRecords.get(player);
        var data = record.save();
        var environment = KnowledgeGateEnvironment.read(player);
        data.putBoolean("GateStageProvider", environment.stagesPresent());
        var stages = new ListTag();
        environment.stages().stream().sorted().forEach(v -> stages.add(StringTag.valueOf(v)));
        data.put("GateStages", stages);
        var tags = new ListTag();
        environment.tags().stream().sorted().forEach(v -> tags.add(StringTag.valueOf(v)));
        data.put("GateTags", tags);
        var previous = LAST.get(player.getUUID());
        long base = VERSIONS.getOrDefault(player.getUUID(), 0L);
        if (previous == null || !data.equals(previous)) {
            long next = base + 1;
            if (previous == null)
                com.mpp.stellaeomphalos.network.SafeDispatch.send(
                        player, new PktStarRecord(session, next, data));
            else {
                var changes = new CompoundTag();
                for (var key : data.getAllKeys())
                    if (!Objects.equals(data.get(key), previous.get(key)))
                        changes.put(key, data.get(key).copy());
                com.mpp.stellaeomphalos.network.SafeDispatch.send(
                        player, new PktStarRecordDelta(session, base, next, changes));
            }
            LAST.put(player.getUUID(), data.copy());
            VERSIONS.put(player.getUUID(), next);
        }
        long epoch = KnowledgeCatalog.PAGES.epoch();
        if (DEFINITION_EPOCH.getOrDefault(player.getUUID(), -1L) != epoch) {
            var definitions = new CompoundTag();
            KnowledgeCatalog.PAGES
                    .pages()
                    .forEach(
                            (id, page) ->
                                    definitions.putString(id.toString(), page.json().toString()));
            var signs = new CompoundTag();
            for (var sign : com.mpp.stellaeomphalos.constellation.sign.SignRegistry.all()) {
                var stars = new ListTag();
                for (var point : sign.stars()) {
                    var star = new CompoundTag();
                    star.putInt("X", point.x());
                    star.putInt("Y", point.y());
                    stars.add(star);
                }
                signs.put(sign.id().toString(), stars);
            }
            definitions.put("Signs", signs);
            var geometry=new CompoundTag();
            for(var sign:com.mpp.stellaeomphalos.constellation.sign.SignRegistry.all()) {
                var entry=new CompoundTag();entry.putInt("Number",com.mpp.stellaeomphalos.constellation.sign.SignRegistry.numericId(sign));
                entry.putInt("Color",sign.renderColor());entry.putBoolean("Major",sign instanceof com.mpp.stellaeomphalos.constellation.sign.MajorSign);
                entry.put("Stars",signs.getList(sign.id().toString(),Tag.TAG_COMPOUND).copy());
                var edges=new ListTag();for(var edge:sign.lines()){
                    var value=new CompoundTag();value.putInt("AX",edge.a().x());value.putInt("AY",edge.a().y());value.putInt("BX",edge.b().x());value.putInt("BY",edge.b().y());edges.add(value);
                }
                entry.put("Edges",edges);geometry.put(sign.id().toString(),entry);
            }
            definitions.put("SignGeometry",geometry);
            var signIds = new CompoundTag();
            for (var sign : com.mpp.stellaeomphalos.constellation.sign.SignRegistry.all())
                signIds.putInt(sign.id().toString(), com.mpp.stellaeomphalos.constellation.sign.SignRegistry.numericId(sign));
            definitions.put("SignIds", signIds);
            var blueprints = new CompoundTag();
            com.mpp.stellaeomphalos.structure.pattern.BlueprintRegistry.all()
                    .forEach(
                            (id, blueprint) -> {
                                var cells = new ListTag();
                                blueprint.blocks().values().stream()
                                        .limit(32768)
                                        .forEach(
                                                cell -> {
                                                    if (cell.rule().example().isAir()) return;
                                                    var tag = new CompoundTag();
                                                    tag.putInt("X", cell.relative().getX());
                                                    tag.putInt("Y", cell.relative().getY());
                                                    tag.putInt("Z", cell.relative().getZ());
                                                    tag.putString(
                                                            "Block",
                                                            net.minecraft.core.registries
                                                                    .BuiltInRegistries.BLOCK
                                                                    .getKey(
                                                                            cell.rule()
                                                                                    .example()
                                                                                    .getBlock())
                                                                    .toString());
                                                    cells.add(tag);
                                                });
                                blueprints.put(id.toString(), cells);
                            });
            definitions.put("Blueprints", blueprints);
            com.mpp.stellaeomphalos.network.SafeDispatch.send(
                    player, new PktCodexDefinitions(epoch, definitions));
            DEFINITION_EPOCH.put(player.getUUID(), epoch);
        }
    }

    public static void gauges(ServerPlayer player) {
        var data = new CompoundTag();
        var entries = new ListTag();
        for (var reading : BoonGaugeRegistry.readAll(player)) {
            var tag = new CompoundTag();
            tag.putString("Name", reading.nameKey());
            tag.putDouble("Value", reading.value());
            tag.putString("Suffix", reading.suffix());
            var b = reading.breakdown();
            tag.putDouble("Base", b.baseline());
            tag.putDouble("Addition", b.addition());
            tag.putDouble("Multiply", b.addedMultiply());
            tag.putDouble("Stacking", b.stackingMultiply());
            tag.putString("Note", b.note());
            entries.add(tag);
        }
        data.put("Readings", entries);
        com.mpp.stellaeomphalos.network.SafeDispatch.send(player, new PktGaugeReadings(data));
    }

    public static void projections(ServerPlayer player) {
        if (!net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getKey(player.getMainHandItem().getItem())
                .getPath()
                .equals("sky_resonator")) return;
        var upgrades = player.getMainHandItem().getOrCreateTag().getList("Upgrades", Tag.TAG_STRING);
        boolean extended = upgrades.contains(StringTag.valueOf("stellaeomphalos:range"));
        boolean precise = upgrades.contains(StringTag.valueOf("stellaeomphalos:precision"));
        int sectors = precise ? 16 : 8;
        var entries = new ListTag();
        for (var entity :
                player
                        .serverLevel()
                        .getEntitiesOfClass(
                                net.minecraft.world.entity.LivingEntity.class,
                                player.getBoundingBox().inflate(extended ? 48 : 24),
                                e -> e != player && e.isAlive())
                        .stream()
                        .limit(64)
                        .toList()) {
            double dx = entity.getX() - player.getX(), dz = entity.getZ() - player.getZ();
            var entry = new CompoundTag();
            entry.putString("Kind", "LIVING");
            entry.putInt(
                    "Direction",
                    Math.floorMod((int) Math.round(Math.atan2(dz, dx) / Math.PI * sectors / 2), sectors));
            entry.putInt("Sectors", sectors);
            entry.putString(
                    "Distance",
                    entity.distanceToSqr(player) < 64
                            ? "NEAR"
                            : entity.distanceToSqr(player) < 256 ? "MID" : "FAR");
            entries.add(entry);
        }
        var data = new CompoundTag();
        data.put("Entries", entries);
        com.mpp.stellaeomphalos.network.SafeDispatch.send(player, new PktReaderProjection(data));
    }
}

package com.mpp.stellaeomphalos.player.boon;

import com.mojang.logging.LogUtils;
import com.mpp.stellaeomphalos.constellation.attribute.BoonValueBridge;
import com.mpp.stellaeomphalos.constellation.boon.BoonFreePointLedger;
import com.mpp.stellaeomphalos.constellation.boon.BoonNode;
import com.mpp.stellaeomphalos.constellation.boon.BoonProgressView;
import com.mpp.stellaeomphalos.constellation.boon.BoonTree;
import com.mpp.stellaeomphalos.constellation.boon.ConnectorBoonNode;
import com.mpp.stellaeomphalos.constellation.boon.CoreRootBoonNode;
import com.mpp.stellaeomphalos.constellation.boon.SocketBoonNode;
import com.mpp.stellaeomphalos.constellation.sign.SignDiscoveryView;
import com.mpp.stellaeomphalos.constellation.sign.SignRegistry;
import com.mpp.stellaeomphalos.network.toClient.PktBoonDelta;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nullable;

/**
 * Per-player boon progress, stored in the player's persistent NBT under the {@code BoonProgress}
 * sub-tag (all keys PascalCase). The server copy is authoritative; the client sees an immutable
 * mirror snapshot installed by the client module. On load, a stale tree version clears the tree and
 * rebuilds the currently attuned sign's root node (AC-2.16); unknown node ids are silently dropped;
 * known signs are folded into seen signs.
 *
 * <p>Skill points: {@code available = level + freePointTokens - (applied - appliedRoots)} —
 * root-like nodes never consume a point (contract §2.4).
 */
public final class BoonProgress implements BoonProgressView, SignDiscoveryView {

    public static final String ROOT_KEY = "BoonProgress";

    private static final Map<UUID, BoonProgress> SERVER_CACHE = new ConcurrentHashMap<>();
    private static volatile Function<Player, BoonProgress> clientView;

    private final boolean mirror;
    private int treeVersion = BoonTree.BOON_TREE_VERSION;
    private final Set<ResourceLocation> applied = new LinkedHashSet<>();
    private final Set<ResourceLocation> sealed = new LinkedHashSet<>();
    private final BoonFreePointLedger freePoints = new BoonFreePointLedger();
    private long exp;
    private final CompoundTag boonData = new CompoundTag();
    private final Set<ResourceLocation> knownSigns = new LinkedHashSet<>();
    private final Set<ResourceLocation> seenSigns = new LinkedHashSet<>();
    @Nullable private ResourceLocation attunedSign;
    // Client-mirror-only synced scalars (the client must not evaluate the config-driven curve).
    private int mirrorLevel = 1;
    private int mirrorPoints;

    private BoonProgress(boolean mirror) {
        this.mirror = mirror;
    }

    // ------------------------------------------------------------------ access

    /**
     * Dual-end read: server players resolve to the authoritative store, client players to the
     * mirror.
     */
    public static BoonProgress get(Player player) {
        if (player instanceof ServerPlayer serverPlayer) return getServer(serverPlayer);
        var view = clientView;
        if (view == null) throw new IllegalStateException("Client boon view is not installed");
        return view.apply(player);
    }

    public static BoonProgress getServer(ServerPlayer player) {
        if (player instanceof net.minecraftforge.common.util.FakePlayer)
            return new BoonProgress(false);
        return SERVER_CACHE.computeIfAbsent(player.getUUID(), id -> loadFrom(player));
    }

    public static void dropCache(UUID playerId) {
        SERVER_CACHE.remove(playerId);
    }

    /** Installed by client/boon during client setup (client imports player, never the reverse). */
    public static void installClientView(Function<Player, BoonProgress> view) {
        clientView = view;
    }

    /** Immutable client snapshot, rebuilt whenever a sync packet arrives. */
    public static BoonProgress clientMirror(
            Set<ResourceLocation> applied,
            Set<ResourceLocation> sealed,
            int availablePoints,
            long exp,
            int level,
            @Nullable ResourceLocation attunedSign) {
        var progress = new BoonProgress(true);
        progress.applied.addAll(applied);
        progress.sealed.addAll(sealed);
        progress.mirrorPoints = availablePoints;
        progress.exp = exp;
        progress.mirrorLevel = level;
        progress.attunedSign = attunedSign;
        progress.treeVersion = BoonTree.BOON_TREE_VERSION;
        return progress;
    }

    // ------------------------------------------------------------------ view contract

    @Override
    public boolean hasNode(ResourceLocation nodeId) {
        return applied.contains(nodeId);
    }

    @Override
    public boolean isSealed(ResourceLocation nodeId) {
        return sealed.contains(nodeId);
    }

    @Override
    public int availablePoints() {
        if (mirror) return mirrorPoints;
        var tree = BoonTree.ready() ? BoonTree.get() : null;
        int spent =
                BoonLedger.spentPoints(
                        id -> {
                            BoonNode node = tree == null ? null : tree.node(id);
                            return node != null && node.type().isRootLike();
                        },
                        applied,
                        sealed);
        return level() + freePoints.count() - spent;
    }

    @Override
    public boolean knowsSign(ResourceLocation signId) {
        return knownSigns.contains(signId);
    }

    @Override
    public Set<ResourceLocation> knownSigns() {
        return Set.copyOf(knownSigns);
    }

    public Set<ResourceLocation> seenSigns() {
        return Set.copyOf(seenSigns);
    }

    public long exp() {
        return exp;
    }

    @Override
    public int level() {
        return mirror ? mirrorLevel : BoonLevelCurve.levelForExp(exp);
    }

    @Nullable
    public ResourceLocation attunedSign() {
        return attunedSign;
    }

    public Set<ResourceLocation> appliedNodes() {
        return Set.copyOf(applied);
    }

    public List<String> freePointTokens() {
        return freePoints.tokens();
    }

    /** Per-node private data slot (gem sockets, behavior history); created lazily. */
    public CompoundTag nodeData(ResourceLocation nodeId) {
        var key = nodeId.toString();
        if (!boonData.contains(key, Tag.TAG_COMPOUND)) boonData.put(key, new CompoundTag());
        return boonData.getCompound(key);
    }

    // ------------------------------------------------------------------ server mutations

    /** Server-authoritative unlock; validates the node's unlock rule and skill point budget. */
    public boolean unlock(ServerPlayer player, ResourceLocation nodeId) {
        if (player instanceof net.minecraftforge.common.util.FakePlayer
                || !com.mpp.stellaeomphalos.player.progress.StarRecords.get(player).valid())
            return false;
        assertServerSide();
        var tree = BoonTree.get();
        var node = tree.node(nodeId);
        if (node == null || applied.contains(nodeId) || sealed.contains(nodeId)) return false;
        if (!node.rule().mayUnlock(player, node, this)) return false;
        applyNode(player, node);
        milestone(player, "boon", nodeId, level());
        if (node instanceof CoreRootBoonNode) freePoints.grantAll(CoreRootBoonNode.BONUS_TOKENS);
        if (node instanceof ConnectorBoonNode connector)
            forceOpenNeighbors(player, tree, connector);
        afterProgressMutation(player);
        BoonEffectDispatcher.markExpDirty(player);
        return true;
    }

    /**
     * Respec: every socketed gem returns to the player (dropped at their feet when the inventory is
     * full, never voided — AC-2.17), then applied/sealed nodes and free-point tokens clear.
     */
    public boolean reset(ServerPlayer player) {
        if (player instanceof net.minecraftforge.common.util.FakePlayer
                || !com.mpp.stellaeomphalos.player.progress.StarRecords.get(player).valid())
            return false;
        assertServerSide();
        var tree = BoonTree.ready() ? BoonTree.get() : null;
        if (tree != null)
            for (var id : List.copyOf(applied)) {
                var node = tree.node(id);
                if (node instanceof SocketBoonNode socket)
                    socket.dropToPlayer(player, nodeData(id));
            }
        applied.clear();
        sealed.clear();
        freePoints.clear();
        afterProgressMutation(player);
        BoonEffectDispatcher.sendTreeSync(player);
        BoonEffectDispatcher.markExpDirty(player);
        return true;
    }

    /**
     * Seals/unseals an applied node; sealed nodes keep their slot but grant nothing and cost
     * nothing.
     */
    public boolean setSealed(ServerPlayer player, ResourceLocation nodeId, boolean seal) {
        if (player instanceof net.minecraftforge.common.util.FakePlayer
                || !com.mpp.stellaeomphalos.player.progress.StarRecords.get(player).valid())
            return false;
        assertServerSide();
        if (!applied.contains(nodeId)) return false;
        boolean changed = seal ? sealed.add(nodeId) : sealed.remove(nodeId);
        if (changed) {
            afterProgressMutation(player);
            BoonEffectDispatcher.sendDelta(
                    player,
                    nodeId,
                    seal ? PktBoonDelta.ACTION_SEAL : PktBoonDelta.ACTION_UNSEAL,
                    new CompoundTag());
            BoonEffectDispatcher.markExpDirty(player);
        }
        return changed;
    }

    /**
     * Experience gain with double throttling: at the cap positive gains drop; one call is capped to
     * 8% of the current band.
     */
    public void grantExp(ServerPlayer player, long amount) {
        if (player instanceof net.minecraftforge.common.util.FakePlayer
                || !com.mpp.stellaeomphalos.player.progress.StarRecords.get(player).valid()) return;
        assertServerSide();
        if (amount <= 0) return;
        long next = cappedGain(exp, amount, BoonLevelCurve.maxLevel());
        if (next == exp) return;
        exp = next;
        persist(player);
        milestone(player, "boon", new ResourceLocation("stellaeomphalos", "rank"), level());
        BoonValueBridge.invalidate(player.getUUID());
        BoonEffectDispatcher.markExpDirty(player);
    }

    /** Death penalty: loses 25% of the experience inside the current level band. */
    public void applyDeathPenalty(ServerPlayer player) {
        if (player instanceof net.minecraftforge.common.util.FakePlayer
                || !com.mpp.stellaeomphalos.player.progress.StarRecords.get(player).valid()) return;
        assertServerSide();
        long next = deathPenaltyExp(exp, BoonLevelCurve.maxLevel());
        if (next == exp) return;
        exp = next;
        persist(player);
        milestone(player, "boon", new ResourceLocation("stellaeomphalos", "rank"), level());
        BoonEffectDispatcher.markExpDirty(player);
    }

    public void discover(ServerPlayer player, ResourceLocation signId) {
        if (player instanceof net.minecraftforge.common.util.FakePlayer
                || !com.mpp.stellaeomphalos.player.progress.StarRecords.get(player).valid()) return;
        assertServerSide();
        if (knownSigns.add(signId)) {
            seenSigns.add(signId);
            persist(player);
            milestone(player, "sign", signId, knownSigns.size());
        }
    }

    public void markSeen(ServerPlayer player, ResourceLocation signId) {
        if (player instanceof net.minecraftforge.common.util.FakePlayer
                || !com.mpp.stellaeomphalos.player.progress.StarRecords.get(player).valid()) return;
        assertServerSide();
        if (seenSigns.add(signId)) persist(player);
    }

    public void setAttuned(ServerPlayer player, @Nullable ResourceLocation signId) {
        if (player instanceof net.minecraftforge.common.util.FakePlayer
                || !com.mpp.stellaeomphalos.player.progress.StarRecords.get(player).valid()) return;
        assertServerSide();
        if (java.util.Objects.equals(attunedSign, signId)) return;
        reset(player); // Return socketed items before replacing a tree.
        attunedSign = signId;
        if (BoonTree.ready()) {
            for (var key : List.copyOf(boonData.getAllKeys())) boonData.remove(key);
            var root = signId == null ? null : BoonTree.get().rootOf(signId);
            if (root != null) applied.add(root.id());
        }
        afterProgressMutation(player);
        BoonEffectDispatcher.sendTreeSync(player);
        if (signId != null) milestone(player, "resonance", signId, 1);
    }

    /**
     * Sockets a gem into an unlocked socket node; refused otherwise (or when the gem is
     * unacceptable).
     */
    public boolean socket(ServerPlayer player, ResourceLocation nodeId, ItemStack stack) {
        if (player instanceof net.minecraftforge.common.util.FakePlayer
                || !com.mpp.stellaeomphalos.player.progress.StarRecords.get(player).valid())
            return false;
        assertServerSide();
        var tree = BoonTree.get();
        if (!(tree.node(nodeId) instanceof SocketBoonNode socket)) return false;
        boolean[] ok = {false};
        boolean accepted =
                socket.setContained(player, this, nodeData(nodeId), stack, () -> ok[0] = true);
        if (accepted && ok[0]) {
            persist(player);
            BoonValueBridge.invalidate(player.getUUID());
            var extra = new CompoundTag();
            var contained = socket.contained(nodeData(nodeId));
            if (!contained.isEmpty()) extra.put("Item", contained.save(new CompoundTag()));
            BoonEffectDispatcher.sendDelta(player, nodeId, PktBoonDelta.ACTION_SOCKET, extra);
        }
        return accepted;
    }

    /** Removes the socketed gem, returning it to the player (drops when the inventory is full). */
    public boolean unsocket(ServerPlayer player, ResourceLocation nodeId) {
        if (player instanceof net.minecraftforge.common.util.FakePlayer
                || !com.mpp.stellaeomphalos.player.progress.StarRecords.get(player).valid())
            return false;
        assertServerSide();
        var tree = BoonTree.get();
        if (!(tree.node(nodeId) instanceof SocketBoonNode socket)) return false;
        var data = nodeData(nodeId);
        if (socket.contained(data).isEmpty()) return false;
        socket.dropToPlayer(player, data);
        persist(player);
        BoonValueBridge.invalidate(player.getUUID());
        BoonEffectDispatcher.sendDelta(
                player, nodeId, PktBoonDelta.ACTION_SOCKET, new CompoundTag());
        return true;
    }

    // ------------------------------------------------------------------ mutation internals

    private static void milestone(
            ServerPlayer player, String kind, ResourceLocation subject, int amount) {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                new com.mpp.stellaeomphalos.core.platform.ProgressMilestoneEvent(
                        player, kind, subject, amount));
    }

    public void mergeKnowledge(
            ServerPlayer player,
            java.util.Set<ResourceLocation> known,
            java.util.Set<ResourceLocation> seen) {
        if (mirror || player instanceof net.minecraftforge.common.util.FakePlayer) return;
        knownSigns.addAll(known);
        seenSigns.addAll(seen);
        seenSigns.addAll(known);
        persist(player);
    }

    private void applyNode(ServerPlayer player, BoonNode node) {
        applied.add(node.id());
        BoonEffectDispatcher.sendDelta(
                player, node.id(), PktBoonDelta.ACTION_UNLOCK, new CompoundTag());
    }

    private void forceOpenNeighbors(
            ServerPlayer player, BoonTree tree, ConnectorBoonNode connector) {
        for (var neighborId : connector.neighbors()) {
            if (applied.contains(neighborId) || sealed.contains(neighborId)) continue;
            var neighbor = tree.node(neighborId);
            if (neighbor == null) continue;
            applyNode(player, neighbor);
            freePoints.grant(connector.tokenFor(neighborId));
        }
    }

    private void afterProgressMutation(ServerPlayer player) {
        persist(player);
        BoonValueBridge.invalidate(player.getUUID());
    }

    private void assertServerSide() {
        if (mirror) throw new IllegalStateException("Client boon mirror is read-only");
    }

    // ------------------------------------------------------------------ persistence

    private static BoonProgress loadFrom(ServerPlayer player) {
        var progress = new BoonProgress(false);
        var root = player.getPersistentData();
        var durable =
                com.mpp.stellaeomphalos.player.progress.StarRecords.get(player).boonSnapshot();
        if (!durable.isEmpty()) root.put(ROOT_KEY, durable);
        if (!root.contains(ROOT_KEY, Tag.TAG_COMPOUND)) return progress;
        var storage =
                loadStorage(
                        root.getCompound(ROOT_KEY),
                        BoonProgress::nodeExists,
                        BoonProgress::signExists);
        storage = migrate(storage, BoonProgress::rootIdOfSign, BoonProgress::nodeExists);
        progress.applyStorage(storage);
        progress.persist(player);
        return progress;
    }

    private void applyStorage(Storage storage) {
        treeVersion = storage.treeVersion();
        applied.addAll(storage.applied());
        sealed.addAll(storage.sealed());
        freePoints.grantAll(storage.freeTokens());
        exp = storage.exp();
        boonData.merge(storage.boonData().copy());
        knownSigns.addAll(storage.knownSigns());
        seenSigns.addAll(storage.seenSigns());
        attunedSign = storage.attunedSign();
    }

    public void persist(ServerPlayer player) {
        if (mirror || player instanceof net.minecraftforge.common.util.FakePlayer) return;
        var saved = saveStorage(new CompoundTag(), snapshot());
        player.getPersistentData().put(ROOT_KEY, saved);
        if (com.mpp.stellaeomphalos.player.progress.StarRecords.get(player).captureBoons(saved))
            com.mpp.stellaeomphalos.player.progress.StarRecords.dirty(player);
    }

    private Storage snapshot() {
        return new Storage(
                treeVersion,
                List.copyOf(applied),
                List.copyOf(sealed),
                freePoints.tokens(),
                exp,
                boonData.copy(),
                List.copyOf(knownSigns),
                List.copyOf(seenSigns),
                attunedSign);
    }

    private static boolean nodeExists(ResourceLocation id) {
        return !BoonTree.ready() || BoonTree.get().node(id) != null;
    }

    private static boolean signExists(ResourceLocation id) {
        return !SignRegistry.frozen() || SignRegistry.byId(id) != null;
    }

    @Nullable
    private static ResourceLocation rootIdOfSign(ResourceLocation signId) {
        if (!BoonTree.ready()) return null;
        var root = BoonTree.get().rootOf(signId);
        return root == null ? null : root.id();
    }

    // ------------------------------------------------------------------ storage codec (pure,
    // unit-testable)

    /** Serializable snapshot of the persistent state; string fields are ResourceLocation ids. */
    public record Storage(
            int treeVersion,
            List<ResourceLocation> applied,
            List<ResourceLocation> sealed,
            List<String> freeTokens,
            long exp,
            CompoundTag boonData,
            List<ResourceLocation> knownSigns,
            List<ResourceLocation> seenSigns,
            @Nullable ResourceLocation attunedSign) {
        public Storage {
            applied = List.copyOf(applied);
            sealed = List.copyOf(sealed);
            freeTokens = List.copyOf(freeTokens);
            knownSigns = List.copyOf(knownSigns);
            seenSigns = List.copyOf(seenSigns);
        }
    }

    /**
     * Reads storage from NBT with compatibility policies: unknown node ids are silently dropped,
     * sealed entries not present in applied drop too, known signs fold into seen signs.
     */
    public static Storage loadStorage(
            CompoundTag tag,
            Predicate<ResourceLocation> nodeExists,
            Predicate<ResourceLocation> signExists) {
        int version =
                tag.contains("BoonTreeVersion", Tag.TAG_INT) ? tag.getInt("BoonTreeVersion") : 0;
        var applied = readIdList(tag, "Applied", nodeExists);
        var sealed = readIdList(tag, "Sealed", id -> nodeExists.test(id) && applied.contains(id));
        var tokens = new ArrayList<String>();
        for (var entry : tag.getList("FreeTokens", Tag.TAG_STRING)) {
            var token = entry.getAsString();
            if (!token.isEmpty() && !tokens.contains(token)) tokens.add(token);
        }
        long exp = Math.max(0L, tag.getLong("BoonExp"));
        var boonData =
                tag.contains("BoonData", Tag.TAG_COMPOUND)
                        ? tag.getCompound("BoonData").copy()
                        : new CompoundTag();
        for (var key : List.copyOf(boonData.getAllKeys())) {
            var parsed = ResourceLocation.tryParse(key);
            if (parsed == null || !nodeExists.test(parsed)) boonData.remove(key);
        }
        var known = readIdList(tag, "KnownSigns", signExists);
        var seen = readIdList(tag, "SeenSigns", signExists);
        for (var id : known) if (!seen.contains(id)) seen.add(id);
        ResourceLocation attuned = null;
        var attunedRaw = tag.getString("AttunedSign");
        if (!attunedRaw.isEmpty()) {
            var candidate = ResourceLocation.tryParse(attunedRaw);
            if (candidate != null && signExists.test(candidate)) attuned = candidate;
        }
        return new Storage(version, applied, sealed, tokens, exp, boonData, known, seen, attuned);
    }

    /**
     * Tree-version migration (AC-2.16, pure part): a stale version clears the whole tree state and
     * rebuilds only the currently attuned sign's root node, when that root still exists.
     */
    public static Storage migrate(
            Storage storage,
            Function<ResourceLocation, ResourceLocation> rootIdOfSign,
            Predicate<ResourceLocation> nodeExists) {
        if (storage.treeVersion() >= BoonTree.BOON_TREE_VERSION) return storage;
        LogUtils.getLogger()
                .info(
                        "Boon tree version {} is stale (current {}); clearing applied nodes",
                        storage.treeVersion(),
                        BoonTree.BOON_TREE_VERSION);
        var rebuilt = new ArrayList<ResourceLocation>();
        if (storage.attunedSign() != null) {
            var rootId = rootIdOfSign.apply(storage.attunedSign());
            if (rootId != null && nodeExists.test(rootId)) rebuilt.add(rootId);
        }
        return new Storage(
                BoonTree.BOON_TREE_VERSION,
                rebuilt,
                List.of(),
                List.of(),
                storage.exp(),
                new CompoundTag(),
                storage.knownSigns(),
                storage.seenSigns(),
                storage.attunedSign());
    }

    public static CompoundTag saveStorage(CompoundTag tag, Storage storage) {
        tag.putInt("BoonTreeVersion", storage.treeVersion());
        tag.put("Applied", writeIdList(storage.applied()));
        tag.put("Sealed", writeIdList(storage.sealed()));
        var tokens = new ListTag();
        storage.freeTokens().forEach(token -> tokens.add(StringTag.valueOf(token)));
        tag.put("FreeTokens", tokens);
        tag.putLong("BoonExp", storage.exp());
        tag.put("BoonData", storage.boonData().copy());
        tag.put("KnownSigns", writeIdList(storage.knownSigns()));
        tag.put("SeenSigns", writeIdList(storage.seenSigns()));
        if (storage.attunedSign() != null)
            tag.putString("AttunedSign", storage.attunedSign().toString());
        return tag;
    }

    private static List<ResourceLocation> readIdList(
            CompoundTag tag, String key, Predicate<ResourceLocation> keep) {
        var result = new ArrayList<ResourceLocation>();
        for (var entry : tag.getList(key, Tag.TAG_STRING)) {
            var id = ResourceLocation.tryParse(entry.getAsString());
            if (id != null && keep.test(id) && !result.contains(id)) result.add(id);
        }
        return result;
    }

    private static ListTag writeIdList(List<ResourceLocation> ids) {
        var list = new ListTag();
        ids.forEach(id -> list.add(StringTag.valueOf(id.toString())));
        return list;
    }

    // ------------------------------------------------------------------ experience math (pure,
    // unit-testable)

    /**
     * Applies the double throttle: full-level gains drop; one call caps at 8% of the current band.
     */
    public static long cappedGain(long exp, long amount, int maxLevel) {
        if (amount <= 0) return exp;
        int level = BoonLevelCurve.levelForExp(exp, maxLevel);
        if (level >= maxLevel) return exp;
        long span = BoonLevelCurve.spanOf(level);
        long cap = Math.max(1L, (long) Math.floor(span * 0.08));
        long gained = Math.min(amount, cap);
        long cap2 = BoonLevelCurve.expForLevel(maxLevel);
        return Math.min(exp + gained, cap2);
    }

    /** Experience after death: keeps the band floor plus 75% of the in-band progress. */
    public static long deathPenaltyExp(long exp, int maxLevel) {
        int level = BoonLevelCurve.levelForExp(exp, maxLevel);
        long floor = BoonLevelCurve.expForLevel(level);
        return floor + (exp - floor) * 3L / 4L;
    }
}

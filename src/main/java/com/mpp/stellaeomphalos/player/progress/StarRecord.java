package com.mpp.stellaeomphalos.player.progress;

import com.mpp.stellaeomphalos.constellation.boon.BoonTree;
import com.mpp.stellaeomphalos.core.platform.StarTier;
import com.mpp.stellaeomphalos.player.boon.BoonProgress;

import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

/** Server-thread confined state; all persistence and sharing return defensive copies. */
public class StarRecord extends AbstractStarRecord {
    public static final int VERSION = 1;
    private StarTier tier = StarTier.DISCOVERY;
    private final Set<ResourceLocation> known = new LinkedHashSet<>(),
            seen = new LinkedHashSet<>(),
            nodes = new LinkedHashSet<>(),
            shards = new LinkedHashSet<>(),
            targets = new LinkedHashSet<>();
    private final Set<String> branches = new LinkedHashSet<>();
    private final Map<ResourceLocation, Integer> pages = new LinkedHashMap<>();
    private String route = "";
    private boolean rewarded;
    private int resonanceCount;
    private CompoundTag boons = new CompoundTag();

    public StarRecord() {
        this(true);
    }

    protected StarRecord(boolean valid) {
        super(valid);
        if (valid) branches.add("DISCOVERY");
    }

    @Override
    public StarTier tier() {
        return tier;
    }

    @Override
    public Set<ResourceLocation> knownSigns() {
        return Set.copyOf(known);
    }

    @Override
    public Set<ResourceLocation> seenSigns() {
        return Set.copyOf(seen);
    }

    @Override
    public Set<ResourceLocation> researchedNodes() {
        return Set.copyOf(nodes);
    }

    @Override
    public Set<ResourceLocation> unlockedShards() {
        return Set.copyOf(shards);
    }

    @Override
    public Set<ResourceLocation> usedTargets() {
        return Set.copyOf(targets);
    }

    @Override
    public Set<String> branches() {
        return Set.copyOf(branches);
    }

    @Override
    public Map<ResourceLocation, Integer> codexSeen() {
        return Map.copyOf(pages);
    }

    @Override
    public String lastRoute() {
        return route;
    }

    @Override
    public boolean firstJoinRewarded() {
        return rewarded;
    }

    public int resonanceCount() {
        return resonanceCount;
    }

    public boolean resonance() {
        if (!valid()) return false;
        resonanceCount = Math.min(1000000, resonanceCount + 1);
        return changed(true);
    }

    public CompoundTag boonSnapshot() {
        return boons.copy();
    }

    public boolean promote(StarTier next) {
        if (!valid() || tier.reaches(next)) return false;
        tier = next;
        return changed(true);
    }

    public boolean branch(String branch) {
        if (!valid()) return false;
        try {
            StarTier.valueOf(branch);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return changed(branches.add(branch));
    }

    public boolean node(ResourceLocation id) {
        return valid() && changed(nodes.add(id));
    }

    public boolean shard(ResourceLocation id) {
        return valid() && changed(shards.add(id));
    }

    public boolean target(ResourceLocation id) {
        return valid() && changed(targets.add(id));
    }

    public boolean discover(ResourceLocation id) {
        if (!valid()) return false;
        boolean modified = known.add(id);
        modified |= seen.add(id);
        return changed(modified);
    }

    public boolean see(ResourceLocation id) {
        return valid() && changed(seen.add(id));
    }

    public boolean reward() {
        if (!valid() || rewarded) return false;
        rewarded = true;
        return changed(true);
    }

    public boolean read(ResourceLocation page, String nextRoute) {
        if (!valid()
                || nextRoute.length() > 512
                || pages.size() >= 4096 && !pages.containsKey(page)) return false;
        int count = pages.getOrDefault(page, 0);
        pages.put(page, Math.min(1000000, count + 1));
        route = nextRoute;
        return changed(true);
    }

    /** BoonProgress is the only live boon owner. This is its durable snapshot. */
    public boolean captureBoons(CompoundTag snapshot) {
        if (!valid()) return false;
        boolean modified = !boons.equals(snapshot);
        boons = snapshot.copy();
        modified |= known.addAll(ids(snapshot, "KnownSigns"));
        modified |= seen.addAll(ids(snapshot, "SeenSigns"));
        modified |= seen.addAll(known);
        return changed(modified);
    }

    public CompoundTag share() {
        var tag = new CompoundTag();
        tag.putString("Tier", tier.name());
        tag.put("KnownSigns", list(known));
        tag.put("SeenSigns", list(seen));
        tag.put("Branches", strings(branches));
        tag.put("ResearchedNodes", list(nodes));
        tag.put("UsedTargets", list(targets));
        return tag;
    }

    /**
     * Explicit allowlist: no experience, free points, shards or reading/reward state is imported.
     */
    public boolean merge(CompoundTag share) {
        if (!valid()) return false;
        boolean modified = known.addAll(ids(share, "KnownSigns"));
        modified |= seen.addAll(ids(share, "SeenSigns"));
        modified |= seen.addAll(known);
        modified |= nodes.addAll(ids(share, "ResearchedNodes"));
        modified |= targets.addAll(ids(share, "UsedTargets"));
        var next = StarTier.parse(share.getString("Tier"));
        if (!tier.reaches(next)) {
            tier = next;
            modified = true;
        }
        for (var entry : share.getList("Branches", Tag.TAG_STRING)) {
            try {
                modified |= branches.add(StarTier.valueOf(entry.getAsString()).name());
            } catch (IllegalArgumentException ignored) {
                /* Unknown extensions are not authority. */
            }
        }
        return changed(modified);
    }

    public CompoundTag save() {
        var tag = share();
        tag.putInt("RecordVersion", VERSION);
        tag.putBoolean("FirstJoinRewarded", rewarded);
        tag.putString("CodexLastRoute", route);
        tag.put("UnlockedShards", list(shards));
        tag.put("Boons", boons.copy());
        var read = new CompoundTag();
        pages.forEach((id, count) -> read.putInt(id.toString(), count));
        tag.put("CodexSeen", read);
        tag.putInt("ResonanceCount", resonanceCount);
        return tag;
    }

    public static StarRecord load(CompoundTag tag) {
        if (tag.getInt("RecordVersion") > VERSION) throw new FutureRecordException();
        var record = new StarRecord();
        record.merge(tag);
        record.shards.addAll(ids(tag, "UnlockedShards"));
        record.resonanceCount = Math.max(0, Math.min(1000000, tag.getInt("ResonanceCount")));
        record.rewarded = !tag.contains("FirstJoinRewarded") || tag.getBoolean("FirstJoinRewarded");
        String route = tag.getString("CodexLastRoute");
        record.route = route.length() <= 512 ? route : "";
        var read = tag.getCompound("CodexSeen");
        for (var key : new TreeSet<>(read.getAllKeys())) {
            var id = ResourceLocation.tryParse(key);
            if (id != null && record.pages.size() < 4096)
                record.pages.put(id, Math.max(0, Math.min(1000000, read.getInt(key))));
        }
        record.boons = tag.getCompound("Boons").copy();
        if (!record.boons.isEmpty()) {
            if (tag.getInt("RecordVersion") < VERSION) record.boons.putInt("BoonTreeVersion", 0);
            var storage =
                    BoonProgress.loadStorage(
                            record.boons,
                            id -> !BoonTree.ready() || BoonTree.get().node(id) != null,
                            id -> true);
            storage =
                    BoonProgress.migrate(
                            storage,
                            id -> {
                                var root = BoonTree.ready() ? BoonTree.get().rootOf(id) : null;
                                return root == null ? null : root.id();
                            },
                            id -> !BoonTree.ready() || BoonTree.get().node(id) != null);
            record.captureBoons(BoonProgress.saveStorage(new CompoundTag(), storage));
        }
        return record;
    }

    public static Set<ResourceLocation> ids(CompoundTag tag, String key) {
        var result = new LinkedHashSet<ResourceLocation>();
        for (var entry : tag.getList(key, Tag.TAG_STRING)) {
            var id = ResourceLocation.tryParse(entry.getAsString());
            if (id != null && result.size() < 4096) result.add(id);
        }
        return result;
    }

    public static ListTag list(Collection<ResourceLocation> ids) {
        return strings(ids.stream().map(ResourceLocation::toString).sorted().toList());
    }

    private static ListTag strings(Collection<String> values) {
        var result = new ListTag();
        values.stream().sorted().forEach(s -> result.add(StringTag.valueOf(s)));
        return result;
    }

    public static final class FutureRecordException extends IllegalArgumentException {
        public FutureRecordException() {
            super("Star record was written by a newer version; preserved unchanged");
        }
    }
}

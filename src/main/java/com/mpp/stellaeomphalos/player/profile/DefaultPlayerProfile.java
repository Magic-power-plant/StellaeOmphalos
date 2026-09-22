package com.mpp.stellaeomphalos.player.profile;

import java.util.LinkedHashSet;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

/** Bounded mutable implementation used by the Forge capability provider. */
public final class DefaultPlayerProfile implements PlayerProfile {
    public static final int CURRENT_SCHEMA = 1;
    private static final int MAX_IDS = 2048;
    private final Set<ResourceLocation> knownSigns = new LinkedHashSet<>();
    private final Set<ResourceLocation> seenSigns = new LinkedHashSet<>();
    private final Set<ResourceLocation> researchGroups = new LinkedHashSet<>();
    private final Set<ResourceLocation> unlockedBoons = new LinkedHashSet<>();
    private long boonExperience;
    private int freeTokens;
    @Nullable private ResourceLocation attunedSign;
    private boolean wasAttuned;

    @Override public int schema() { return CURRENT_SCHEMA; }
    @Override public Set<ResourceLocation> knownSigns() { return Set.copyOf(knownSigns); }
    @Override public Set<ResourceLocation> seenSigns() { return Set.copyOf(seenSigns); }
    @Override public Set<ResourceLocation> researchGroups() { return Set.copyOf(researchGroups); }
    @Override public Set<ResourceLocation> unlockedBoons() { return Set.copyOf(unlockedBoons); }
    @Override public long boonExperience() { return boonExperience; }
    @Override public int freeTokens() { return freeTokens; }
    @Override public ResourceLocation attunedSign() { return attunedSign; }
    @Override public boolean wasAttuned() { return wasAttuned; }

    @Override public void discoverSign(ResourceLocation id) { add(knownSigns, id); add(seenSigns, id); }
    @Override public void markSeen(ResourceLocation id) { add(seenSigns, id); }
    @Override public void setAttuned(ResourceLocation id) { attunedSign = id; wasAttuned = true; }
    @Override public void grantResearch(ResourceLocation id) { add(researchGroups, id); }
    @Override public void grantExperience(long amount) { boonExperience = Math.max(0L, Math.addExact(boonExperience, Math.max(0L, amount))); }
    @Override public void setFreeTokens(int amount) { freeTokens = Math.max(0, Math.min(100000, amount)); }
    @Override public void resetProgress() { clear(); }

    @Override public void load(CompoundTag tag) {
        clear();
        readIds(tag, "known_signs", knownSigns);
        readIds(tag, "seen_signs", seenSigns);
        readIds(tag, "research", researchGroups);
        readIds(tag, "boons", unlockedBoons);
        boonExperience = Math.max(0L, tag.getLong("boon_exp"));
        freeTokens = Math.max(0, Math.min(100000, tag.getInt("free_tokens")));
        String attuned = tag.getString("attuned_sign");
        attunedSign = attuned.isBlank() ? null : ResourceLocation.tryParse(attuned);
        wasAttuned = tag.getBoolean("was_attuned") || attunedSign != null;
        seenSigns.addAll(knownSigns);
    }

    @Override public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putInt("schema", CURRENT_SCHEMA);
        tag.put("known_signs", writeIds(knownSigns));
        tag.put("seen_signs", writeIds(seenSigns));
        tag.put("research", writeIds(researchGroups));
        tag.put("boons", writeIds(unlockedBoons));
        tag.putLong("boon_exp", boonExperience);
        tag.putInt("free_tokens", freeTokens);
        if (attunedSign != null) tag.putString("attuned_sign", attunedSign.toString());
        tag.putBoolean("was_attuned", wasAttuned);
        return tag;
    }

    @Override public void copyFrom(PlayerProfile other) {
        load(other.save());
    }

    private void clear() {
        knownSigns.clear(); seenSigns.clear(); researchGroups.clear(); unlockedBoons.clear();
        boonExperience = 0; freeTokens = 0; attunedSign = null; wasAttuned = false;
    }

    private static void add(Set<ResourceLocation> target, ResourceLocation id) {
        if (id != null && target.size() < MAX_IDS) target.add(id);
    }

    private static void readIds(CompoundTag tag, String key, Set<ResourceLocation> target) {
        if (!tag.contains(key, Tag.TAG_LIST)) return;
        for (var value : tag.getList(key, Tag.TAG_STRING)) {
            var id = ResourceLocation.tryParse(value.getAsString());
            if (id == null || target.size() >= MAX_IDS) break;
            target.add(id);
        }
    }

    private static ListTag writeIds(Set<ResourceLocation> values) {
        var result = new ListTag();
        values.stream().sorted().forEach(id -> result.add(StringTag.valueOf(id.toString())));
        return result;
    }
}

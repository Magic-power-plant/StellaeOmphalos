package com.mpp.stellaeomphalos.constellation.attribute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/** 宝晶词条：可序列化、可跨端携带的属性加成；absolute 固定为 true，序列化只落 Gem/Attr/Mode/Value。 */
public final class GemAffixModifier extends BoonModifier {

    private static final int MAX_TRANSLATION_CACHE = 4096;

    private record TranslationKey(UUID gemId, int chainHash) {}

    private static final Map<TranslationKey, List<BoonModifier>> TRANSLATIONS =
            Collections.synchronizedMap(new LinkedHashMap<>(1024, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<TranslationKey, List<BoonModifier>> eldest) {
                    return size() > MAX_TRANSLATION_CACHE;
                }
            });

    private final UUID gemId;

    public GemAffixModifier(UUID gemId, BoonAttribute attribute, Mode mode, double value) {
        super(attribute, mode, value, true);
        if (gemId == null) throw new IllegalArgumentException("null gemId");
        this.gemId = gemId;
    }

    public UUID gemId() {
        return gemId;
    }

    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putUUID("Gem", gemId);
        tag.putString("Attr", attribute().id().toString());
        tag.putByte("Mode", (byte) mode().ordinal());
        tag.putFloat("Value", (float) value());
        return tag;
    }

    public static GemAffixModifier load(CompoundTag tag) {
        var attribute = BoonAttributeRegistry.require(new ResourceLocation(tag.getString("Attr")));
        return new GemAffixModifier(tag.getUUID("Gem"), attribute, Mode.fromOrdinal(tag.getByte("Mode")), tag.getFloat("Value"));
    }

    /** 转译结果按 (gemId, 链身份) 缓存，LRU 上限 4096；转译结果不落盘，跨端各自重建。 */
    public List<BoonModifier> translateCached(Player player, List<BoonTranslator> translators) {
        var key = new TranslationKey(gemId, chainHash(translators));
        var cached = TRANSLATIONS.get(key);
        if (cached != null) return cached;
        List<BoonModifier> current = List.of((BoonModifier) this);
        for (var translator : translators) {
            var next = new ArrayList<BoonModifier>();
            for (var item : current) next.addAll(translator.translate(player, item, ownerNode()));
            current = next;
        }
        var result = new ArrayList<>(current);
        for (var translator : translators) result.addAll(translator.extraModifiers(player, this));
        var frozen = List.copyOf(result);
        TRANSLATIONS.put(key, frozen);
        return frozen;
    }

    private static int chainHash(List<BoonTranslator> translators) {
        int hash = 1;
        for (var translator : translators) hash = 31 * hash + System.identityHashCode(translator);
        return hash;
    }

    public static int translationCacheSize() {
        return TRANSLATIONS.size();
    }

    public static void clearTranslationCache() {
        TRANSLATIONS.clear();
    }
}

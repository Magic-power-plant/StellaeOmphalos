package com.mpp.stellaeomphalos.content.item.knowledge;

import com.google.gson.*;
import com.mpp.stellaeomphalos.data.loader.FoundationDataProvider;
import com.mpp.stellaeomphalos.knowledge.codex.*;

import net.minecraft.data.*;
import net.minecraftforge.data.event.GatherDataEvent;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/** All distributed Part-5 JSON is generated from the common catalogue, including localization. */
public final class KnowledgeDataProvider implements DataProvider {
    private final PackOutput output;

    public KnowledgeDataProvider(PackOutput output) {
        this.output = output;
        KnowledgeCatalog.initialize();
        KnowledgeCatalog.languages()
                .forEach(
                        (key, values) ->
                                FoundationDataProvider.language(key, values.get(0), values.get(1)));
        for (var row : TRANSLATIONS) FoundationDataProvider.language(row[0], row[1], row[2]);
        for (var kind : com.mpp.stellaeomphalos.player.mantle.MantleRegistry.Kind.values())
            FoundationDataProvider.language(
                    "stellaeomphalos.mantle." + kind.name().toLowerCase(Locale.ROOT),
                    kind.name().toLowerCase(Locale.ROOT),
                    switch (kind) {
                        case GROWTH -> "生长";
                        case GUARDIAN -> "守护";
                        case HERDER -> "牧者";
                        case RETORT -> "报复";
                        case RUIN -> "破坏";
                        case HEARTH -> "炉火";
                        case HOROLOGE -> "时钟";
                        case LANTERN -> "灯火";
                        case PROSPECTOR -> "寻矿";
                        case TIDE -> "潮汐";
                        case ARTIFICER -> "工匠";
                        case SWIFT -> "迅捷";
                    });
    }

    public static void gather(GatherDataEvent event) {
        event.getGenerator()
                .addProvider(
                        event.includeClient() || event.includeServer(),
                        new KnowledgeDataProvider(event.getGenerator().getPackOutput()));
    }

    public CompletableFuture<?> run(CachedOutput cache) {
        var futures = new ArrayList<CompletableFuture<?>>();
        var pages = KnowledgeCatalog.PAGES.defaults();
        for (var node : KnowledgeCatalog.NODES.all())
            for (var id : node.pages())
                if (!pages.containsKey(id))
                    throw new IllegalStateException("Missing required codex page " + id);
        if (!pages.values().stream()
                .map(CodexPage::kind)
                .collect(java.util.stream.Collectors.toSet())
                .containsAll(List.of(PageKind.values())))
            throw new IllegalStateException("Incomplete page kind coverage");
        pages.forEach(
                (id, page) ->
                        write(
                                cache,
                                futures,
                                "data/"
                                        + id.getNamespace()
                                        + "/codex_page/"
                                        + id.getPath()
                                        + ".json",
                                page.json()));
        var icons =
                Map.of(
                        "codex",
                        "book",
                        "lore_shard",
                        "amethyst_shard",
                        "shard_capsule",
                        "ender_pearl",
                        "insight_scroll",
                        "paper");
        icons.forEach(
                (id, icon) -> {
                    var model = new JsonObject();
                    model.addProperty("parent", "minecraft:item/generated");
                    var textures = new JsonObject();
                    textures.addProperty("layer0", "minecraft:item/" + icon);
                    model.add("textures", textures);
                    if (id.equals("lore_shard")) {
                        var override = new JsonObject();
                        var predicate = new JsonObject();
                        predicate.addProperty("stellaeomphalos:gated", 1);
                        override.add("predicate", predicate);
                        override.addProperty("model", "stellaeomphalos:item/lore_shard_unknown");
                        var list = new JsonArray();
                        list.add(override);
                        model.add("overrides", list);
                        var unknown = new JsonObject();
                        unknown.addProperty("parent", "minecraft:item/generated");
                        var unknownTexture = new JsonObject();
                        unknownTexture.addProperty("layer0", "minecraft:item/flint");
                        unknown.add("textures", unknownTexture);
                        write(
                                cache,
                                futures,
                                "assets/stellaeomphalos/models/item/lore_shard_unknown.json",
                                unknown);
                    }
                    write(
                            cache,
                            futures,
                            "assets/stellaeomphalos/models/item/" + id + ".json",
                            model);
                });
        write(
                cache,
                futures,
                "data/stellaeomphalos/recipes/codex.json",
                JsonParser.parseString(
                        """
{"type":"minecraft:crafting_shapeless","ingredients":[{"item":"minecraft:book"},{"item":"minecraft:amethyst_shard"}],"result":{"item":"stellaeomphalos:codex"}}
"""));
        write(
                cache,
                futures,
                "data/stellaeomphalos/recipes/insight_scroll.json",
                JsonParser.parseString(
                        """
{"type":"minecraft:crafting_shaped","pattern":[" P ","PAP"," P "],"key":{"P":{"item":"minecraft:paper"},"A":{"item":"minecraft:amethyst_shard"}},"result":{"item":"stellaeomphalos:insight_scroll"}}
"""));
        write(
                cache,
                futures,
                "data/stellaeomphalos/recipes/shard_capsule.json",
                JsonParser.parseString(
                        """
{"type":"minecraft:crafting_shapeless","ingredients":[{"item":"minecraft:glass_bottle"},{"item":"minecraft:amethyst_shard"},{"item":"minecraft:glowstone_dust"}],"result":{"item":"stellaeomphalos:shard_capsule"}}
"""));
        for (String trigger : List.of("sign", "rite", "altar", "shard", "boon", "resonance")) {
            var advancement = new JsonObject();
            var criteria = new JsonObject();
            var criterion = new JsonObject();
            criterion.addProperty("trigger", "stellaeomphalos:" + trigger + "_milestone");
            var conditions = new JsonObject();
            if (trigger.equals("shard")) conditions.addProperty("count", 1);
            if (trigger.equals("boon")) conditions.addProperty("rank", 2);
            criterion.add("conditions", conditions);
            criteria.add("milestone", criterion);
            advancement.add("criteria", criteria);
            write(
                    cache,
                    futures,
                    "data/stellaeomphalos/advancements/knowledge/" + trigger + ".json",
                    advancement);
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private void write(
            CachedOutput cache, List<CompletableFuture<?>> futures, String path, JsonElement json) {
        futures.add(DataProvider.saveStable(cache, json, output.getOutputFolder().resolve(path)));
    }

    public String getName() {
        return "Stellae Omphalos knowledge";
    }

    private static final String[][] TRANSLATIONS = {
        {"item.stellaeomphalos.codex", "Celestial Codex", "星典"},
        {"item.stellaeomphalos.lore_shard", "Lore Shard", "星识残片"},
        {"item.stellaeomphalos.shard_capsule", "Shard Capsule", "残片封壳"},
        {"item.stellaeomphalos.insight_scroll", "Insight Scroll", "授识卷轴"},
        {"itemGroup.stellaeomphalos.knowledge", "Celestial Knowledge", "星典与知识"},
        {"stellaeomphalos.codex.title", "Celestial Codex", "星典"},
        {"stellaeomphalos.codex.study", "Study", "研习"},
        {"stellaeomphalos.codex.sign_search", "Pattern:", "星图："},
        {"stellaeomphalos.codex.signs", "Signs", "星象"},
        {"stellaeomphalos.codex.boons", "Boons", "星眷"},
        {"stellaeomphalos.codex.lore", "Lore", "残片"},
        {"stellaeomphalos.codex.milestones", "Progress", "进度"},
        {"stellaeomphalos.codex.search", "Search", "搜索"},
        {"stellaeomphalos.codex.back", "Back", "返回"},
        {"stellaeomphalos.codex.forward", "Forward", "前进"},
        {"stellaeomphalos.codex.close", "Close", "关闭"},
        {"stellaeomphalos.codex.missing", "This page is unavailable.", "该典页暂不可用。"},
        {
            "stellaeomphalos.codex.locked",
            "Continue your studies to reveal this entry.",
            "继续研习以揭示此条目。"
        },
        {"stellaeomphalos.codex.unread", "Unread only", "仅未读"},
        {"stellaeomphalos.codex.all", "All chapters", "所有研习支"},
        {"stellaeomphalos.codex.preview", "Project structure", "投影星构"},
        {"stellaeomphalos.codex.gauges", "Attribute readings", "属性读数"},
        {"stellaeomphalos.codex.gauge_adjusted", "Adjusted by attributes or limits", "经属性或上限调整"},
        {"stellaeomphalos.codex.new", "New knowledge recorded", "已收录新知识"},
        {"stellaeomphalos.codex.duplicate", "Already in your archive", "文库中已有此记录"},
        {"stellaeomphalos.codex.rejected", "This shard cannot be revealed yet", "尚无法揭示这枚残片"},
        {
            "stellaeomphalos.codex.zoom",
            "Scroll to zoom; double-click a chapter to focus",
            "滚轮缩放；双击研习支聚焦"
        },
        {
            "stellaeomphalos.record.backup_restored",
            "Celestial records restored from backup.",
            "星录已从备份恢复。"
        },
        {
            "stellaeomphalos.record.records_rebuilt",
            "Celestial records were damaged. Evidence was preserved and a new archive was created.",
            "星录文件损坏；已保留取证副本并建立新档案。"
        },
        {
            "stellaeomphalos.record.save_failed",
            "Celestial progress is still in memory but could not be saved. Contact your server"
                + " operator.",
            "星录仍保存在内存中，但暂未成功写入磁盘。请联系服务器管理员。"
        }
    };
}

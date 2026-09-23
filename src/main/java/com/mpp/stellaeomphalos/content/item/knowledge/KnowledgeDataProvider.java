package com.mpp.stellaeomphalos.content.item.knowledge;

import com.google.gson.*;
import com.mpp.stellaeomphalos.data.loader.FoundationDataProvider;
import com.mpp.stellaeomphalos.knowledge.codex.*;

import net.minecraft.data.*;
import net.minecraftforge.data.event.GatherDataEvent;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/** All distributed 《星典知识与玩家进度》 JSON is generated from the common catalogue, including localization. */
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
                        "lore_capsule",
                        "ender_pearl",
                        "lore_scroll",
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
                "data/stellaeomphalos/recipes/lore_scroll.json",
                JsonParser.parseString(
                        """
{"type":"minecraft:crafting_shaped","pattern":[" P ","PAP"," P "],"key":{"P":{"item":"minecraft:paper"},"A":{"item":"minecraft:amethyst_shard"}},"result":{"item":"stellaeomphalos:lore_scroll"}}
"""));
        write(
                cache,
                futures,
                "data/stellaeomphalos/recipes/lore_capsule.json",
                JsonParser.parseString(
                        """
{"type":"minecraft:crafting_shapeless","ingredients":[{"item":"minecraft:glass_bottle"},{"item":"minecraft:amethyst_shard"},{"item":"minecraft:glowstone_dust"}],"result":{"item":"stellaeomphalos:lore_capsule"}}
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
            var display = new JsonObject();
            var icon = new JsonObject(); icon.addProperty("item", "stellaeomphalos:codex");
            display.add("icon", icon);
            var title = new JsonObject(); title.addProperty("translate", "stellaeomphalos.milestone." + trigger);
            var description = new JsonObject(); description.addProperty("translate", "stellaeomphalos.milestone.description");
            display.add("title", title); display.add("description", description);
            display.addProperty("frame", "task"); display.addProperty("show_toast", true);
            display.addProperty("announce_to_chat", false); display.addProperty("hidden", false);
            if (trigger.equals("sign")) display.addProperty("background", "minecraft:textures/gui/advancements/backgrounds/stone.png");
            else advancement.addProperty("parent", "stellaeomphalos:knowledge/sign");
            advancement.add("display", display);
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
        {"stellaeomphalos.hud.direction.fine.0", "East", "东"},
        {"stellaeomphalos.hud.direction.fine.1", "East-southeast", "东偏南"},
        {"stellaeomphalos.hud.direction.fine.2", "Southeast", "东南"},
        {"stellaeomphalos.hud.direction.fine.3", "South-southeast", "南偏东"},
        {"stellaeomphalos.hud.direction.fine.4", "South", "南"},
        {"stellaeomphalos.hud.direction.fine.5", "South-southwest", "南偏西"},
        {"stellaeomphalos.hud.direction.fine.6", "Southwest", "西南"},
        {"stellaeomphalos.hud.direction.fine.7", "West-southwest", "西偏南"},
        {"stellaeomphalos.hud.direction.fine.8", "West", "西"},
        {"stellaeomphalos.hud.direction.fine.9", "West-northwest", "西偏北"},
        {"stellaeomphalos.hud.direction.fine.10", "Northwest", "西北"},
        {"stellaeomphalos.hud.direction.fine.11", "North-northwest", "北偏西"},
        {"stellaeomphalos.hud.direction.fine.12", "North", "北"},
        {"stellaeomphalos.hud.direction.fine.13", "North-northeast", "北偏东"},
        {"stellaeomphalos.hud.direction.fine.14", "Northeast", "东北"},
        {"stellaeomphalos.hud.direction.fine.15", "East-northeast", "东偏北"},
        {"effect.stellaeomphalos.bleeding", "Bleeding", "流血"},
        {"stellaeomphalos.rite.progress_ticks", "Rite: %s (%s ticks)", "仪式：%s（%s tick）"},
        {"stellaeomphalos.rite.state.scanning", "Scanning", "扫描"},
        {"stellaeomphalos.rite.state.ready", "Ready", "就绪"},
        {"stellaeomphalos.rite.state.interrupted", "Interrupted", "中断"},
        {"stellaeomphalos.rite.state.locked", "Locked", "锁定"},
        {"stellaeomphalos.imprint.title", "Starmap engraving", "星图铭刻"},
        {"stellaeomphalos.imprint.clear", "Clear", "清空"},
        {"stellaeomphalos.imprint.engrave", "Engrave", "铭刻"},
        {"stellaeomphalos.imprint.no_signs", "No known signs", "尚无已知星象"},
        {"stellaeomphalos.rite.hold_mode", "Output mode: %s", "产物模式：%s"},
        {"stellaeomphalos.rite.hold.held", "Hold", "保留"},
        {"stellaeomphalos.rite.hold.auto_inventory", "Deliver nearby", "近距离交付"},
        {"stellaeomphalos.rite.hold.drop_on_full", "Drop when full", "满槽掉落"},
        {"stellaeomphalos.astrolabe.unavailable", "No known target in range", "范围内没有已记录目标"},
        {"stellaeomphalos.astrolabe.exact", "%s: %s", "%s：%s"},
        {"stellaeomphalos.structure.status", "Structure: %s%%, degraded cells: %s", "星构：%s%%，降级单元：%s"},
        {"stellaeomphalos.rite.status", "Rite: %s (%s%%)", "仪式：%s（%s%%）"},
        {"stellaeomphalos.rite.output", "Produced %s x%s (cycle %s)", "已产出 %s ×%s（第 %s 周期）"},
        {"stellaeomphalos.retrogen.status", "Retrogen: %s done, %s queued, %s skipped", "补生成：已完成 %s，待处理 %s，跳过 %s"},
        {"stellaeomphalos.retrogen.done", "Retrogen finished: %s done, %s queued, %s skipped", "补生成完成：已完成 %s，待处理 %s，跳过 %s"},
        {"stellaeomphalos.codex.unlocked", "Unlocked %s codex entries", "已解锁 %s 条星典条目"},
        {"stellaeomphalos.hud.charge", "Charge: %s", "充能：%s"},
        {"stellaeomphalos.hud.mantle", "Mantle guard: %s", "星披守护：%s"},
        {"stellaeomphalos.hud.reader", "Living: %s / %s", "生物：%s／%s"},
        {"stellaeomphalos.paste.unknown", "Unknown blueprint", "未知蓝图"},
        {"stellaeomphalos.paste.result", "Placed %s, skipped %s, failed %s", "已放置 %s，跳过 %s，失败 %s"},
        {"stellaeomphalos.record.future_version", "This archive requires a newer mod version; opened read-only.", "此星档案需要更新版本的模组，已按只读模式打开。"},
        {"stellaeomphalos.milestone.description", "A discovery recorded in your celestial archive.", "一项新发现已记入星档案。"},
        {"stellaeomphalos.milestone.sign", "Sign milestone", "初见星象"},
        {"stellaeomphalos.milestone.rite", "Rite milestone", "仪式运转"},
        {"stellaeomphalos.milestone.altar", "Altar milestone", "星之祭坛进阶"},
        {"stellaeomphalos.milestone.shard", "Shard milestone", "星识初集"},
        {"stellaeomphalos.milestone.boon", "Boon milestone", "星眷成长"},
        {"stellaeomphalos.milestone.resonance", "Resonance milestone", "共鸣之证"},
        {"stellaeomphalos.rite.state.idle", "Idle", "闲置"},
        {"stellaeomphalos.rite.state.warmup", "Warmup", "预热"},
        {"stellaeomphalos.rite.state.running", "Running", "运行"},
        {"stellaeomphalos.rite.state.stalled", "Stalled", "停滞"},
        {"stellaeomphalos.rite.state.finishing", "Finishing", "收尾"},
        {"stellaeomphalos.rite.state.cooldown", "Cooldown", "冷却"},
        {"stellaeomphalos.rite.state.stopped", "Stopped", "停止"},
        {"stellaeomphalos.rite.state.aborted", "Aborted", "中止"},
        {"stellaeomphalos.rite.state.suspended", "Suspended", "暂停"},
        {"stellaeomphalos.rite.state.waiting", "Waiting", "等待"},
        {"stellaeomphalos.rite.state.failed", "Failed", "失败"},
        {"stellaeomphalos.rite.state.complete", "Complete", "完成"},
        {"stellaeomphalos.hud.direction.0", "East", "东"},
        {"stellaeomphalos.hud.direction.1", "Southeast", "东南"},
        {"stellaeomphalos.hud.direction.2", "South", "南"},
        {"stellaeomphalos.hud.direction.3", "Southwest", "西南"},
        {"stellaeomphalos.hud.direction.4", "West", "西"},
        {"stellaeomphalos.hud.direction.5", "Northwest", "西北"},
        {"stellaeomphalos.hud.direction.6", "North", "北"},
        {"stellaeomphalos.hud.direction.7", "Northeast", "东北"},
        {"stellaeomphalos.hud.distance.near", "Near", "近"},
        {"stellaeomphalos.hud.distance.mid", "Mid", "中"},
        {"stellaeomphalos.hud.distance.far", "Far", "远"},
        {"item.stellaeomphalos.codex", "Celestial Codex", "星典"},
        {"item.stellaeomphalos.lore_shard", "Lore Shard", "星识残片"},
        {"item.stellaeomphalos.lore_capsule", "Shard Capsule", "残片封壳"},
        {"item.stellaeomphalos.lore_scroll", "Insight Scroll", "授识卷轴"},
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

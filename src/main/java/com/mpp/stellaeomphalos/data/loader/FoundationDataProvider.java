package com.mpp.stellaeomphalos.data.loader;

import com.google.gson.JsonObject;
import com.mpp.stellaeomphalos.OmphalosConfig;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraftforge.data.event.GatherDataEvent;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public final class FoundationDataProvider implements DataProvider {
    private static final java.util.Map<String, String> CHINESE =
            java.util.Map.ofEntries(
                    java.util.Map.entry("progression.shardPoolMode", "残片公共池模式"),
                    java.util.Map.entry("progression.saveIntervalSeconds", "星录保存间隔（秒）"),
                    java.util.Map.entry("mantle.rechargeTicks", "守护层恢复刻数"),
                    java.util.Map.entry("mantle.maxStacks", "守护层数上限"),
                    java.util.Map.entry("mantle.healing", "星披每刻治疗量"),
                    java.util.Map.entry("mantle.fireReduction", "炉火伤害减免率"),
                    java.util.Map.entry("mantle.retortScale", "报复伤害倍率"),
                    java.util.Map.entry("mantle.retortTicks", "报复有效刻数"),
                    java.util.Map.entry("mantle.stasisCooldown", "时钟星披冷却"),
                    java.util.Map.entry("mantle.activationChance", "星披触发概率"),
                    java.util.Map.entry("mantle.enableClientSideDetection", "启用本地星披侦查"),
                    java.util.Map.entry("codex.focusThreshold", "星图聚焦阈值"),
                    java.util.Map.entry("codex.branchThreshold", "星图研习支阈值"),
                    java.util.Map.entry("codex.cloudFadeThreshold", "星云淡出阈值"),
                    java.util.Map.entry("codex.nodeClickThreshold", "节点点击缩放阈值"),
                    java.util.Map.entry("structure.reverifyInterval", "结构复检间隔"),
                    java.util.Map.entry("ritual.positionBudget", "仪式每刻位置预算"),
                    java.util.Map.entry("ritual.offlineDropThreshold", "离线产物保留时间"),
                    java.util.Map.entry("ritual.progressResetOnStall", "暂停时重置仪式进度"),
                    java.util.Map.entry("ritual.allowTeamCollect", "允许队友收取产物"),
                    java.util.Map.entry("worldgen.retrogenChunks", "每刻补生成区块上限"),
                    java.util.Map.entry("worldgen.retrogenMillis", "每刻补生成毫秒上限"),
                    java.util.Map.entry("worldgen.springPumpRate", "泉脉每刻抽取量"),
                    java.util.Map.entry("worldgen.enabled", "\u542f\u7528\u4e16\u754c\u751f\u6210"),
                    java.util.Map.entry(
                            "worldgen.retrogen", "\u65e7\u533a\u5757\u8865\u751f\u6210"),
                    java.util.Map.entry(
                            "performance.maxSightlineLength",
                            "\u901a\u89c6\u8ddd\u79bb\u4e0a\u9650"),
                    java.util.Map.entry(
                            "performance.maxScannedBlocksPerTick",
                            "\u6bcf\u523b\u65b9\u5757\u626b\u63cf\u9884\u7b97"),
                    java.util.Map.entry(
                            "performance.maxScheduledTasksPerTick",
                            "\u6bcf\u523b\u4efb\u52a1\u9884\u7b97"),
                    java.util.Map.entry(
                            "performance.sightlineStepWidth",
                            "\u901a\u89c6\u91c7\u6837\u6b65\u957f"),
                    java.util.Map.entry(
                            "performance.skyDensityGridSize",
                            "\u5929\u7a7a\u6d53\u5ea6\u7f51\u683c"),
                    java.util.Map.entry(
                            "performance.structureMatchBudgetPerTick",
                            "\u6bcf\u523b\u7ed3\u6784\u5339\u914d\u9884\u7b97"),
                    java.util.Map.entry(
                            "compat.enchantmentAmplification",
                            "\u9644\u9b54\u7b49\u7ea7\u589e\u5e45"),
                    java.util.Map.entry(
                            "gameplay.blockRodHardnessLimit",
                            "\u7f6e\u6362\u6756\u53ef\u66ff\u6362\u65b9\u5757\u786c\u5ea6\u95e8\u69db"),
                    java.util.Map.entry(
                            "amulet.chanceSecondRoll",
                            "\u62a4\u7b26\u7b2c\u4e8c\u6b21\u63b7\u9ab0\u6982\u7387"),
                    java.util.Map.entry(
                            "amulet.chanceThirdRoll",
                            "\u62a4\u7b26\u7b2c\u4e09\u6b21\u63b7\u9ab0\u6982\u7387"),
                    java.util.Map.entry(
                            "amulet.chanceExtraLevel",
                            "\u62a4\u7b26\u989d\u5916\u7b49\u7ea7\u6982\u7387"),
                    java.util.Map.entry(
                            "amulet.chanceGlobalModifier",
                            "\u62a4\u7b26\u5168\u5c40\u4fee\u6b63\u6982\u7387"),
                    java.util.Map.entry(
                            "amulet.chanceNewEnchantment",
                            "\u62a4\u7b26\u65b0\u589e\u9644\u9b54\u6982\u7387"),
                    java.util.Map.entry(
                            "logging.dataTableVerbose",
                            "\u6570\u636e\u8868\u8be6\u7ec6\u65e5\u5fd7"),
                    java.util.Map.entry(
                            "gameplay.lightProximityAltarRecipe",
                            "星之祭坛\u90bb\u8fd1\u5149\u7167\u914d\u65b9"),
                    java.util.Map.entry(
                            "gameplay.lightProximityWandRecipe",
                            "\u7b26\u6587\u6756\u90bb\u8fd1\u5149\u7167\u914d\u65b9"),
                    java.util.Map.entry(
                            "gameplay.giveCodexOnFirstJoin",
                            "\u9996\u6b21\u767b\u5f55\u53d1\u653e\u661f\u5178"),
                    java.util.Map.entry(
                            "gameplay.signPaperRarity", "\u661f\u8c61\u7eb8\u7a00\u6709\u5ea6"),
                    java.util.Map.entry(
                            "gameplay.signPaperQuality", "\u661f\u8c61\u7eb8\u54c1\u8d28"),
                    java.util.Map.entry(
                            "gameplay.mantleChaosResistance",
                            "\u661f\u62ab\u6df7\u6c8c\u6297\u6027"),
                    java.util.Map.entry(
                            "gameplay.weaponOilMultiplier", "\u5251\u6cb9\u4f24\u5bb3\u500d\u7387"),
                    java.util.Map.entry(
                            "gameplay.mobSpawnDenyAll",
                            "\u661f\u57df\u963b\u6b62\u5168\u90e8\u751f\u7269\u751f\u6210"),
                    java.util.Map.entry(
                            "gameplay.wandChainBreakChance",
                            "\u7b26\u6587\u6756\u8fde\u9501\u7834\u574f\u6982\u7387"),
                    java.util.Map.entry(
                            "gameplay.inactivityThresholdMs",
                            "\u6302\u673a\u5224\u5b9a\u65f6\u95f4\uff08\u6beb\u79d2\uff09"),
                    java.util.Map.entry(
                            "progression.maxBoonLevel", "\u661f\u7737\u7b49\u7ea7\u4e0a\u9650"),
                    java.util.Map.entry(
                            "performance.lumenTopologyOpsPerTick",
                            "\u661f\u8f89\u7f51\u7edc\u6bcf\u523b\u62d3\u6251\u64cd\u4f5c\u9884\u7b97"),
                    java.util.Map.entry(
                            "performance.lumenRoutingStepsPerTick",
                            "\u661f\u8f89\u7f51\u7edc\u6bcf\u523b\u5bfb\u8def\u6b65\u6570\u9884\u7b97"),
                    java.util.Map.entry(
                            "performance.lumenTickBudgetMicros",
                            "\u661f\u8f89\u7f51\u7edc\u6bcf\u523b\u65f6\u95f4\u9884\u7b97\uff08\u5fae\u79d2\uff09"),
                    java.util.Map.entry(
                            "performance.lumenMaxNodesPerSection",
                            "\u661f\u8f89\u7f51\u7edc\u5355\u533a\u6bb5\u8282\u70b9\u4e0a\u9650"),
                    java.util.Map.entry(
                            "performance.lumenMaxHops",
                            "\u661f\u8f89\u7f51\u7edc\u94fe\u8def\u8df3\u6570\u4e0a\u9650"),
                    java.util.Map.entry(
                            "performance.lumenProximityOpsPerTick",
                            "\u661f\u8f89\u90bb\u8fd1\u5ea6\u6bcf\u523b\u8ba1\u7b97\u9884\u7b97"),
                    java.util.Map.entry(
                            "gameplay.lumenEnabled", "\u542f\u7528\u661f\u8f89\u7f51\u7edc"),
                    java.util.Map.entry(
                            "gameplay.lumenLossPerHop", "\u661f\u8f89\u9010\u8df3\u635f\u8017"),
                    java.util.Map.entry("gameplay.stasisEnabled", "\u542f\u7528\u505c\u65f6\u57df"),
                    java.util.Map.entry(
                            "gameplay.stasisMaxZones",
                            "\u505c\u65f6\u57df\u6570\u91cf\u4e0a\u9650"),
                    java.util.Map.entry(
                            "gameplay.stasisMaxRadius",
                            "\u505c\u65f6\u57df\u534a\u5f84\u4e0a\u9650"),
                    java.util.Map.entry(
                            "gameplay.stasisMaxFrozenBlockEntities",
                            "\u5355\u505c\u65f6\u57df\u51bb\u7ed3\u65b9\u5757\u5b9e\u4f53\u4e0a\u9650"),
                    java.util.Map.entry(
                            "gameplay.domainEffectsEnabled",
                            "\u542f\u7528\u661f\u57df\u6548\u679c"),
                    java.util.Map.entry(
                            "gameplay.chargeRegenPerTick",
                            "充能每刻回复基数"),
                    java.util.Map.entry(
                            "gameplay.lumenFluidHotInteraction",
                            "星流\u70ed\u53cd\u5e94\u6a21\u5f0f"),
                    java.util.Map.entry(
                            "gameplay.lumenFluidColdResultBlock",
                            "星流\u51b7\u53cd\u5e94\u7ed3\u679c\u65b9\u5757"),
                    java.util.Map.entry(
                            "gameplay.lumenFluidHotResultBlock",
                            "星流\u70ed\u53cd\u5e94\u7ed3\u679c\u65b9\u5757"),
                    java.util.Map.entry(
                            "render.maxEffectDistance", "\u7279\u6548\u6e32\u67d3\u8ddd\u79bb"),
                    java.util.Map.entry("render.gatewayShield", "\u661f\u95e8\u62a4\u76fe"),
                    java.util.Map.entry("render.hudChargeBars", "\u5145\u80fd\u72b6\u6001\u6761"),
                    java.util.Map.entry(
                            "render.skySignOverlay", "\u661f\u8c61\u591c\u7a7a\u53e0\u52a0"),
                    java.util.Map.entry(
                            "sound.masterScale", "\u661f\u67a2\u97f3\u6548\u97f3\u91cf"),
                    java.util.Map.entry(
                            "codex.animations", "\u661f\u5178\u7ffb\u9875\u52a8\u753b"));
    private static final java.util.Map<String, String[]> EXTRA_LANGUAGES =
            new java.util.LinkedHashMap<>();

    public static void language(String key, String english, String chinese) {
        EXTRA_LANGUAGES.put(key, new String[] {english, chinese});
    }

    private static final java.util.Map<String, com.google.gson.JsonObject> EXTRA_SOUNDS =
            new java.util.LinkedHashMap<>();

    public static void sound(String id, com.google.gson.JsonObject value) {
        EXTRA_SOUNDS.put(id, value.deepCopy());
    }

    public static java.util.Map<String, com.google.gson.JsonObject> soundEntries() {
        return java.util.Map.copyOf(EXTRA_SOUNDS);
    }

    private final PackOutput output;

    public FoundationDataProvider(PackOutput output) {
        this.output = output;
    }

    public static void gather(GatherDataEvent event) {
        event.getGenerator()
                .addProvider(
                        event.includeClient() || event.includeServer(),
                        new FoundationDataProvider(event.getGenerator().getPackOutput()));
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        var en = new JsonObject();
        var zh = new JsonObject();
        add(en, zh, "config.title", "Stellae Omphalos", "\u661f\u67a2");
        add(en, zh, "config.client", "Client", "\u5ba2\u6237\u7aef");
        add(en, zh, "config.common", "Common", "\u901a\u7528");
        add(
                en,
                zh,
                "config.server",
                "Server (read only)",
                "\u670d\u52a1\u7aef\uff08\u53ea\u8bfb\uff09");
        add(en, zh, "config.apply", "Apply", "\u5e94\u7528");
        add(
                en,
                zh,
                "command.data_report",
                "Data table issues: %s",
                "\u6570\u636e\u8868\u95ee\u9898\uff1a%s");
        add(
                en,
                zh,
                "command.debug_enabled",
                "Network diagnostics active",
                "\u7f51\u7edc\u8bca\u65ad\u5df2\u542f\u7528");
        add(
                en,
                zh,
                "command.migration_report",
                "Migration %s: %s issues",
                "\u8fc1\u79fb %s\uff1a%s \u9879\u95ee\u9898");
        add(
                en,
                zh,
                "command.boon_unlocked",
                "Boon unlocked: %s",
                "\u5df2\u89e3\u9501\u661f\u7737\uff1a%s");
        add(
                en,
                zh,
                "command.boon_denied",
                "Boon cannot be unlocked",
                "\u65e0\u6cd5\u89e3\u9501\u8be5\u661f\u7737");
        add(
                en,
                zh,
                "command.boon_reset",
                "Boon tree reset",
                "\u661f\u7737\u6811\u5df2\u91cd\u7f6e");
        add(
                en,
                zh,
                "command.boon_discovered",
                "Sign discovered: %s",
                "\u5df2\u53d1\u73b0\u661f\u8c61\uff1a%s");
        add(en, zh, "command.boon_attuned", "Attuned to %s", "\u5df2\u5171\u9e23\uff1a%s");
        add(
                en,
                zh,
                "command.boon_cooldown",
                "Too fast, please wait",
                "\u64cd\u4f5c\u8fc7\u4e8e\u9891\u7e41\uff0c\u8bf7\u7a0d\u5019");
        raw(en, zh, "command.stellaeomphalos.invalid_subcommand", "Unknown subcommand: %s", "未知子命令：%s");
        raw(en, zh, "command.stellaeomphalos.help.help", "Show command help", "显示命令帮助");
        raw(en, zh, "command.stellaeomphalos.help.signs", "List or grant constellation discoveries", "查看或授予星象发现");
        raw(en, zh, "command.stellaeomphalos.help.research", "Inspect or grant research groups", "查看或授予研究组");
        raw(en, zh, "command.stellaeomphalos.help.progress", "Show player progression", "查看玩家进度");
        raw(en, zh, "command.stellaeomphalos.help.reset", "Reset player progression", "重置玩家进度");
        raw(en, zh, "command.stellaeomphalos.help.boons", "Inspect or modify boon progress", "查看或修改星眷进度");
        raw(en, zh, "command.stellaeomphalos.help.attune", "Change the attuned sign", "更改共鸣星象");
        raw(en, zh, "command.stellaeomphalos.help.build", "Place a registered structure blueprint", "放置已注册的结构蓝图");
        raw(en, zh, "command.stellaeomphalos.help.maximize", "Maximize player progression", "最大化玩家进度");
        raw(en, zh, "command.stellaeomphalos.help.network", "Open a network diagnostic window", "开启网络诊断窗口");
        raw(en, zh, "command.stellaeomphalos.help.diagnose", "Inspect a server diagnostic source", "检查服务端诊断源");
        raw(en, zh, "command.stellaeomphalos.help.profile", "Export, import, or inspect a player profile", "导出、导入或查看玩家档案");
        raw(en, zh, "command.stellaeomphalos.help.migrate", "Run a migration verification task", "运行迁移验证任务");
        raw(en, zh, "command.stellaeomphalos.signs.list", "%s knows: %s", "%s 已知星象：%s");
        raw(en, zh, "command.stellaeomphalos.signs.granted", "Granted constellation discoveries to %s: %s", "已向 %s 授予 %s 个星象发现");
        raw(en, zh, "command.stellaeomphalos.unknown_sign", "Unknown constellation: %s", "未知星象：%s");
        raw(en, zh, "command.stellaeomphalos.unknown_research", "Unknown research group: %s", "未知研究组：%s");
        raw(en, zh, "command.stellaeomphalos.research.all", "Granted new research groups to %s: %s (%s)", "已向 %s 授予 %s 个新研究组：%s");
        raw(en, zh, "command.stellaeomphalos.research.granted", "Granted research group to %s: %s", "已向 %s 授予研究组 %s");
        raw(en, zh, "command.stellaeomphalos.progress.next", "%s next level: %s (total experience %s)", "%s 下一级：%s（所需总经验 %s）");
        raw(en, zh, "command.stellaeomphalos.progress.show", "%s: level %s, experience %s, free points %s, known signs %s", "%s：等级 %s，经验 %s，可用点数 %s，已知星象 %s");
        raw(en, zh, "command.stellaeomphalos.reset.done", "Reset progression for %s", "已重置 %s 的进度");
        raw(en, zh, "command.stellaeomphalos.boons.exp", "Granted boon experience to %s", "已向 %s 授予星眷经验");
        raw(en, zh, "command.stellaeomphalos.boons.unlocked", "%s unlocked boons: %s (experience %s)", "%s 已解锁星眷：%s（经验 %s）");
        raw(en, zh, "command.stellaeomphalos.boons.unlocked_count", "Unlocked boons for %s: %s", "已为 %s 解锁 %s 个星眷");
        raw(en, zh, "command.stellaeomphalos.boons.sealed", "Sealed boon %s", "已封印星眷 %s");
        raw(en, zh, "command.stellaeomphalos.attune.done", "%s is attuned to %s", "%s 已与 %s 共鸣");
        raw(en, zh, "command.stellaeomphalos.build.reloaded", "Blueprint snapshot is already current; use /reload to reload data", "蓝图快照已是最新；请使用 /reload 重载数据");
        raw(en, zh, "command.stellaeomphalos.build.no_target", "No registered blueprint: %s", "没有已注册的蓝图：%s");
        raw(en, zh, "command.stellaeomphalos.build.placed", "Placed %s blocks, skipped %s, failed %s", "已放置 %s 个方块，跳过 %s，失败 %s");
        raw(en, zh, "command.stellaeomphalos.maximize.done", "%s maximized at level %s", "%s 已最大化至等级 %s");
        raw(en, zh, "command.stellaeomphalos.network.awaiting", "Network diagnostics started for %s", "已为 %s 开启网络诊断");
        raw(en, zh, "command.stellaeomphalos.network.rejected", "The network diagnostic request was rejected", "网络诊断请求被拒绝");
        raw(en, zh, "command.stellaeomphalos.diagnose.header", "%s diagnostic result: %s", "%s 诊断结果：%s");
        raw(en, zh, "command.stellaeomphalos.profile.exported", "Exported profile for %s", "已导出 %s 的档案");
        raw(en, zh, "command.stellaeomphalos.profile.imported", "Imported profile for %s", "已导入 %s 的档案");
        raw(en, zh, "command.stellaeomphalos.profile.rejected", "The profile file was rejected", "档案文件被拒绝");
        raw(en, zh, "command.stellaeomphalos.profile.force_required", "The snapshot belongs to another server or player; use --force to override", "快照属于其他服务器或玩家；使用 --force 才能覆盖");
        raw(en, zh, "command.stellaeomphalos.profile.status", "%s profile: %s signs, %s research groups, %s boons", "%s 档案：%s 个星象，%s 个研究组，%s 个星眷");
        raw(en, zh, "command.stellaeomphalos.migrate.result", "Migration task accepted: %s", "已接受迁移任务：%s");
        raw(
                en,
                zh,
                "block.stellaeomphalos.molten_lumen",
                "Molten Lumen",
                "星流");
        raw(
                en,
                zh,
                "item.stellaeomphalos.molten_lumen_bucket",
                "Molten Lumen Bucket",
                "星流桶");
        raw(en, zh, "item.stellaeomphalos.geode_shard", "Geode Shard", "片晶碎片");
        // 《方块物品实体完整清单》§6.2.2.1：材料族补全的两个条目。
        raw(en, zh, "item.stellaeomphalos.resonance_gem", "Resonance Gem", "共鸣宝石");
        raw(en, zh, "item.stellaeomphalos.parchment", "Parchment", "羊皮纸");
        raw(en, zh, "block.stellaeomphalos.infused_log", "Infused Log", "\u704c\u6ce8\u539f\u6728");
        raw(
                en,
                zh,
                "block.stellaeomphalos.collector",
                "Lumen Collector",
                "\u661f\u8f89\u6536\u96c6\u5668");
        raw(en, zh, "block.stellaeomphalos.lumen_relay", "Lumen Relay", "\u661f\u8f89\u4e2d\u7ee7");
        raw(
                en,
                zh,
                "block.stellaeomphalos.lumen_battery",
                "Lumen Battery",
                "\u661f\u8f89\u50a8\u80fd\u5668");
        raw(
                en,
                zh,
                "effect.stellaeomphalos.death_protection",
                "Aegis of Stars",
                "\u661f\u4f51\u5e87\u62a4");
        raw(
                en,
                zh,
                "fluid_type.stellaeomphalos.molten_lumen",
                "Molten Lumen",
                "星流");
        raw(
                en,
                zh,
                "death.attack.stellaeomphalos.starlight",
                "%1$s was struck down by starlight",
                "%1$s 被星光击碎");
        raw(
                en,
                zh,
                "death.attack.stellaeomphalos.starlight.player",
                "%1$s was struck down by starlight whilst fighting %2$s",
                "%1$s \u5728\u4e0e %2$s \u6218\u6597\u65f6被星光击碎");
        raw(
                en,
                zh,
                "death.attack.stellaeomphalos.boon_thorns",
                "%1$s was impaled on starlit thorns",
                "%1$s \u88ab\u661f\u8292\u8346\u68d8\u8d2f\u7a7f");
        raw(
                en,
                zh,
                "death.attack.stellaeomphalos.boon_thorns.player",
                "%1$s was impaled on %2$s's starlit thorns",
                "%1$s \u88ab %2$s \u7684\u661f\u8292\u8346\u68d8\u8d2f\u7a7f");
        raw(en, zh, "sign.stellaeomphalos.aevitas", "Aevitas", "生息座");
        raw(en, zh, "sign.stellaeomphalos.armara", "Armara", "遁甲座");
        raw(en, zh, "sign.stellaeomphalos.discidia", "Discidia", "攻烈座");
        raw(en, zh, "sign.stellaeomphalos.evorsio", "Evorsio", "解离座");
        raw(en, zh, "sign.stellaeomphalos.vicio", "Vicio", "虚御座");
        raw(en, zh, "sign.stellaeomphalos.bootes", "Bootes", "牧夫座");
        raw(en, zh, "sign.stellaeomphalos.fornax", "Fornax", "天炉座");
        raw(en, zh, "sign.stellaeomphalos.horologium", "Horologium", "时钟座");
        raw(en, zh, "sign.stellaeomphalos.lucerna", "Lucerna", "圣芒座");
        raw(en, zh, "sign.stellaeomphalos.mineralis", "Mineralis", "矿溢座");
        raw(en, zh, "sign.stellaeomphalos.octans", "Octans", "南极座");
        raw(en, zh, "sign.stellaeomphalos.pelotrio", "Pelotrio", "唤生座");
        raw(en, zh, "sign.stellaeomphalos.gelu", "Gelu", "霜冰座");
        raw(en, zh, "sign.stellaeomphalos.ulteria", "Ulteria", "疏尘座");
        raw(en, zh, "sign.stellaeomphalos.alcara", "Alcara", "振变座");
        raw(en, zh, "sign.stellaeomphalos.vorux", "Vorux", "贪饕座");
        for (var section :
                new OmphalosConfig.Section[] {
                    OmphalosConfig.COMMON, OmphalosConfig.SERVER, OmphalosConfig.CLIENT
                }) {
            section.keys().stream()
                    .sorted()
                    .forEach(
                            key -> {
                                String words =
                                        key.substring(key.indexOf('.') + 1)
                                                .replaceAll("([a-z])([A-Z])", "$1 $2");
                                String label =
                                        Character.toUpperCase(words.charAt(0)) + words.substring(1);
                                add(
                                        en,
                                        zh,
                                        "config." + key,
                                        label,
                                        key.equals("crafting.disabledFamilies")
                                                ? "禁用的制作配方族"
                                                : CHINESE.getOrDefault(key, label));
                            });
        }
        raw(en, zh, "stellaeomphalos.visual.observe", "Observe the sky", "观测星空");
        raw(en, zh, "stellaeomphalos.visual.next_sign", "Next constellation", "下一星象");
        raw(en, zh, "stellaeomphalos.visual.no_signs", "No visible constellations", "当前没有可观测的星象");
        raw(en, zh, "stellaeomphalos.visual.pattern_matched", "Pattern matched", "星图连线正确");
        raw(en, zh, "stellaeomphalos.visual.sign_list", "Constellation atlas", "星象图鉴");
        raw(en, zh, "stellaeomphalos.visual.sign_detail", "Constellation detail", "星象详情");
        raw(en, zh, "stellaeomphalos.visual.observatory", "Observatory", "观星台");
        raw(en, zh, "stellaeomphalos.visual.telescope", "Fixed telescope", "方块望远镜");
        raw(en, zh, "stellaeomphalos.visual.hand_telescope", "Hand telescope", "手持望远镜");
        raw(en, zh, "stellaeomphalos.visual.sign_scroll", "Constellation scroll", "星象卷轴");
        raw(en, zh, "stellaeomphalos.visual.lore_scroll", "Lore scroll", "星识卷轴");
        raw(en, zh, "subtitles.stellaeomphalos.altar_craft_complete", "Altar craft complete", "星坛合成完成");
        raw(en, zh, "subtitles.stellaeomphalos.altar_craft_loop", "Altar craft loop", "星坛聚拢星辉");
        raw(en, zh, "subtitles.stellaeomphalos.astrolabe_found", "Astrolabe found", "星盘找到目标");
        raw(en, zh, "subtitles.stellaeomphalos.astrolabe_ping", "Astrolabe ping", "星盘标记目标");
        raw(en, zh, "subtitles.stellaeomphalos.attunement", "Attunement", "共鸣回响");
        raw(en, zh, "subtitles.stellaeomphalos.book_close", "Book close", "书本合拢");
        raw(en, zh, "subtitles.stellaeomphalos.book_flip", "Book flip", "书页翻动");
        raw(en, zh, "subtitles.stellaeomphalos.boon_seal_break", "Boon seal break", "星眷封印解除");
        raw(en, zh, "subtitles.stellaeomphalos.boon_unlock", "Boon unlock", "星眷节点解锁");
        raw(en, zh, "subtitles.stellaeomphalos.clip_switch", "Clip switch", "透镜卡扣切换");
        raw(en, zh, "subtitles.stellaeomphalos.codex_close", "Codex close", "星典合拢");
        raw(en, zh, "subtitles.stellaeomphalos.codex_open", "Codex open", "星典展开");
        raw(en, zh, "subtitles.stellaeomphalos.codex_page_turn", "Codex page turn", "星典翻页");
        raw(en, zh, "subtitles.stellaeomphalos.codex_search_type", "Codex search type", "星典检索");
        raw(en, zh, "subtitles.stellaeomphalos.craft_finish", "Craft finish", "合成完成");
        raw(en, zh, "subtitles.stellaeomphalos.craft_loop", "Craft loop", "合成持续");
        raw(en, zh, "subtitles.stellaeomphalos.crystal_fracture", "Crystal fracture", "水晶碎裂");
        raw(en, zh, "subtitles.stellaeomphalos.crystal_grow", "Crystal grow", "水晶生长");
        raw(en, zh, "subtitles.stellaeomphalos.gateway_charge", "Gateway charge", "星门聚焦");
        raw(en, zh, "subtitles.stellaeomphalos.gateway_teleport", "Gateway teleport", "星门传送");
        raw(en, zh, "subtitles.stellaeomphalos.grindstone_complete", "Grindstone complete", "研磨完成");
        raw(en, zh, "subtitles.stellaeomphalos.grindstone_grind_loop", "Grindstone grind loop", "砂轮研磨");
        raw(en, zh, "subtitles.stellaeomphalos.infuser_craft_loop", "Infuser craft loop", "星辉注入");
        raw(en, zh, "subtitles.stellaeomphalos.infusion_bubble", "Infusion bubble", "注魔气泡");
        raw(en, zh, "subtitles.stellaeomphalos.lumen_collect_loop", "Lumen collect loop", "水晶汇集星光");
        raw(en, zh, "subtitles.stellaeomphalos.mantle_activate", "Mantle activate", "星披激活");
        raw(en, zh, "subtitles.stellaeomphalos.meteor_fall", "Meteor fall", "流星划过");
        raw(en, zh, "subtitles.stellaeomphalos.meteor_impact", "Meteor impact", "流星落地");
        raw(en, zh, "subtitles.stellaeomphalos.quern_spin", "Quern spin", "砂轮旋转");
        raw(en, zh, "subtitles.stellaeomphalos.relay_link", "Relay link", "星辉线路连接");
        raw(en, zh, "subtitles.stellaeomphalos.relay_unlink", "Relay unlink", "星辉线路断开");
        raw(en, zh, "subtitles.stellaeomphalos.resonance_attune", "Resonance attune", "共鸣完成");
        raw(en, zh, "subtitles.stellaeomphalos.rite_output", "Rite output", "星仪产物生成");
        raw(en, zh, "subtitles.stellaeomphalos.ritual_end", "Ritual end", "星仪结束");
        raw(en, zh, "subtitles.stellaeomphalos.ritual_fail", "Ritual fail", "星仪中断");
        raw(en, zh, "subtitles.stellaeomphalos.ritual_loop", "Ritual loop", "星仪运转");
        raw(en, zh, "subtitles.stellaeomphalos.ritual_start", "Ritual start", "星仪启动");
        raw(en, zh, "subtitles.stellaeomphalos.shard_reveal", "Shard reveal", "星识残片揭示");
        raw(en, zh, "subtitles.stellaeomphalos.sign_discover", "Sign discover", "星象被发现");
        raw(en, zh, "subtitles.stellaeomphalos.spring_draw", "Spring draw", "泉眼抽取");
        raw(en, zh, "subtitles.stellaeomphalos.structure_break", "Structure break", "星构破坏");
        raw(en, zh, "subtitles.stellaeomphalos.structure_formed", "Structure formed", "星构成形");
        raw(en, zh, "subtitles.stellaeomphalos.view_sequence_whoosh", "View sequence whoosh", "视角掠过");
        raw(en, zh, "subtitles.stellaeomphalos.wand_augment_switch", "Wand augment switch", "符文杖切换增幅");
        raw(en, zh, "subtitles.stellaeomphalos.well_liquid_loop", "Well liquid loop", "星辉井涌流");
        raw(en, zh, "stellaeomphalos.visual.waiting", "Waiting for discovery confirmation", "等待星象发现确认");
        raw(en, zh, "stellaeomphalos.visual.discovered", "Constellation discovered", "已发现星象");
        raw(en, zh, "stellaeomphalos.visual.sky_unavailable", "A clear night sky and an upward view are required", "需要晴朗夜空；手持镜需抬头观测");
        raw(en, zh, "stellaeomphalos.config.restart_required", "Reopen the world to apply these settings", "重新进入世界以应用这些设置");
        raw(en, zh, "stellaeomphalos.visual.chart_burned", "The parchment burned; the glass is unchanged", "羊皮纸已烧毁，玻璃仍可继续雕刻");
        raw(en, zh, "stellaeomphalos.visual.chart_complete", "The star glass has been engraved", "星图玻璃已完成雕刻");
        raw(en, zh, "stellaeomphalos.visual.astrolabe", "Astrolabe targets", "星盘目标");
        raw(en, zh, "stellaeomphalos.config.effects.enabled", "enabled", "特效总开关");
        raw(en, zh, "stellaeomphalos.config.effects.budget", "budget", "特效轨数量上限");
        raw(en, zh, "stellaeomphalos.config.effects.renderDistance", "renderDistance", "特效视距");
        raw(en, zh, "stellaeomphalos.config.effects.beams", "beams", "星辉光束");
        raw(en, zh, "stellaeomphalos.config.effects.arcs", "arcs", "闪电特效");
        raw(en, zh, "stellaeomphalos.config.effects.composite", "composite", "复合球壳特效");
        raw(en, zh, "stellaeomphalos.config.effects.orbits", "orbits", "环绕与时停光环");
        raw(en, zh, "stellaeomphalos.config.particles.budget", "budget", "粒子数量上限");
        raw(en, zh, "stellaeomphalos.config.particles.quality", "quality", "粒子画质");
        raw(en, zh, "stellaeomphalos.config.particles.effectDistance", "effectDistance", "粒子视距");
        raw(en, zh, "stellaeomphalos.config.render.beRendererDistance", "beRendererDistance", "机器渲染视距");
        raw(en, zh, "stellaeomphalos.config.render.vanillaShaders", "vanillaShaders", "使用原版着色器");
        raw(en, zh, "stellaeomphalos.config.sky.overlay", "overlay", "星空模式");
        raw(en, zh, "stellaeomphalos.config.sky.respectForeign", "respectForeign", "兼容其他天空渲染");
        raw(en, zh, "stellaeomphalos.config.sky.starLayers", "starLayers", "星点层数");
        raw(en, zh, "stellaeomphalos.config.sky.meteorTrails", "meteorTrails", "流星拖尾");
        raw(en, zh, "stellaeomphalos.config.view.sequences", "sequences", "允许视角演出");
        raw(en, zh, "stellaeomphalos.config.palette.runtimeExtraction", "runtimeExtraction", "动态提取物品主色");
        raw(en, zh, "stellaeomphalos.config.debug.showEffectStats", "showEffectStats", "显示特效统计");
        raw(en, zh, "structure.stellaeomphalos.ancient_shrine", "Ancient Shrine", "远古神殿");
        raw(en, zh, "structure.stellaeomphalos.small_shrine", "Small Shrine", "小型神龛");
        raw(en, zh, "structure.stellaeomphalos.small_ruin", "Small Ruin", "小型遗迹");
        raw(en, zh, "structure.stellaeomphalos.desert_shrine", "Desert Shrine", "沙漠神龛");
        raw(en, zh, "structure.stellaeomphalos.treasure_shrine", "Treasure Shrine", "藏宝神龛");
        raw(en, zh, "structure.stellaeomphalos.lumen_spring", "Lumen Spring", "星辉泉眼");
        raw(en, zh, "stellaeomphalos.visual.gateway", "Celestial gateway", "天体星门");
        raw(en, zh, "stellaeomphalos.visual.gateway_target", "Gateway at %s", "星门 %s");
        raw(en, zh, "stellaeomphalos.visual.gateway_charge", "Focus and travel", "聚焦并传送");
        raw(en, zh, "stellaeomphalos.visual.gateway_empty", "No other known gateways in this dimension", "当前维度尚无其他已登记星门");
        raw(en, zh, "stellaeomphalos.config.view.captureTargets", "Capture gateway views", "保存星门景观预览");
        raw(en,zh,"stellaeomphalos.visual.pattern_failed","The pattern was not accepted; try again","星图未通过，请重新连线");
        raw(en,zh,"stellaeomphalos.config.render.staticMeshes","Cache crystal meshes","缓存水晶静态网格");
        EXTRA_LANGUAGES.forEach((key, pair) -> raw(en, zh, key, pair[0], pair[1]));
        var futures = new ArrayList<CompletableFuture<?>>();
        futures.add(
                DataProvider.saveStable(
                        cache,
                        en,
                        output.getOutputFolder()
                                .resolve("assets/stellaeomphalos/lang/en_us.json")));
        futures.add(
                DataProvider.saveStable(
                        cache,
                        zh,
                        output.getOutputFolder()
                                .resolve("assets/stellaeomphalos/lang/zh_cn.json")));
        var example = new JsonObject();
        example.addProperty("schema_version", 1);
        example.addProperty("enabled", true);
        example.add("targets", new com.google.gson.JsonArray());
        example.add("values", new com.google.gson.JsonArray());
        futures.add(
                DataProvider.saveStable(
                        cache,
                        example,
                        output.getOutputFolder()
                                .resolve("data/stellaeomphalos/compat_blacklist/_example.json")));
        try {
            var structure = new net.minecraft.nbt.CompoundTag();
            structure.putInt("DataVersion", 3465);
            var size = new net.minecraft.nbt.ListTag();
            for (int i = 0; i < 3; i++) size.add(net.minecraft.nbt.IntTag.valueOf(1));
            structure.put("size", size);
            structure.put("blocks", new net.minecraft.nbt.ListTag());
            structure.put("entities", new net.minecraft.nbt.ListTag());
            var palette = new net.minecraft.nbt.ListTag();
            var air = new net.minecraft.nbt.CompoundTag();
            air.putString("Name", "minecraft:air");
            palette.add(air);
            structure.put("palette", palette);
            var bytes = new java.io.ByteArrayOutputStream();
            net.minecraft.nbt.NbtIo.writeCompressed(structure, bytes);
            byte[] content = bytes.toByteArray();
            cache.writeIfNeeded(
                    output.getOutputFolder()
                            .resolve("data/stellaeomphalos/structures/foundation_empty.nbt"),
                    content,
                    com.google.common.hash.Hashing.sha1().hashBytes(content));
        } catch (java.io.IOException exception) {
            return CompletableFuture.failedFuture(exception);
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private static void raw(
            JsonObject en, JsonObject zh, String key, String english, String chinese) {
        en.addProperty(key, english);
        zh.addProperty(key, chinese);
    }

    private static void add(
            JsonObject en, JsonObject zh, String key, String english, String chinese) {
        en.addProperty("stellaeomphalos." + key, english);
        zh.addProperty("stellaeomphalos." + key, chinese);
    }

    @Override
    public String getName() {
        return "Stellae Omphalos foundation resources";
    }
}

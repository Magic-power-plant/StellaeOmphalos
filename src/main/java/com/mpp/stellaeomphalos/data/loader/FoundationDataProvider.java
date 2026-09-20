package com.mpp.stellaeomphalos.data.loader;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraftforge.data.event.GatherDataEvent;
import com.mpp.stellaeomphalos.OmphalosConfig;

public final class FoundationDataProvider implements DataProvider {
    private static final java.util.Map<String, String> CHINESE = java.util.Map.ofEntries(
            java.util.Map.entry("worldgen.enabled", "\u542f\u7528\u4e16\u754c\u751f\u6210"),
            java.util.Map.entry("worldgen.retrogen", "\u65e7\u533a\u5757\u8865\u751f\u6210"),
            java.util.Map.entry("performance.maxSightlineLength", "\u901a\u89c6\u8ddd\u79bb\u4e0a\u9650"),
            java.util.Map.entry("performance.maxScannedBlocksPerTick", "\u6bcf\u523b\u65b9\u5757\u626b\u63cf\u9884\u7b97"),
            java.util.Map.entry("performance.maxScheduledTasksPerTick", "\u6bcf\u523b\u4efb\u52a1\u9884\u7b97"),
            java.util.Map.entry("performance.sightlineStepWidth", "\u901a\u89c6\u91c7\u6837\u6b65\u957f"),
            java.util.Map.entry("performance.skyDensityGridSize", "\u5929\u7a7a\u6d53\u5ea6\u7f51\u683c"),
            java.util.Map.entry("performance.structureMatchBudgetPerTick", "\u6bcf\u523b\u7ed3\u6784\u5339\u914d\u9884\u7b97"),
            java.util.Map.entry("compat.enchantmentAmplification", "\u9644\u9b54\u7b49\u7ea7\u589e\u5e45"),
            java.util.Map.entry("logging.dataTableVerbose", "\u6570\u636e\u8868\u8be6\u7ec6\u65e5\u5fd7"),
            java.util.Map.entry("gameplay.lightProximityAltarRecipe", "\u661f\u575b\u90bb\u8fd1\u5149\u7167\u914d\u65b9"),
            java.util.Map.entry("gameplay.lightProximityWandRecipe", "\u7b26\u6587\u6756\u90bb\u8fd1\u5149\u7167\u914d\u65b9"),
            java.util.Map.entry("gameplay.giveCodexOnFirstJoin", "\u9996\u6b21\u767b\u5f55\u53d1\u653e\u661f\u5178"),
            java.util.Map.entry("gameplay.signPaperRarity", "\u661f\u8c61\u7eb8\u7a00\u6709\u5ea6"),
            java.util.Map.entry("gameplay.signPaperQuality", "\u661f\u8c61\u7eb8\u54c1\u8d28"),
            java.util.Map.entry("gameplay.mantleChaosResistance", "\u661f\u62ab\u6df7\u6c8c\u6297\u6027"),
            java.util.Map.entry("gameplay.weaponOilMultiplier", "\u5251\u6cb9\u4f24\u5bb3\u500d\u7387"),
            java.util.Map.entry("gameplay.mobSpawnDenyAll", "\u661f\u57df\u963b\u6b62\u5168\u90e8\u751f\u7269\u751f\u6210"),
            java.util.Map.entry("gameplay.wandChainBreakChance", "\u7b26\u6587\u6756\u8fde\u9501\u7834\u574f\u6982\u7387"),
            java.util.Map.entry("gameplay.inactivityThresholdMs", "\u6302\u673a\u5224\u5b9a\u65f6\u95f4\uff08\u6beb\u79d2\uff09"),
            java.util.Map.entry("progression.maxBoonLevel", "\u661f\u7737\u7b49\u7ea7\u4e0a\u9650"),
            java.util.Map.entry("performance.lumenTopologyOpsPerTick", "\u661f\u8f89\u7f51\u7edc\u6bcf\u523b\u62d3\u6251\u64cd\u4f5c\u9884\u7b97"),
            java.util.Map.entry("performance.lumenRoutingStepsPerTick", "\u661f\u8f89\u7f51\u7edc\u6bcf\u523b\u5bfb\u8def\u6b65\u6570\u9884\u7b97"),
            java.util.Map.entry("performance.lumenTickBudgetMicros", "\u661f\u8f89\u7f51\u7edc\u6bcf\u523b\u65f6\u95f4\u9884\u7b97\uff08\u5fae\u79d2\uff09"),
            java.util.Map.entry("performance.lumenMaxNodesPerSection", "\u661f\u8f89\u7f51\u7edc\u5355\u533a\u6bb5\u8282\u70b9\u4e0a\u9650"),
            java.util.Map.entry("performance.lumenMaxHops", "\u661f\u8f89\u7f51\u7edc\u94fe\u8def\u8df3\u6570\u4e0a\u9650"),
            java.util.Map.entry("performance.lumenProximityOpsPerTick", "\u661f\u8f89\u90bb\u8fd1\u5ea6\u6bcf\u523b\u8ba1\u7b97\u9884\u7b97"),
            java.util.Map.entry("gameplay.lumenEnabled", "\u542f\u7528\u661f\u8f89\u7f51\u7edc"),
            java.util.Map.entry("gameplay.lumenLossPerHop", "\u661f\u8f89\u9010\u8df3\u635f\u8017"),
            java.util.Map.entry("gameplay.stasisEnabled", "\u542f\u7528\u505c\u65f6\u57df"),
            java.util.Map.entry("gameplay.stasisMaxZones", "\u505c\u65f6\u57df\u6570\u91cf\u4e0a\u9650"),
            java.util.Map.entry("gameplay.stasisMaxRadius", "\u505c\u65f6\u57df\u534a\u5f84\u4e0a\u9650"),
            java.util.Map.entry("gameplay.stasisMaxFrozenBlockEntities", "\u5355\u505c\u65f6\u57df\u51bb\u7ed3\u65b9\u5757\u5b9e\u4f53\u4e0a\u9650"),
            java.util.Map.entry("gameplay.domainEffectsEnabled", "\u542f\u7528\u661f\u57df\u6548\u679c"),
            java.util.Map.entry("gameplay.chargeRegenPerTick", "\u661f\u80fd\u6bcf\u523b\u56de\u590d\u57fa\u6570"),
            java.util.Map.entry("gameplay.lumenFluidHotInteraction", "\u7194\u878d\u661f\u8f89\u70ed\u53cd\u5e94\u6a21\u5f0f"),
            java.util.Map.entry("gameplay.lumenFluidColdResultBlock", "\u7194\u878d\u661f\u8f89\u51b7\u53cd\u5e94\u7ed3\u679c\u65b9\u5757"),
            java.util.Map.entry("gameplay.lumenFluidHotResultBlock", "\u7194\u878d\u661f\u8f89\u70ed\u53cd\u5e94\u7ed3\u679c\u65b9\u5757"),
            java.util.Map.entry("render.maxEffectDistance", "\u7279\u6548\u6e32\u67d3\u8ddd\u79bb"),
            java.util.Map.entry("render.gatewayShield", "\u661f\u95e8\u62a4\u76fe"),
            java.util.Map.entry("render.hudChargeBars", "\u5145\u80fd\u72b6\u6001\u6761"),
            java.util.Map.entry("render.skySignOverlay", "\u661f\u8c61\u591c\u7a7a\u53e0\u52a0"),
            java.util.Map.entry("sound.masterScale", "\u661f\u67a2\u97f3\u6548\u97f3\u91cf"),
            java.util.Map.entry("codex.animations", "\u661f\u5178\u7ffb\u9875\u52a8\u753b")
    );
    private final PackOutput output;
    public FoundationDataProvider(PackOutput output) { this.output = output; }
    public static void gather(GatherDataEvent event) {
        event.getGenerator().addProvider(event.includeClient() || event.includeServer(), new FoundationDataProvider(event.getGenerator().getPackOutput()));
    }
    @Override public CompletableFuture<?> run(CachedOutput cache) {
        var en = new JsonObject(); var zh = new JsonObject();
        add(en, zh, "config.title", "Stellae Omphalos", "\u661f\u67a2");
        add(en, zh, "config.client", "Client", "\u5ba2\u6237\u7aef");
        add(en, zh, "config.common", "Common", "\u901a\u7528");
        add(en, zh, "config.server", "Server (read only)", "\u670d\u52a1\u7aef\uff08\u53ea\u8bfb\uff09");
        add(en, zh, "config.apply", "Apply", "\u5e94\u7528");
        add(en, zh, "command.data_report", "Data table issues: %s", "\u6570\u636e\u8868\u95ee\u9898\uff1a%s");
        add(en, zh, "command.debug_enabled", "Network diagnostics active", "\u7f51\u7edc\u8bca\u65ad\u5df2\u542f\u7528");
        add(en, zh, "command.migration_report", "Migration %s: %s issues", "\u8fc1\u79fb %s\uff1a%s \u9879\u95ee\u9898");
        add(en, zh, "command.boon_unlocked", "Boon unlocked: %s", "\u5df2\u89e3\u9501\u661f\u7737\uff1a%s");
        add(en, zh, "command.boon_denied", "Boon cannot be unlocked", "\u65e0\u6cd5\u89e3\u9501\u8be5\u661f\u7737");
        add(en, zh, "command.boon_reset", "Boon tree reset", "\u661f\u7737\u6811\u5df2\u91cd\u7f6e");
        add(en, zh, "command.boon_discovered", "Sign discovered: %s", "\u5df2\u53d1\u73b0\u661f\u8c61\uff1a%s");
        add(en, zh, "command.boon_attuned", "Attuned to %s", "\u5df2\u5171\u9e23\uff1a%s");
        add(en, zh, "command.boon_cooldown", "Too fast, please wait", "\u64cd\u4f5c\u8fc7\u4e8e\u9891\u7e41\uff0c\u8bf7\u7a0d\u5019");
        raw(en, zh, "block.stellaeomphalos.molten_lumen", "Molten Lumen", "\u7194\u878d\u661f\u8f89");
        raw(en, zh, "item.stellaeomphalos.molten_lumen_bucket", "Molten Lumen Bucket", "\u7194\u878d\u661f\u8f89\u6876");
        raw(en, zh, "item.stellaeomphalos.geode_shard", "Geode Shard", "\u6676\u7c07\u788e\u7802");
        raw(en, zh, "block.stellaeomphalos.infused_log", "Infused Log", "\u704c\u6ce8\u539f\u6728");
        raw(en, zh, "block.stellaeomphalos.lumen_collector", "Lumen Collector", "\u661f\u8f89\u6536\u96c6\u5668");
        raw(en, zh, "block.stellaeomphalos.lumen_relay", "Lumen Relay", "\u661f\u8f89\u4e2d\u7ee7");
        raw(en, zh, "block.stellaeomphalos.lumen_battery", "Lumen Battery", "\u661f\u8f89\u50a8\u80fd\u5668");
        raw(en, zh, "effect.stellaeomphalos.death_protection", "Aegis of Stars", "\u661f\u4f51\u5e87\u62a4");
        raw(en, zh, "fluid_type.stellaeomphalos.molten_lumen", "Molten Lumen", "\u7194\u878d\u661f\u8f89");
        raw(en, zh, "death.attack.stellaeomphalos.starlight", "%1$s was struck down by starlight", "%1$s \u88ab\u661f\u8f89\u51fb\u788e");
        raw(en, zh, "death.attack.stellaeomphalos.starlight.player", "%1$s was struck down by starlight whilst fighting %2$s", "%1$s \u5728\u4e0e %2$s \u6218\u6597\u65f6\u88ab\u661f\u8f89\u51fb\u788e");
        raw(en, zh, "death.attack.stellaeomphalos.boon_thorns", "%1$s was impaled on starlit thorns", "%1$s \u88ab\u661f\u8292\u8346\u68d8\u8d2f\u7a7f");
        raw(en, zh, "death.attack.stellaeomphalos.boon_thorns.player", "%1$s was impaled on %2$s's starlit thorns", "%1$s \u88ab %2$s \u7684\u661f\u8292\u8346\u68d8\u8d2f\u7a7f");
        raw(en, zh, "sign.stellaeomphalos.aevitas", "Aevitas", "\u751f\u53d1\u5ea7");
        raw(en, zh, "sign.stellaeomphalos.armara", "Armara", "\u536b\u620d\u5ea7");
        raw(en, zh, "sign.stellaeomphalos.discidia", "Discidia", "\u5f81\u4f10\u5ea7");
        raw(en, zh, "sign.stellaeomphalos.evorsio", "Evorsio", "\u5d29\u89e3\u5ea7");
        raw(en, zh, "sign.stellaeomphalos.vicio", "Vicio", "\u98ce\u884c\u5ea7");
        raw(en, zh, "sign.stellaeomphalos.bootes", "Bootes", "\u7267\u91ce\u5ea7");
        raw(en, zh, "sign.stellaeomphalos.fornax", "Fornax", "\u7194\u7089\u5ea7");
        raw(en, zh, "sign.stellaeomphalos.horologium", "Horologium", "\u66f4\u6f0f\u5ea7");
        raw(en, zh, "sign.stellaeomphalos.lucerna", "Lucerna", "\u8f89\u5149\u5ea7");
        raw(en, zh, "sign.stellaeomphalos.mineralis", "Mineralis", "\u77ff\u8109\u5ea7");
        raw(en, zh, "sign.stellaeomphalos.octans", "Octans", "\u5782\u9493\u5ea7");
        raw(en, zh, "sign.stellaeomphalos.pelotrio", "Pelotrio", "\u5b73\u751f\u5ea7");
        raw(en, zh, "sign.stellaeomphalos.gelu", "Gelu", "\u51dd\u971c\u5ea7");
        raw(en, zh, "sign.stellaeomphalos.ulteria", "Ulteria", "\u8fdc\u8292\u5ea7");
        raw(en, zh, "sign.stellaeomphalos.alcara", "Alcara", "\u8680\u88c2\u5ea7");
        raw(en, zh, "sign.stellaeomphalos.vorux", "Vorux", "\u9955\u566c\u5ea7");
        for (var section : new OmphalosConfig.Section[]{OmphalosConfig.COMMON, OmphalosConfig.SERVER, OmphalosConfig.CLIENT}) {
            section.keys().stream().sorted().forEach(key -> {
                String words = key.substring(key.indexOf('.') + 1).replaceAll("([a-z])([A-Z])", "$1 $2");
                String label = Character.toUpperCase(words.charAt(0)) + words.substring(1);
                add(en, zh, "config." + key, label, CHINESE.get(key));
            });
        }
        var futures = new ArrayList<CompletableFuture<?>>();
        futures.add(DataProvider.saveStable(cache, en, output.getOutputFolder().resolve("assets/stellaeomphalos/lang/en_us.json")));
        futures.add(DataProvider.saveStable(cache, zh, output.getOutputFolder().resolve("assets/stellaeomphalos/lang/zh_cn.json")));
        var example = new JsonObject();
        example.addProperty("schema_version", 1); example.addProperty("enabled", true);
        example.add("targets", new com.google.gson.JsonArray()); example.add("values", new com.google.gson.JsonArray());
        futures.add(DataProvider.saveStable(cache, example, output.getOutputFolder().resolve("data/stellaeomphalos/compat_blacklist/_example.json")));
        try {
            var structure = new net.minecraft.nbt.CompoundTag();
            structure.putInt("DataVersion", 3465);
            var size = new net.minecraft.nbt.ListTag();
            for (int i = 0; i < 3; i++) size.add(net.minecraft.nbt.IntTag.valueOf(1));
            structure.put("size", size);
            structure.put("blocks", new net.minecraft.nbt.ListTag());
            structure.put("entities", new net.minecraft.nbt.ListTag());
            var palette = new net.minecraft.nbt.ListTag(); var air = new net.minecraft.nbt.CompoundTag();
            air.putString("Name", "minecraft:air"); palette.add(air); structure.put("palette", palette);
            var bytes = new java.io.ByteArrayOutputStream();
            net.minecraft.nbt.NbtIo.writeCompressed(structure, bytes);
            byte[] content = bytes.toByteArray();
            cache.writeIfNeeded(output.getOutputFolder().resolve("data/stellaeomphalos/structures/foundation_empty.nbt"), content,
                    com.google.common.hash.Hashing.sha1().hashBytes(content));
        } catch (java.io.IOException exception) { return CompletableFuture.failedFuture(exception); }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }
    private static void raw(JsonObject en, JsonObject zh, String key, String english, String chinese) {
        en.addProperty(key, english); zh.addProperty(key, chinese);
    }
    private static void add(JsonObject en, JsonObject zh, String key, String english, String chinese) {
        en.addProperty("stellaeomphalos." + key, english); zh.addProperty("stellaeomphalos." + key, chinese);
    }
    @Override public String getName() { return "Stellae Omphalos foundation resources"; }
}

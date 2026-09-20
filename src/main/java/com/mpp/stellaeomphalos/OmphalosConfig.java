package com.mpp.stellaeomphalos;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/** Config events publish immutable snapshots; no callback touches a world. */
public final class OmphalosConfig {
    public enum Retrogen {
        OFF,
        LOADED_ONLY,
        NEW_CHUNKS
    }

    public static final Section COMMON = new Section(ModConfig.Type.COMMON);
    public static final Section SERVER = new Section(ModConfig.Type.SERVER);
    public static final Section CLIENT = new Section(ModConfig.Type.CLIENT);
    private static final AtomicInteger SERVER_REVISION = new AtomicInteger();

    static {
        COMMON.bool("worldgen.enabled", true);
        COMMON.values.put(
                "worldgen.retrogen", COMMON.builder.defineEnum("worldgen.retrogen", Retrogen.OFF));
        COMMON.integer("performance.maxSightlineLength", 128, 1, 128);
        COMMON.integer("performance.maxScannedBlocksPerTick", 512, 1, 65536);
        COMMON.decimal("performance.sightlineStepWidth", 0.05, 0.01, 1);
        COMMON.integer("performance.skyDensityGridSize", 32, 8, 128);
        COMMON.integer("performance.structureMatchBudgetPerTick", 256, 1, 65536);
        COMMON.integer("performance.maxScheduledTasksPerTick", 1024, 1, 65536);
        COMMON.integer("performance.lumenTopologyOpsPerTick", 64, 1, 4096);
        COMMON.integer("performance.lumenRoutingStepsPerTick", 512, 16, 65536);
        COMMON.integer("performance.lumenTickBudgetMicros", 1000, 50, 20000);
        COMMON.integer("performance.lumenMaxNodesPerSection", 256, 16, 4096);
        COMMON.integer("performance.lumenMaxHops", 32, 4, 128);
        COMMON.integer("performance.lumenProximityOpsPerTick", 32, 1, 1024);
        COMMON.bool("compat.enchantmentAmplification", true);
        COMMON.bool("logging.dataTableVerbose", false);
        COMMON.values.put(
                "crafting.disabledFamilies",
                COMMON.builder.defineListAllowEmpty(
                        java.util.List.of("crafting", "disabledFamilies"),
                        java.util.List.<String>of(),
                        value ->
                                value instanceof String text
                                        && net.minecraft.resources.ResourceLocation.tryParse(text)
                                                != null));
        SERVER.bool("gameplay.lightProximityAltarRecipe", true);
        SERVER.bool("gameplay.lightProximityWandRecipe", true);
        SERVER.bool("gameplay.giveCodexOnFirstJoin", true);
        SERVER.integer("gameplay.signPaperRarity", 10, 0, 10000);
        SERVER.integer("gameplay.signPaperQuality", 1, 0, 10);
        SERVER.bool("gameplay.mantleChaosResistance", true);
        SERVER.decimal("gameplay.weaponOilMultiplier", 0.25, 0, 1);
        SERVER.bool("gameplay.mobSpawnDenyAll", false);
        SERVER.decimal("gameplay.wandChainBreakChance", 0.1, 0, 1);
        SERVER.integer("gameplay.inactivityThresholdMs", 300000, 1000, 3600000);
        SERVER.integer("progression.maxBoonLevel", 30, 1, 1000);
        SERVER.bool("gameplay.lumenEnabled", true);
        SERVER.decimal("gameplay.lumenLossPerHop", 0.02, 0, 0.5);
        SERVER.bool("gameplay.stasisEnabled", true);
        SERVER.integer("gameplay.stasisMaxZones", 16, 1, 64);
        SERVER.integer("gameplay.stasisMaxRadius", 32, 4, 64);
        SERVER.integer("gameplay.stasisMaxFrozenBlockEntities", 512, 16, 4096);
        SERVER.bool("gameplay.domainEffectsEnabled", true);
        SERVER.decimal("gameplay.chargeRegenPerTick", 0.01, 0, 1);
        SERVER.values.put(
                "gameplay.lumenFluidHotInteraction",
                SERVER.builder.define("gameplay.lumenFluidHotInteraction", "sand_and_rare"));
        SERVER.values.put(
                "gameplay.lumenFluidColdResultBlock",
                SERVER.builder.define("gameplay.lumenFluidColdResultBlock", "minecraft:ice"));
        SERVER.values.put(
                "gameplay.lumenFluidHotResultBlock",
                SERVER.builder.define("gameplay.lumenFluidHotResultBlock", "minecraft:sand"));
        CLIENT.decimal("render.maxEffectDistance", 64, 8, 256);
        CLIENT.bool("render.gatewayShield", true);
        CLIENT.bool("render.hudChargeBars", true);
        CLIENT.bool("render.skySignOverlay", true);
        CLIENT.decimal("sound.masterScale", 1, 0, 1);
        CLIENT.bool("codex.animations", true);
        COMMON.build();
        SERVER.build();
        CLIENT.build();
    }

    private OmphalosConfig() {}

    public static void register(IEventBus bus) {
        for (var section : new Section[] {COMMON, SERVER, CLIENT}) {
            ModLoadingContext.get().registerConfig(section.type, section.spec);
        }
        bus.addListener(OmphalosConfig::onLoading);
        bus.addListener(OmphalosConfig::onReloading);
        bus.addListener(OmphalosConfig::onUnloading);
    }

    private static void onLoading(ModConfigEvent.Loading event) {
        refresh(event);
    }

    private static void onReloading(ModConfigEvent.Reloading event) {
        refresh(event);
    }

    private static void onUnloading(ModConfigEvent.Unloading event) {
        for (var section : new Section[] {COMMON, SERVER, CLIENT})
            if (event.getConfig().getSpec() == section.spec) section.defaults();
    }

    private static void refresh(ModConfigEvent event) {
        for (var section : new Section[] {COMMON, SERVER, CLIENT}) {
            if (event.getConfig().getSpec() == section.spec) {
                section.refresh();
                if (section == SERVER) SERVER_REVISION.incrementAndGet();
            }
        }
    }

    public static int serverRevision() {
        return SERVER_REVISION.get();
    }

    public static final class Section {
        private final ModConfig.Type type;
        private final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        private final Map<String, ForgeConfigSpec.ConfigValue<?>> values = new LinkedHashMap<>();
        private ForgeConfigSpec spec;
        private volatile Map<String, Object> snapshot = Map.of();

        private Section(ModConfig.Type type) {
            this.type = type;
        }

        private void bool(String key, boolean value) {
            values.put(key, builder.define(key, value));
        }

        private void integer(String key, int value, int min, int max) {
            values.put(key, builder.defineInRange(key, value, min, max));
        }

        private void decimal(String key, double value, double min, double max) {
            values.put(key, builder.defineInRange(key, value, min, max));
        }

        private void build() {
            spec = builder.build();
            defaults();
        }

        private void defaults() {
            var copy = new LinkedHashMap<String, Object>();
            values.forEach((key, value) -> copy.put(key, value.getDefault()));
            snapshot = Map.copyOf(copy);
        }

        private void refresh() {
            var copy = new LinkedHashMap<String, Object>();
            values.forEach((key, value) -> copy.put(key, value.get()));
            snapshot = Map.copyOf(copy);
        }

        public Map<String, Object> snapshot() {
            return snapshot;
        }

        public java.util.Set<String> keys() {
            return java.util.Set.copyOf(values.keySet());
        }

        public ForgeConfigSpec spec() {
            return spec;
        }

        public boolean flag(String key) {
            return (Boolean) snapshot.get(key);
        }

        public int integer(String key) {
            return ((Number) snapshot.get(key)).intValue();
        }

        public double decimal(String key) {
            return ((Number) snapshot.get(key)).doubleValue();
        }

        @SuppressWarnings("unchecked")
        public void set(String key, String text) {
            var value = values.get(key);
            if (value == null) throw new IllegalArgumentException("Unknown setting");
            Object sample = value.getDefault();
            Object parsed;
            if (sample instanceof Integer) parsed = Integer.valueOf(text);
            else if (sample instanceof Double) parsed = Double.valueOf(text);
            else if (sample instanceof Boolean) {
                if (!text.equals("true") && !text.equals("false"))
                    throw new IllegalArgumentException("Invalid boolean");
                parsed = Boolean.valueOf(text);
            } else if (sample instanceof Retrogen) parsed = Retrogen.valueOf(text);
            else throw new IllegalArgumentException("Unsupported setting");
            ForgeConfigSpec.ValueSpec rule =
                    spec.getSpec().get(java.util.Arrays.asList(key.split("\\.")));
            if (!rule.test(parsed)) throw new IllegalArgumentException("Setting out of range");
            ((ForgeConfigSpec.ConfigValue<Object>) value).set(parsed);
            spec.save();
            refresh();
        }

        public void toggle(String key) {
            if (!(values.get(key) instanceof ForgeConfigSpec.BooleanValue value))
                throw new IllegalArgumentException(key);
            value.set(!value.get());
            spec.save();
            refresh();
        }
    }
}

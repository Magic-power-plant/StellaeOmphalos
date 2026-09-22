package com.mpp.stellaeomphalos.constellation.sign;

import com.mojang.logging.LogUtils;
import com.mpp.stellaeomphalos.Omphalos;
import com.mpp.stellaeomphalos.data.loader.DataBootstrap;
import com.mpp.stellaeomphalos.data.loader.DataTable;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Assembly point of the sign module. Declares the sign / sign_naming / sign_sky_anchors data tables,
 * rebuilds {@link SignRegistry} from the sign table after every successful data reload, and wires
 * the sky tick/session hooks onto the Forge bus. The integrator calls {@link #attach(IEventBus)}
 * from the mod constructor and {@link SignServerHooks#attachServer()} for the C2S handler.
 */
public final class SignBootstrap {
    public static final Set<ResourceLocation> BUILTIN_SIGN_IDS = Set.of(
            id("aevitas"), id("armara"), id("discidia"), id("evorsio"), id("vicio"),
            id("bootes"), id("fornax"), id("horologium"), id("lucerna"), id("mineralis"), id("octans"), id("pelotrio"),
            id("gelu"), id("ulteria"), id("alcara"), id("vorux"));

    public static final DataTable<SignDefinitions.Definition> SIGNS =
            DataBootstrap.TABLES.declare(new DataTable<>(id("sign"), SignDefinitions.Definition.CODEC, SignDefinitions::validate));
    public static final DataTable<SignNamingLexicon> SIGN_NAMING =
            DataBootstrap.TABLES.declare(new DataTable<>(id("sign_naming"), SignNamingLexicon.CODEC, (entries, report) -> {
                if (!entries.containsKey(id("en_us"))) report.warn("sign_naming", "$", "en_us naming lexicon missing");
            }));
    public static final DataTable<SignSkyAnchorTable.SlotTable> SKY_ANCHORS =
            DataBootstrap.TABLES.declare(new DataTable<>(id("sign_sky_anchors"), SignSkyAnchorTable.SlotTable.CODEC,
                    SignBootstrap::validateAnchors));

    private SignBootstrap() {}

    private static ResourceLocation id(String path) { return new ResourceLocation(Omphalos.MODID, path); }

    private static void validateAnchors(Map<ResourceLocation, SignSkyAnchorTable.SlotTable> entries,
                                        com.mpp.stellaeomphalos.data.loader.DataLoadReport report) {
        entries.forEach((file, table) -> {
            if (table.majorSlots().size() != 5) report.error(file.toString(), "$.major_slots", "Exactly 5 major slots required");
            if (table.minorSlots().size() != 10) report.error(file.toString(), "$.minor_slots", "Exactly 10 minor slots required");
            table.majorSlots().forEach(anchor -> {
                if (anchor.radius() < 5 || anchor.radius() > 19)
                    report.error(file.toString(), "$.major_slots", "Major slot radius outside [5,19]: " + anchor.radius());
            });
            table.minorSlots().forEach(anchor -> {
                if (anchor.radius() < 10 || anchor.radius() > 35)
                    report.error(file.toString(), "$.minor_slots", "Minor slot radius outside [10,35]: " + anchor.radius());
            });
        });
    }

    public static void attach(IEventBus modBus) {
        com.mpp.stellaeomphalos.lumen.transport.LumenDistributionBridge.registerProvider((level, id) -> {
            var sign = SignRegistry.byId(id);
            return sign == null ? 0 : SignSkyService.distribution(level, sign);
        });
        var forge = MinecraftForge.EVENT_BUS;
        forge.addListener(SignBootstrap::addReloadListener);
        forge.addListener(SignBootstrap::levelTick);
        forge.addListener(SignBootstrap::login);
        forge.addListener(SignBootstrap::dimensionChange);
        forge.addListener(SignBootstrap::logout);
        forge.addListener(SignBootstrap::serverStop);
        modBus.addListener(SignBootstrap::commonSetup);
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(SignRegistry::freeze);
    }

    /** Runs after the foundation DataTableLoader (listener registration order), so tables are fresh. */
    private static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) manager -> rebuildSigns());
    }

    /** Rebuilds all sign instances from the data table; instances are replaced wholesale on reload. */
    public static void rebuildSigns() {
        var entries = DataBootstrap.TABLES.entries(SIGNS);
        var signs = entries.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> build(entry.getKey(), entry.getValue()))
                .filter(Objects::nonNull)
                .map(sign -> (com.mpp.stellaeomphalos.constellation.sign.Sign) sign)
                .toList();
        if (signs.isEmpty() && !entries.isEmpty()) return;    // keep the previous registry on total failure
        SignRegistry.rebuild(signs);
        SignSkyService.dataReloaded(ServerLifecycleHooks.getCurrentServer());
    }

    private static AbstractSign build(ResourceLocation id, SignDefinitions.Definition definition) {
        try {
            var items = definition.signatureItems().stream()
                    .map(item -> {
                        var value = ForgeRegistries.ITEMS.getValue(item);
                        if (value == null) LogUtils.getLogger().warn("Sign {} references unknown item {}", id, item);
                        return value;
                    })
                    .filter(Objects::nonNull)
                    .map(ItemStack::new)
                    .toList();
            AbstractSign sign = switch (definition.kind()) {
                case MAJOR -> new AbstractSign.Major(id, definition.color(), items);
                case RITUAL -> new AbstractSign.Ritual(id, definition.color(), items);
                case TRAIT -> new AbstractSign.Trait(id, definition.color(), items, definition.moonPhases());
                case ANOMALOUS -> new AbstractSign.Anomalous(id, definition.color(), items, definition.omen().orElseThrow());
            };
            definition.stars().forEach(sign::addStar);
            definition.lines().forEach(line -> sign.addConnection(line.a(), line.b()));
            return (AbstractSign) sign.seal();
        } catch (RuntimeException exception) {
            LogUtils.getLogger().error("Invalid sign definition {}", id, exception);
            return null;
        }
    }

    private static void levelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        SignSkyService.tick(level);
    }

    private static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) SignSkyService.join(player);
    }

    private static void dimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) SignSkyService.dimensionChange(player);
    }

    private static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) SignSkyService.logout(player);
    }

    private static void serverStop(ServerStoppingEvent event) {
        SignSkyService.serverStop();
    }
}

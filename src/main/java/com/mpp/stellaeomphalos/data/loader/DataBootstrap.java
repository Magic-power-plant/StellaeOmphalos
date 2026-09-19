package com.mpp.stellaeomphalos.data.loader;

import com.mpp.stellaeomphalos.Omphalos;
import com.mpp.stellaeomphalos.data.codec.PolicyEntry;
import com.mpp.stellaeomphalos.data.registry.OmphalosDataRegistries;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;

public final class DataBootstrap {
    public static final DataTableRegistry TABLES = new DataTableRegistry();
    public static final DataTable<PolicyEntry> COMPAT_BLACKLIST = policy("compat_blacklist");
    public static final DataTable<PolicyEntry> VISIBLE_DIMENSIONS = policy("visible_sign_dimensions");
    public static final DataTable<PolicyEntry> SUPPRESSED_DIMENSIONS = policy("suppressed_sky_dimensions");
    public static final DataTable<PolicyEntry> BOON_XP = policy("boon_xp_curve");
    private DataBootstrap() {}
    private static DataTable<PolicyEntry> policy(String path) {
        return TABLES.declare(new DataTable<>(new ResourceLocation(Omphalos.MODID, path), PolicyEntry.CODEC, (entries, report) -> {
            if (path.equals("boon_xp_curve")) entries.forEach((id, entry) -> {
                int previous = -1;
                for (int i = 0; i < entry.values().size(); i++) {
                    int current = entry.values().get(i);
                    if (current <= previous) report.error(id.toString(), "$.values[" + i + "]", "XP thresholds must increase");
                    previous = current;
                }
            });
        }));
    }
    public static void attach(IEventBus bus) {
        bus.addListener(OmphalosDataRegistries::register);
        bus.addListener(FoundationDataProvider::gather);
        MinecraftForge.EVENT_BUS.addListener(DataBootstrap::reload);
        MinecraftForge.EVENT_BUS.addListener(DataBootstrap::commands);
        RegistryRenameMap.attach();
    }
    private static void reload(AddReloadListenerEvent event) { event.addListener(new DataTableLoader(TABLES)); }
    private static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(Omphalos.MODID).requires(source -> source.hasPermission(2))
                .then(Commands.literal("datatable").then(Commands.literal("report").executes(context -> {
                    var report = TABLES.report();
                    context.getSource().sendSuccess(() -> Component.translatable("stellaeomphalos.command.data_report", report.size()), false);
                    report.forEach(issue -> context.getSource().sendSuccess(() -> Component.literal(
                            issue.severity() + " " + issue.file() + " " + issue.pointer() + ": " + issue.message()), false));
                    return report.size();
                }))));
    }
}

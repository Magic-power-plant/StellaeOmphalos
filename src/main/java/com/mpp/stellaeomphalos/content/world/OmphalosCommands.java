package com.mpp.stellaeomphalos.content.world;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mpp.stellaeomphalos.Omphalos;
import com.mpp.stellaeomphalos.constellation.boon.BoonNode;
import com.mpp.stellaeomphalos.constellation.boon.BoonTree;
import com.mpp.stellaeomphalos.constellation.sign.SignRegistry;
import com.mpp.stellaeomphalos.core.bootstrap.RuntimeServices;
import com.mpp.stellaeomphalos.core.util.io.AtomicFileWriter;
import com.mpp.stellaeomphalos.player.boon.BoonLevelCurve;
import com.mpp.stellaeomphalos.player.boon.BoonProgress;
import com.mpp.stellaeomphalos.player.profile.PlayerProfile;
import com.mpp.stellaeomphalos.player.profile.PlayerProfileCapability;
import com.mpp.stellaeomphalos.player.profile.ProfileSnapshotCodec;
import com.mpp.stellaeomphalos.knowledge.codex.KnowledgeCatalog;
import com.mpp.stellaeomphalos.structure.match.StructureIntegrityHub;
import com.mpp.stellaeomphalos.structure.pattern.BlueprintRegistry;
import com.mpp.stellaeomphalos.structure.pattern.PlacementContext;
import com.mpp.stellaeomphalos.structure.pattern.PlacementTransform;
import com.mpp.stellaeomphalos.structure.pattern.StructurePlacer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.RegisterCommandsEvent;

/** The single 《服务端机制数据存储与网络协议》 administrative command surface. Legacy command branches remain registered elsewhere. */
public final class OmphalosCommands {
    private static final List<String> SUBCOMMANDS = List.of(
            "help", "signs", "research", "progress", "reset", "boons", "attune", "build",
            "maximize", "network", "diagnose", "profile", "migrate");
    private static final SuggestionProvider<CommandSourceStack> SUBCOMMAND_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggest(SUBCOMMANDS, builder);

    private OmphalosCommands() {}

    public static void register(RegisterCommandsEvent event) {
        var root = Commands.literal(Omphalos.MODID)
                .requires(source -> source.hasPermission(0))
                .then(help())
                .then(signs())
                .then(research())
                .then(progress())
                .then(reset())
                .then(boons())
                .then(attune())
                .then(build())
                .then(maximize())
                .then(network())
                .then(diagnose())
                .then(profile())
                .then(migrate());
        event.getDispatcher().register(root);
        event.getDispatcher().register(Commands.literal("so").redirect(root.build()));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> help() {
        return Commands.literal("help")
                .requires(source -> source.hasPermission(0))
                .executes(context -> help(context, null))
                .then(Commands.argument("subcommand", StringArgumentType.word())
                        .suggests(SUBCOMMAND_SUGGESTIONS)
                        .executes(context -> help(context, StringArgumentType.getString(context, "subcommand"))));
    }

    private static int help(CommandContext<CommandSourceStack> context, String requested) {
        if (requested == null) {
            SUBCOMMANDS.forEach(sub -> success(context, "command.stellaeomphalos.help." + sub));
            return SUBCOMMANDS.size();
        }
        if (!SUBCOMMANDS.contains(requested.toLowerCase(Locale.ROOT))) {
            failure(context, "command.stellaeomphalos.invalid_subcommand", requested);
            return 0;
        }
        success(context, "command.stellaeomphalos.help." + requested.toLowerCase(Locale.ROOT));
        return 1;
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> signs() {
        var sign = Commands.argument("sign", StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                        java.util.stream.Stream.concat(
                                SignRegistry.all().stream().map(value -> value.id().toString()),
                                java.util.stream.Stream.of("all"))
                                .toList(), builder))
                .executes(context -> grantSigns(context, EntityArgument.getPlayer(context, "player"),
                        StringArgumentType.getString(context, "sign")));
        return Commands.literal("signs").requires(source -> source.hasPermission(2))
                .executes(context -> listSigns(context, self(context)))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> listSigns(context, EntityArgument.getPlayer(context, "player")))
                        .then(sign));
    }

    private static int listSigns(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        var ids = BoonProgress.getServer(player).knownSigns().stream()
                .map(ResourceLocation::toString).sorted().collect(Collectors.joining(", "));
        success(context, "command.stellaeomphalos.signs.list", player.getGameProfile().getName(), ids);
        return 1;
    }

    private static int grantSigns(CommandContext<CommandSourceStack> context, ServerPlayer player, String value) {
        var progress = BoonProgress.getServer(player);
        if (value.equalsIgnoreCase("all")) {
            int before = progress.knownSigns().size();
            SignRegistry.all().forEach(sign -> progress.discover(player, sign.id()));
            success(context, "command.stellaeomphalos.signs.granted", player.getGameProfile().getName(),
                    progress.knownSigns().size() - before);
            return 1;
        }
        var id = ResourceLocation.tryParse(value);
        if (id == null || SignRegistry.byId(id) == null) {
            failure(context, "command.stellaeomphalos.unknown_sign", value);
            return 0;
        }
        progress.discover(player, id);
        success(context, "command.stellaeomphalos.signs.granted", player.getGameProfile().getName(), 1);
        return 1;
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> research() {
        return Commands.literal("research").requires(source -> source.hasPermission(2))
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("group", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        researchIds(), builder))
                                .executes(context -> research(context))));
    }

    private static int research(CommandContext<CommandSourceStack> context)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player = EntityArgument.getPlayer(context, "player");
        var value = StringArgumentType.getString(context, "group");
        var profile = PlayerProfileCapability.get(player);
        if (value.equalsIgnoreCase("all")) {
            KnowledgeCatalog.initialize();
            int before = profile.researchGroups().size();
            KnowledgeCatalog.NODES.all().forEach(node -> profile.grantResearch(node.id()));
            String groups = profile.researchGroups().stream()
                    .map(ResourceLocation::toString)
                    .sorted()
                    .collect(Collectors.joining(", "));
            success(context, "command.stellaeomphalos.research.all", player.getGameProfile().getName(),
                    profile.researchGroups().size() - before, groups);
            return 1;
        }
        var id = ResourceLocation.tryParse(value);
        if (id == null) {
            failure(context, "command.stellaeomphalos.unknown_research", value);
            return 0;
        }
        profile.grantResearch(id);
        success(context, "command.stellaeomphalos.research.granted", player.getGameProfile().getName(), id);
        return 1;
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> progress() {
        var mode = Commands.argument("mode", StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(List.of("all", "next"), builder))
                .executes(context -> showProgress(context, EntityArgument.getPlayer(context, "player"),
                        StringArgumentType.getString(context, "mode")));
        return Commands.literal("progress").requires(source -> source.hasPermission(2))
                .executes(context -> showProgress(context, self(context), "all"))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> showProgress(context, EntityArgument.getPlayer(context, "player"), "all"))
                        .then(mode));
    }

    private static int showProgress(CommandContext<CommandSourceStack> context, ServerPlayer player, String mode) {
        var progress = BoonProgress.getServer(player);
        if (mode.equalsIgnoreCase("next")) {
            int next = Math.min(BoonLevelCurve.maxLevel(), progress.level() + 1);
            success(context, "command.stellaeomphalos.progress.next", player.getGameProfile().getName(), next,
                    BoonLevelCurve.expForLevel(next));
        } else {
            success(context, "command.stellaeomphalos.progress.show", player.getGameProfile().getName(),
                    progress.level(), progress.exp(), progress.availablePoints(), progress.knownSigns().size());
        }
        return 1;
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> reset() {
        return Commands.literal("reset").requires(source -> source.hasPermission(2))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> {
                            var player = EntityArgument.getPlayer(context, "player");
                            BoonProgress.getServer(player).reset(player);
                            PlayerProfileCapability.get(player).resetProgress();
                            success(context, "command.stellaeomphalos.reset.done", player.getGameProfile().getName());
                            return 1;
                        }));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> boons() {
        var node = Commands.argument("node", StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                        BoonTree.ready() ? BoonTree.get().nodes().stream().map(BoonNode::id).map(ResourceLocation::toString).toList() : List.of(), builder));
        return Commands.literal("boons").requires(source -> source.hasPermission(2))
                .executes(context -> listBoons(context, self(context)))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> listBoons(context, EntityArgument.getPlayer(context, "player")))
                        .then(Commands.literal("exp")
                                .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                        .executes(context -> {
                                            var player = EntityArgument.getPlayer(context, "player");
                                            BoonProgress.getServer(player).grantExp(player, LongArgumentType.getLong(context, "amount"));
                                            success(context, "command.stellaeomphalos.boons.exp", player.getGameProfile().getName());
                                            return 1;
                                        })))
                        .then(Commands.literal("unlock").then(node.executes(context -> unlock(context))))
                        .then(Commands.literal("seal").then(Commands.argument("node", ResourceLocationArgument.id())
                                .executes(context -> {
                                    var player = EntityArgument.getPlayer(context, "player");
                                    var id = ResourceLocationArgument.getId(context, "node");
                                    boolean changed = BoonProgress.getServer(player).setSealed(player, id, true);
                                    if (changed) success(context, "command.stellaeomphalos.boons.sealed", id);
                                    else failure(context, "command.stellaeomphalos.boon_denied");
                                    return changed ? 1 : 0;
                                }))));
    }

    private static int listBoons(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        var progress = BoonProgress.getServer(player);
        success(context, "command.stellaeomphalos.boons.unlocked", player.getGameProfile().getName(),
                progress.appliedNodes().stream().map(ResourceLocation::toString).sorted().collect(Collectors.joining(", ")),
                progress.exp());
        return 1;
    }

    private static int unlock(CommandContext<CommandSourceStack> context)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player = EntityArgument.getPlayer(context, "player");
        var value = StringArgumentType.getString(context, "node");
        var progress = BoonProgress.getServer(player);
        if (value.equalsIgnoreCase("all")) {
            int unlocked = 0;
            for (int pass = 0; pass < 4; pass++) {
                boolean changed = false;
                if (BoonTree.ready()) for (var node : BoonTree.get().nodes()) {
                    if (progress.unlock(player, node.id())) { unlocked++; changed = true; }
                }
                if (!changed) break;
            }
            success(context, "command.stellaeomphalos.boons.unlocked_count", player.getGameProfile().getName(), unlocked);
            return unlocked > 0 ? 1 : 0;
        }
        var id = ResourceLocation.tryParse(value);
        if (id == null || !progress.unlock(player, id)) {
            failure(context, "command.stellaeomphalos.boon_denied");
            return 0;
        }
        success(context, "command.stellaeomphalos.boon_unlocked", id);
        return 1;
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> attune() {
        var sign = Commands.argument("sign", StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                        java.util.stream.Stream.concat(StreamNames.majorSigns(), java.util.stream.Stream.of("none")).toList(), builder))
                .executes(context -> {
                    var player = EntityArgument.getPlayer(context, "player");
                    var value = StringArgumentType.getString(context, "sign");
                    ResourceLocation id = value.equalsIgnoreCase("none") ? null : ResourceLocation.tryParse(value);
                    if (id != null && SignRegistry.byId(id) == null) {
                        failure(context, "command.stellaeomphalos.unknown_sign", value);
                        return 0;
                    }
                    BoonProgress.getServer(player).setAttuned(player, id);
                    PlayerProfileCapability.get(player).setAttuned(id);
                    success(context, "command.stellaeomphalos.attune.done", player.getGameProfile().getName(), value);
                    return 1;
                });
        return Commands.literal("attune").requires(source -> source.hasPermission(2))
                .then(Commands.argument("player", EntityArgument.player()).then(sign));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> build() {
        var blueprint = Commands.argument("structure", ResourceLocationArgument.id())
                .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(BlueprintRegistry.all().keySet(), builder))
                .executes(OmphalosCommands::build)
                .then(Commands.literal("reload").executes(context -> {
                    success(context, "command.stellaeomphalos.build.reloaded");
                    return 1;
                }));
        return Commands.literal("build").requires(source -> source.hasPermission(2)).then(blueprint);
    }

    private static int build(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player = self(context);
        var id = ResourceLocationArgument.getId(context, "structure");
        var blueprint = BlueprintRegistry.find(id).orElse(null);
        if (blueprint == null) {
            failure(context, "command.stellaeomphalos.build.no_target", id);
            return 0;
        }
        var result = StructurePlacer.place(blueprint, player.serverLevel(), player.blockPosition(), PlacementTransform.values()[0],
                new PlacementContext(PlacementContext.Source.SCHEMATIC_PASTE, player.getUUID(), true, true, player.serverLevel().getSeed()));
        success(context, "command.stellaeomphalos.build.placed", result.placed(), result.skipped(), result.failed());
        return result.placed();
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> maximize() {
        return Commands.literal("maximize").requires(source -> source.hasPermission(2))
                .then(Commands.argument("player", EntityArgument.player()).executes(context -> {
                    var player = EntityArgument.getPlayer(context, "player");
                    var progress = BoonProgress.getServer(player);
                    for (int i = 0; i < 8192 && progress.level() < BoonLevelCurve.maxLevel(); i++) progress.grantExp(player, Long.MAX_VALUE);
                    unlockAll(progress, player);
                    success(context, "command.stellaeomphalos.maximize.done", player.getGameProfile().getName(), progress.level());
                    return 1;
                }));
    }

    private static void unlockAll(BoonProgress progress, ServerPlayer player) {
        if (!BoonTree.ready()) return;
        for (int pass = 0; pass < 4; pass++) {
            boolean changed = false;
            for (var node : BoonTree.get().nodes()) changed |= progress.unlock(player, node.id());
            if (!changed) return;
        }
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> network() {
        return Commands.literal("network").requires(source -> source.hasPermission(2))
                .then(Commands.argument("player", EntityArgument.player()).executes(context -> {
                    var player = EntityArgument.getPlayer(context, "player");
                    boolean started = RuntimeServices.current().beginNetworkDebug(player, player.blockPosition());
                    if (started) success(context, "command.stellaeomphalos.network.awaiting", player.getGameProfile().getName());
                    else failure(context, "command.stellaeomphalos.network.rejected");
                    return started ? 1 : 0;
                }));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> diagnose() {
        var kind = Commands.argument("kind", StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(List.of("network", "pattern", "profile"), builder))
                .executes(context -> diagnose(context, false))
                .then(Commands.argument("position", BlockPosArgument.blockPos()).executes(context -> diagnose(context, true)));
        return Commands.literal("diagnose").requires(source -> source.hasPermission(2)).then(kind);
    }

    private static int diagnose(CommandContext<CommandSourceStack> context, boolean hasPosition)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player = self(context);
        var kind = StringArgumentType.getString(context, "kind");
        if (kind.equals("network")) {
            var position = hasPosition ? BlockPosArgument.getBlockPos(context, "position") : player.blockPosition();
            return RuntimeServices.current().beginNetworkDebug(player, position) ? 1 : 0;
        }
        if (kind.equals("pattern")) {
            success(context, "command.stellaeomphalos.diagnose.header", kind,
                    StructureIntegrityHub.of(player.serverLevel()).revalidate());
            return 1;
        }
        var profile = PlayerProfileCapability.get(player);
        success(context, "command.stellaeomphalos.profile.status", player.getGameProfile().getName(),
                profile.knownSigns().size(), profile.researchGroups().size(), profile.unlockedBoons().size());
        return 1;
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> profile() {
        return Commands.literal("profile").requires(source -> source.hasPermission(4))
                .then(Commands.literal("export").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("file", StringArgumentType.word()).executes(context -> profileExport(context)))))
                .then(Commands.literal("import").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("file", StringArgumentType.word())
                                .executes(context -> profileImport(context, false))
                                .then(Commands.literal("--force")
                                        .executes(context -> profileImport(context, true)))))
                .then(Commands.literal("adopt").then(Commands.argument("old", EntityArgument.player())
                        .then(Commands.argument("new", EntityArgument.player()).executes(context -> {
                            var oldProfile = PlayerProfileCapability.get(EntityArgument.getPlayer(context, "old"));
                            var newPlayer = EntityArgument.getPlayer(context, "new");
                            PlayerProfileCapability.get(newPlayer).copyFrom(oldProfile);
                            success(context, "command.stellaeomphalos.profile.imported", newPlayer.getGameProfile().getName());
                            return 1;
                        }))))
                .then(Commands.literal("status").then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> {
                            var player = EntityArgument.getPlayer(context, "player");
                            var profile = PlayerProfileCapability.get(player);
                            success(context, "command.stellaeomphalos.profile.status", player.getGameProfile().getName(),
                                    profile.knownSigns().size(), profile.researchGroups().size(), profile.unlockedBoons().size());
                            return 1;
                        }))));
    }

    private static int profileExport(CommandContext<CommandSourceStack> context)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player = EntityArgument.getPlayer(context, "player");
        var file = safeProfileFile(context, StringArgumentType.getString(context, "file"));
        if (file == null) return 0;
        try {
            var snapshot = ProfileSnapshotCodec.encode(
                    PlayerProfileCapability.get(player),
                    player.getUUID(),
                    ProfileSnapshotCodec.serverFingerprint(context.getSource().getServer()));
            var bytes = new ByteArrayOutputStream();
            net.minecraft.nbt.NbtIo.writeCompressed(snapshot, bytes);
            AtomicFileWriter.write(file, bytes.toByteArray());
            success(context, "command.stellaeomphalos.profile.exported", player.getGameProfile().getName());
            return 1;
        } catch (IOException exception) {
            failure(context, "command.stellaeomphalos.profile.rejected");
            return 0;
        }
    }

    private static int profileImport(CommandContext<CommandSourceStack> context, boolean force)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player = EntityArgument.getPlayer(context, "player");
        var file = safeProfileFile(context, StringArgumentType.getString(context, "file"));
        if (file == null || !Files.isRegularFile(file)) {
            failure(context, "command.stellaeomphalos.profile.rejected");
            return 0;
        }
        try {
            var snapshot = net.minecraft.nbt.NbtIo.readCompressed(file.toFile());
            var detached = ProfileSnapshotCodec.decode(
                    snapshot,
                    player.getUUID(),
                    ProfileSnapshotCodec.serverFingerprint(context.getSource().getServer()),
                    force);
            PlayerProfileCapability.get(player).copyFrom(detached);
            success(context, "command.stellaeomphalos.profile.imported", player.getGameProfile().getName());
            return 1;
        } catch (IOException exception) {
            failure(context, "command.stellaeomphalos.profile.rejected");
            return 0;
        } catch (RuntimeException exception) {
            failure(context, exception.getMessage() != null && exception.getMessage().contains("identity")
                    ? "command.stellaeomphalos.profile.force_required"
                    : "command.stellaeomphalos.profile.rejected");
            return 0;
        }
    }

    private static List<String> researchIds() {
        KnowledgeCatalog.initialize();
        return java.util.stream.Stream.concat(
                        java.util.stream.Stream.of("all"),
                        KnowledgeCatalog.NODES.all().stream().map(node -> node.id().toString()))
                .toList();
    }

    private static Path safeProfileFile(CommandContext<CommandSourceStack> context, String name) {
        if (!name.matches("[A-Za-z0-9._-]{1,16}") || name.contains("..")) {
            failure(context, "command.stellaeomphalos.profile.rejected");
            return null;
        }
        var root = context.getSource().getServer().getWorldPath(LevelResource.ROOT)
                .resolve("stellaeomphalos_profiles").toAbsolutePath().normalize();
        var file = root.resolve(name + ".dat").normalize();
        if (!file.startsWith(root)) {
            failure(context, "command.stellaeomphalos.profile.rejected");
            return null;
        }
        return file;
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> migrate() {
        return Commands.literal("migrate").requires(source -> source.hasPermission(4))
                .then(Commands.literal("legacy-crystals").executes(context -> migration(context, "legacy-crystals")))
                .then(Commands.literal("legacy-progress").executes(context -> migration(context, "legacy-progress")))
                .then(Commands.literal("verify").executes(context -> migration(context, "verify")));
    }

    private static int migration(CommandContext<CommandSourceStack> context, String id) {
        success(context, "command.stellaeomphalos.migrate.result", id);
        return 1;
    }

    private static ServerPlayer self(CommandContext<CommandSourceStack> context)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return context.getSource().getPlayerOrException();
    }

    private static void success(CommandContext<CommandSourceStack> context, String key, Object... args) {
        context.getSource().sendSuccess(() -> Component.translatable(key, args), false);
    }

    private static void failure(CommandContext<CommandSourceStack> context, String key, Object... args) {
        context.getSource().sendFailure(Component.translatable(key, args));
    }

    private static final class StreamNames {
        private static java.util.stream.Stream<String> majorSigns() {
            return SignRegistry.majorSigns().stream().map(sign -> sign.id().toString());
        }
    }
}

package com.mpp.stellaeomphalos.content.world;

import com.mpp.stellaeomphalos.structure.pattern.*;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import com.mojang.brigadier.arguments.IntegerArgumentType;

public final class BlueprintCommands {
    private BlueprintCommands() {}
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("stellaeomphalos").requires(s -> s.hasPermission(2))
                .then(Commands.literal("paste")
                    .then(Commands.argument("blueprint", ResourceLocationArgument.id())
                        .suggests((context, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggestResource(
                                BlueprintRegistry.all().keySet(), builder))
                        .then(Commands.argument("origin", BlockPosArgument.blockPos())
                            .executes(c -> place(c, 0))
                            .then(Commands.argument("transform", IntegerArgumentType.integer(0, 7))
                                .executes(c -> place(c, IntegerArgumentType.getInteger(c, "transform"))))))));
    }
    private static int place(com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> c, int transform)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var level = c.getSource().getLevel();
        var origin = BlockPosArgument.getBlockPos(c, "origin");
        var blueprint = BlueprintRegistry.find(ResourceLocationArgument.getId(c, "blueprint")).orElse(null);
        if (blueprint == null) { c.getSource().sendFailure(Component.translatable("stellaeomphalos.paste.unknown")); return 0; }
        var actor = c.getSource().getEntity() instanceof net.minecraft.server.level.ServerPlayer p ? p : null;
        var result = StructurePlacer.place(blueprint, level, origin, PlacementTransform.values()[transform],
                new PlacementContext(PlacementContext.Source.SCHEMATIC_PASTE, actor == null ? null : actor.getUUID(), true, true, level.getSeed()));
        c.getSource().sendSuccess(() -> Component.translatable("stellaeomphalos.paste.result",
                result.placed(), result.skipped(), result.failed()), false);
        return result.placed();
    }
}

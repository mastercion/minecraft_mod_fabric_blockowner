package com.example;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class PermissionCommand {
    private static final Config config = Config.getInstance();

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, dedicated) -> {
            dispatcher.register(CommandManager.literal("blockowner")
                    .requires(PermissionCommand::hasPermission) // <-- NEW
                    .then(CommandManager.literal("allowlist")
                            .then(CommandManager.literal("add")
                                    .then(CommandManager.argument("playername", StringArgumentType.word())
                                            .executes(PermissionCommand::addPlayer)))
                            .then(CommandManager.literal("remove")
                                    .then(CommandManager.argument("playername", StringArgumentType.word())
                                            .executes(PermissionCommand::removePlayer)))
                    )
            );
        });
    }

    private static boolean hasPermission(ServerCommandSource source) {
        // Check if player is OP with enough level
        if (source.hasPermissionLevel(config.getPermission().getCommands())) {
            return true;
        }
        // Or check if username is in allowedPlayers list
        String username = source.getName();
        return config.getPermission().getAllowedPlayers().contains(username);
    }

    private static int addPlayer(CommandContext<ServerCommandSource> context) {
        String playerName = StringArgumentType.getString(context, "playername");
        if (!config.getPermission().getAllowedPlayers().contains(playerName)) {
            config.getPermission().getAllowedPlayers().add(playerName);
            config.save();
            context.getSource().sendFeedback(() -> Text.literal("[BlockOwner] ")
                    .formatted(Formatting.GREEN)
                    .append(Text.literal("Added player: ")
                            .formatted(Formatting.GRAY))
                    .append(Text.literal(playerName)
                            .formatted(Formatting.AQUA)), false);
        } else {
            context.getSource().sendError(Text.literal("[BlockOwner] Player already allowed: " + playerName));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int removePlayer(CommandContext<ServerCommandSource> context) {
        String playerName = StringArgumentType.getString(context, "playername");
        if (config.getPermission().getAllowedPlayers().contains(playerName)) {
            config.getPermission().getAllowedPlayers().remove(playerName);
            config.save();
            context.getSource().sendFeedback(() -> Text.literal("[BlockOwner] ")
                    .formatted(Formatting.GREEN)
                    .append(Text.literal("Removed player: ")
                            .formatted(Formatting.GRAY))
                    .append(Text.literal(playerName)
                            .formatted(Formatting.RED)), false);
        } else {
            context.getSource().sendError(Text.literal("[BlockOwner] Player not found: " + playerName));
        }
        return Command.SINGLE_SUCCESS;
    }
}


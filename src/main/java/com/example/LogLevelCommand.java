package com.example;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;




public class LogLevelCommand {
    private static final Config config = Config.getInstance();
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            dispatcher.register(CommandManager.literal("blockowner")
                    .requires(source -> hasPermission(source)) // <-- NEW
                    .then(CommandManager.literal("logLevel")
                            .then(CommandManager.argument("logLevel", StringArgumentType.word())
                                    .suggests((context, builder) -> {
                                        return builder.suggest("none").suggest("minimal").suggest("all").buildFuture();
                                    })
                                    .executes(LogLevelCommand::setLogLevel)))
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

    private static int setLogLevel(CommandContext<ServerCommandSource> context) {
        String level = StringArgumentType.getString(context, "logLevel").toLowerCase();
        switch (level) {
            case "none":
                context.getSource().sendFeedback(() ->
                                Text.literal("[BlockOwner] ")
                                        .formatted(Formatting.GREEN)
                                        .append(Text.literal("Log level set to:  ")
                                                .formatted(Formatting.GRAY))
                                        .append(Text.literal("NONE")
                                                .formatted(Formatting.BLUE)),
                        false
                );
                LoggerUtil.setLogLevel(LoggerUtil.LogLevel.NONE);
                config.setLogLevel("none"); // <-- Set to config
                //context.getSource().sendFeedback(() -> Text.literal("BlockOwner log level set to NONE").formatted(Formatting.GREEN), false);
                break;
            case "minimal":
                LoggerUtil.setLogLevel(LoggerUtil.LogLevel.MINIMAL);
                context.getSource().sendFeedback(() ->
                                Text.literal("[BlockOwner] ")
                                        .formatted(Formatting.GREEN)
                                        .append(Text.literal("Log level set to:  ")
                                                .formatted(Formatting.GRAY))
                                        .append(Text.literal("MINIMAL")
                                                .formatted(Formatting.BLUE)),
                        false
                );
                config.setLogLevel("minimal"); // <-- Set to config
                //context.getSource().sendFeedback(() -> Text.literal("BlockOwner log level set to MINIMAL").formatted(Formatting.GREEN), false);
                break;
            case "all":
                LoggerUtil.setLogLevel(LoggerUtil.LogLevel.ALL);
                context.getSource().sendFeedback(() ->
                                Text.literal("[BlockOwner] ")
                                        .formatted(Formatting.GREEN)
                                        .append(Text.literal("Log level set to:  ")
                                                .formatted(Formatting.GRAY))
                                        .append(Text.literal("ALL")
                                                .formatted(Formatting.BLUE)),
                        false
                );
                config.setLogLevel("all"); // <-- Set to config
                //context.getSource().sendFeedback(() -> Text.literal("BlockOwner log level set to ALL").formatted(Formatting.GREEN), false);
                break;
            default:
                context.getSource().sendFeedback(() ->
                                Text.literal("[BlockOwner] ")
                                        .formatted(Formatting.GREEN)
                                        .append(Text.literal("Invalid log level: ")
                                                .formatted(Formatting.GRAY))
                                        .append(Text.literal(level)
                                                .formatted(Formatting.BLUE)),
                        false
                );
                //context.getSource().sendFeedback(() -> Text.literal("Invalid log level: " + level).formatted(Formatting.RED), false);
                return 0;
        }
        config.save();
        return SINGLE_SUCCESS;
    }
}

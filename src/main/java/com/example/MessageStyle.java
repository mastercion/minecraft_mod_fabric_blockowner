package com.example;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class MessageStyle {

    // This will be initially loaded from the config
    static String displayFormat = Config.getInstance().getDisplayFormat();

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, dedicated) -> {
            dispatcher.register(CommandManager.literal("blockowner")
                    .then(CommandManager.literal("style")
                            .then(CommandManager.argument("format", StringArgumentType.greedyString())
                                    .executes(context -> {
                                        String format = StringArgumentType.getString(context, "format");
                                        Config.getInstance().setDisplayFormat(format);

                                        context.getSource().sendFeedback(() ->
                                                        Text.literal("[BlockOwner] ")
                                                                .formatted(Formatting.GREEN)
                                                                .append(Text.literal("Display format set to: ")
                                                                        .formatted(Formatting.GRAY))
                                                                .append(Text.literal(applyMinecraftFormatting(format))),
                                                false
                                        );
                                        return 1;
                                    }))));
        });
    }

    private static void setDisplayFormat(ServerCommandSource source, String format) {
        displayFormat = format;
        Config.getInstance().setDisplayFormat(format); // Save to config
        source.sendFeedback(() -> Text.literal("BlockOwner display format set to:"), false);
        source.sendFeedback(() -> Text.literal(applyMinecraftFormatting(format)), false);
    }

    public static String getDisplayFormat() {
        return displayFormat;
    }

    public static String applyFormat(BlockData blockData) {
        Config config = Config.getInstance();
        String displayFormat = config.getDisplayFormat();

        String formattedText = displayFormat;
        formattedText = formattedText.replace("{Player}", blockData.owner);
        formattedText = formattedText.replace("{Block}", blockData.block.getName().getString());  // Get the localized block name
        formattedText = formattedText.replace("{Date}", blockData.getFormattedTimestamp());

        return applyMinecraftFormatting(formattedText);
    }

    private static String applyMinecraftFormatting(String text) {
        return text.replaceAll("&([0-9a-fk-or])", "§$1");
    }
}

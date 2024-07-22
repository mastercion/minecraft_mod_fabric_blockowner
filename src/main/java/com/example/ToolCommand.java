package com.example;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class ToolCommand {
    private static final Config config = Config.getInstance();

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, dedicated) -> {
            dispatcher.register(CommandManager.literal("blockowner")
                    .then(CommandManager.literal("tool")
                            .then(CommandManager.argument("tool", ItemStackArgumentType.itemStack(registryAccess))
                                    .executes(ToolCommand::setTool)))
            );
        });
    }

    private static int setTool(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ItemStack itemStack = ItemStackArgumentType.getItemStackArgument(context, "tool").createStack(1, false);
        config.inspectTool = itemStack.getItem().toString();
        config.save();
        context.getSource().sendFeedback(() -> Text.literal("Inspect tool set to: " + config.inspectTool).formatted(Formatting.AQUA), true);
        return SINGLE_SUCCESS;
    }
}

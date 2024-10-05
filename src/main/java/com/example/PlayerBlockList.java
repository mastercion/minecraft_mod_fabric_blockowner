package com.example;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
public class PlayerBlockList {
    private static final int BLOCKS_PER_PAGE = 6;
    private static final int CHAT_CLEAR_LINES = 100;
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("blockowner")
                    .then(CommandManager.literal("list")
                            .then(CommandManager.argument("dimension", DimensionArgumentType.dimension())
                                    .then(CommandManager.argument("player", EntityArgumentType.player())
                                            .executes(context -> executeList(context.getSource(),
                                                    getDimensionName(DimensionArgumentType.getDimensionArgument(context, "dimension")),
                                                    EntityArgumentType.getPlayer(context, "player"),
                                                    1))
                                            .then(CommandManager.argument("page", IntegerArgumentType.integer(1))
                                                    .executes(context -> executeList(context.getSource(),
                                                            getDimensionName(DimensionArgumentType.getDimensionArgument(context, "dimension")),
                                                            EntityArgumentType.getPlayer(context, "player"),
                                                            IntegerArgumentType.getInteger(context, "page")))))
                                    .executes(context -> executeList(context.getSource(),
                                            getDimensionName(DimensionArgumentType.getDimensionArgument(context, "dimension")),
                                            context.getSource().getPlayer(),
                                            1)))
                            .executes(context -> executeList(context.getSource(),
                                    getDimensionName(context.getSource().getWorld()),
                                    context.getSource().getPlayer(),
                                    1))));
        });
    }
    private static String getDimensionName(ServerWorld world) {
        Identifier dimensionId = world.getRegistryKey().getValue();
        if (dimensionId.equals(World.OVERWORLD.getValue())) {
            return "minecraft:overworld";
        } else if (dimensionId.equals(World.NETHER.getValue())) {
            return "minecraft:the_nether";
        } else if (dimensionId.equals(World.END.getValue())) {
            return "minecraft:the_end";
        } else {
            return dimensionId.toString();
        }
    }
    private static int executeList(ServerCommandSource source, String dimension, ServerPlayerEntity player, int page) {
        if (player == null) {
            source.sendFeedback(() -> Text.literal("Player not found or not specified.").formatted(Formatting.RED), false);
            return 0;
        }
        clearChat(source);
        String playerName = player.getName().getString();
        List<BlockData> userBlocks = getBlocksForUser(playerName, dimension);
        if (userBlocks.isEmpty()) {
            source.sendFeedback(() -> Text.literal("No blocks found for player " + playerName + " in dimension " + dimension).formatted(Formatting.YELLOW), false);
            return 0;
        }
        int totalPages = (int) Math.ceil((double) userBlocks.size() / BLOCKS_PER_PAGE);
        page = Math.max(1, Math.min(page, totalPages));
        MutableText header = Text.literal("[BlockOwner] ")
                .formatted(Formatting.GREEN)
                .append(Text.literal("Blocks by ")
                        .formatted(Formatting.GOLD))
                .append(Text.literal(playerName)
                        .formatted(Formatting.AQUA))
                .append(Text.literal(" in ")
                        .formatted(Formatting.GOLD))
                .append(Text.literal(dimension)
                        .formatted(Formatting.GREEN));
        source.sendFeedback(() -> header, false);
        int startIndex = (page - 1) * BLOCKS_PER_PAGE;
        int endIndex = Math.min(startIndex + BLOCKS_PER_PAGE, userBlocks.size());
        for (int i = startIndex; i < endIndex; i++) {
            BlockData bd = userBlocks.get(i);
            BlockPos pos = getBlockPosForBlockData(playerName, bd);
            String blockName = bd.block.getTranslationKey().replace("block.minecraft.", "");
            MutableText blockInfo = Text.literal(String.format("%s (%d, %d, %d)", blockName, pos.getX(), pos.getY(), pos.getZ()))
                    .formatted(Formatting.GRAY);
            source.sendFeedback(() -> blockInfo, false);
        }
        MutableText footer = createPageNavigation(page, totalPages, playerName, dimension);
        source.sendFeedback(() -> footer, false);
        return userBlocks.size();
    }
    private static void clearChat(ServerCommandSource source) {
        for (int i = 0; i < CHAT_CLEAR_LINES; i++) {
            source.sendFeedback(() -> Text.literal(""), false);
        }
    }
    private static MutableText createPageNavigation(int currentPage, int totalPages, String playerName, String dimension) {
        MutableText footer = Text.literal("");
        if (currentPage > 1) {
            footer.append(createClickablePageButton("<<", currentPage - 1, playerName, dimension));
        } else {
            footer.append(Text.literal("<<").formatted(Formatting.GRAY));
        }
        footer.append(Text.literal(" Page " + currentPage + " of " + totalPages + " ").formatted(Formatting.GOLD));
        if (currentPage < totalPages) {
            footer.append(createClickablePageButton(">>", currentPage + 1, playerName, dimension));
        } else {
            footer.append(Text.literal(">>").formatted(Formatting.GRAY));
        }
        return footer;
    }
    private static MutableText createClickablePageButton(String text, int page, String playerName, String dimension) {
        return Text.literal(text)
                .formatted(Formatting.YELLOW)
                .styled(style -> style
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                "/blockowner list " + dimension + " " + playerName + " " + page))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Text.literal("Go to page " + page))));
    }
    private static List<BlockData> getBlocksForUser(String user, String dimension) {
        List<BlockData> userBlocks = new ArrayList<>();
        Map<BlockPos, BlockData> playerBlocks = EventHandlers.userBlockOwners.get(user);
        if (playerBlocks != null) {
            for (Map.Entry<BlockPos, BlockData> entry : playerBlocks.entrySet()) {
                BlockData bd = entry.getValue();
                if (bd.dimension.equals(dimension)) {
                    userBlocks.add(bd);
                }
            }
        }
        return userBlocks;
    }
    private static BlockPos getBlockPosForBlockData(String user, BlockData blockData) {
        Map<BlockPos, BlockData> playerBlocks = EventHandlers.userBlockOwners.get(user);
        if (playerBlocks != null) {
            for (Map.Entry<BlockPos, BlockData> entry : playerBlocks.entrySet()) {
                if (entry.getValue() == blockData) {
                    return entry.getKey();
                }
            }
        }
        return new BlockPos(0, 0, 0); // Fallback if not found
    }
}
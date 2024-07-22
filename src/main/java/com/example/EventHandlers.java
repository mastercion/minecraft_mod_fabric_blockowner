package com.example;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class EventHandlers {
    private static final Map<String, Map<BlockPos, BlockData>> userBlockOwners = new HashMap<>();
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(BlockPos.class, new BlockPosAdapter())
            .registerTypeAdapter(BlockData.class, new BlockDataAdapter())
            .setPrettyPrinting()
            .create();
    private static final File CONFIG_DIR = new File("config/blockowner/player");
    private static Config config = Config.getInstance(); // Use getInstance() to access the singleton

    static {
        if (!CONFIG_DIR.exists()) {
            CONFIG_DIR.mkdirs();
        }
    }

    public static void register() {
        LoggerUtil.log("Registering event handlers.", LoggerUtil.LogLevel.MINIMAL);
        config.load(); // Ensure config is loaded properly
        loadAllUserBlockData();

        // Register block placement tracking
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            LoggerUtil.log("UseBlockCallback.EVENT triggered.", LoggerUtil.LogLevel.ALL);
            if (!world.isClient) {

                BlockPos pos = hitResult.getBlockPos().offset(hitResult.getSide());
                ItemStack itemStack = player.getStackInHand(hand);
                // Check if the item in hand is a block item
                if (itemStack.getItem() instanceof BlockItem) {
                    Block block = ((BlockItem) itemStack.getItem()).getBlock();
                    BlockPos immutablePos = pos.toImmutable();
                    String playerName = player.getName().getString();
                    LocalDateTime timestamp = LocalDateTime.now();
                    userBlockOwners.computeIfAbsent(playerName, k -> new HashMap<>())
                            .put(immutablePos, new BlockData(block, playerName, timestamp));
                    LoggerUtil.log("Adding block to blockOwners: " + immutablePos + " placed by " + playerName, LoggerUtil.LogLevel.MINIMAL);
                    saveBlockData(playerName);
                    LoggerUtil.log("Block placed by: " + playerName + " at " + immutablePos + " on " + timestamp, LoggerUtil.LogLevel.MINIMAL);
                } else {
                    LoggerUtil.log("Item in hand is not a block item: " + itemStack.getItem().getTranslationKey(), LoggerUtil.LogLevel.ALL);
                }

            } else {
                LoggerUtil.log("Event triggered on client side, ignoring.", LoggerUtil.LogLevel.ALL);
            }
            return ActionResult.PASS;
        });

        // Register item usage callback for wooden hoe
        UseItemCallback.EVENT.register((player, world, hand) -> {
            LoggerUtil.log("UseItemCallback.EVENT triggered.", LoggerUtil.LogLevel.ALL);
            if (!world.isClient && player.getStackInHand(hand).getItem().toString().equals(config.inspectTool)) {
                if (player.hasPermissionLevel(4)) { // Check if the player is an operator
                    BlockHitResult hitResult = rayTrace(world, (ServerPlayerEntity) player, RaycastContext.FluidHandling.NONE);
                    if (hitResult.getType() == HitResult.Type.BLOCK) {
                        BlockPos pos = hitResult.getBlockPos().toImmutable();
                        Block block = world.getBlockState(pos).getBlock();
                        LoggerUtil.log("Ray traced block at: " + pos + " of type " + block.getTranslationKey(), LoggerUtil.LogLevel.ALL);

                        BlockData blockData = null;
                        for (Map<BlockPos, BlockData> blockDataMap : userBlockOwners.values()) {
                            if (blockDataMap.containsKey(pos)) {
                                blockData = blockDataMap.get(pos);
                                break;
                            }
                        }

                        if (blockData != null) {
                            player.sendMessage(Text.of("Block placed by: " + blockData.owner + " (Block: " + blockData.block.getTranslationKey() + ", Date: " + blockData.getFormattedTimestamp() + ")"), true);
                            LoggerUtil.log("Block placed by: " + blockData.owner, LoggerUtil.LogLevel.MINIMAL);
                        } else if (block != Blocks.AIR) {
                            player.sendMessage(Text.of("Block not tracked (Block: " + block.getTranslationKey() + ")"), true);
                            LoggerUtil.log("Block not tracked at: " + pos, LoggerUtil.LogLevel.ALL);
                        } else {
                            player.sendMessage(Text.of("Air block not tracked"), true);
                            LoggerUtil.log("Air block not tracked at: " + pos, LoggerUtil.LogLevel.ALL);
                        }
                    } else {
                        LoggerUtil.log("Ray tracing did not hit a block", LoggerUtil.LogLevel.ALL);
                    }
                    return new TypedActionResult<>(ActionResult.SUCCESS, player.getStackInHand(hand));
                } else {
                    LoggerUtil.log("Player " + player.getName().getString() + " is not an operator, ignoring event.", LoggerUtil.LogLevel.MINIMAL);
                }
            }
            return new TypedActionResult<>(ActionResult.PASS, player.getStackInHand(hand));
        });
    }

    private static BlockHitResult rayTrace(World world, ServerPlayerEntity player, RaycastContext.FluidHandling fluidHandling) {
        float tickDelta = 1.0F;
        BlockHitResult result = world.raycast(new RaycastContext(
                player.getCameraPosVec(tickDelta),
                player.getCameraPosVec(tickDelta).add(player.getRotationVec(tickDelta).multiply(20)),
                RaycastContext.ShapeType.OUTLINE,
                fluidHandling,
                player
        ));
        LoggerUtil.log("Ray trace result: " + result, LoggerUtil.LogLevel.ALL);
        return result;
    }

    private static void saveBlockData(String playerName) {
        File dataFile = new File(CONFIG_DIR, playerName + "BlockData.json");
        try (FileWriter writer = new FileWriter(dataFile)) {
            Map<BlockPos, BlockData> blockData = userBlockOwners.get(playerName);
            if (blockData != null) {
                // Manually create JSON structure to ensure correct key format
                JsonObject jsonObject = new JsonObject();
                for (Map.Entry<BlockPos, BlockData> entry : blockData.entrySet()) {
                    BlockPos pos = entry.getKey();
                    BlockData data = entry.getValue();
                    String key = pos.getX() + "," + pos.getY() + "," + pos.getZ();
                    JsonElement value = GSON.toJsonTree(data);
                    jsonObject.add(key, value);
                }
                String jsonData = GSON.toJson(jsonObject);
                writer.write(jsonData);
                LoggerUtil.log("Block data saved to " + dataFile.getAbsolutePath(), LoggerUtil.LogLevel.MINIMAL);
                LoggerUtil.log("Block data content: " + jsonData, LoggerUtil.LogLevel.ALL);
            } else {
                LoggerUtil.log("No block data to save for player " + playerName, LoggerUtil.LogLevel.MINIMAL);
            }
        } catch (IOException e) {
            LoggerUtil.log("Failed to save block data: " + e.getMessage(), LoggerUtil.LogLevel.MINIMAL);
            e.printStackTrace();
        }
    }

    private static void loadAllUserBlockData() {
        File[] files = CONFIG_DIR.listFiles((dir, name) -> name.endsWith("BlockData.json"));
        if (files != null) {
            for (File file : files) {
                String playerName = file.getName().replace("BlockData.json", "");
                loadBlockData(playerName);
            }
        }
    }

    private static void loadBlockData(String playerName) {
        File dataFile = new File(CONFIG_DIR, playerName + "BlockData.json");
        if (dataFile.exists() && dataFile.length() > 0) {
            try (FileReader reader = new FileReader(dataFile)) {
                Type type = new TypeToken<Map<String, BlockData>>() {}.getType();
                Map<String, BlockData> data = GSON.fromJson(reader, type);
                if (data != null) {
                    Map<BlockPos, BlockData> blockData = new HashMap<>();
                    for (Map.Entry<String, BlockData> entry : data.entrySet()) {
                        String[] parts = entry.getKey().split(",");
                        if (parts.length == 3) {
                            int x = Integer.parseInt(parts[0].trim());
                            int y = Integer.parseInt(parts[1].trim());
                            int z = Integer.parseInt(parts[2].trim());
                            BlockPos pos = new BlockPos(x, y, z);
                            blockData.put(pos, entry.getValue());
                        }
                    }
                    userBlockOwners.put(playerName, blockData);
                    LoggerUtil.log("Block data loaded from " + dataFile.getAbsolutePath(), LoggerUtil.LogLevel.MINIMAL);
                }
            } catch (IOException | JsonSyntaxException e) {
                LoggerUtil.log("Failed to load block data: " + e.getMessage(), LoggerUtil.LogLevel.MINIMAL);
                e.printStackTrace();
            }
        } else {
            LoggerUtil.log("Data file is empty or does not exist, skipping loading for " + playerName, LoggerUtil.LogLevel.MINIMAL);
        }
    }
}

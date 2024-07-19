package com.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
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
import java.util.HashMap;
import java.util.Map;

public class EventHandlers {
    private static final Map<String, Map<BlockPos, BlockData>> userBlockOwners = new HashMap<>();
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(BlockPos.class, new BlockPosAdapter())
            .registerTypeAdapter(BlockData.class, new BlockDataAdapter())
            .setPrettyPrinting()
            .create();

    public static void register() {
        System.out.println("DEBUG: Registering event handlers.");
        loadAllUserBlockData();

        // Register block placement tracking
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            System.out.println("DEBUG: UseBlockCallback.EVENT triggered.");
            if (!world.isClient) {
                BlockPos pos = hitResult.getBlockPos().offset(hitResult.getSide());
                ItemStack itemStack = player.getStackInHand(hand);

                // Check if the item in hand is a block item
                if (itemStack.getItem() instanceof BlockItem) {
                    Block block = ((BlockItem) itemStack.getItem()).getBlock();
                    BlockPos immutablePos = pos.toImmutable();
                    String playerName = player.getName().getString();
                    userBlockOwners.computeIfAbsent(playerName, k -> new HashMap<>())
                            .put(immutablePos, new BlockData(block, playerName));
                    System.out.println("DEBUG: Adding block to blockOwners: " + immutablePos + " placed by " + playerName);
                    saveBlockData(playerName);
                    System.out.println("DEBUG: Block placed by: " + playerName + " at " + immutablePos);
                } else {
                    System.out.println("DEBUG: Item in hand is not a block item: " + itemStack.getItem().getTranslationKey());
                }
            } else {
                System.out.println("DEBUG: Event triggered on client side, ignoring.");
            }
            return ActionResult.PASS;
        });

        // Register item usage callback for wooden hoe
        UseItemCallback.EVENT.register((player, world, hand) -> {
            System.out.println("DEBUG: UseItemCallback.EVENT triggered.");
            if (!world.isClient && player.getStackInHand(hand).getItem() == Items.WOODEN_HOE) {
                if (player.hasPermissionLevel(4)) { // Check if the player is an operator
                    BlockHitResult hitResult = rayTrace(world, (ServerPlayerEntity) player, RaycastContext.FluidHandling.NONE);
                    if (hitResult.getType() == HitResult.Type.BLOCK) {
                        BlockPos pos = hitResult.getBlockPos().toImmutable();
                        Block block = world.getBlockState(pos).getBlock();
                        System.out.println("DEBUG: Ray traced block at: " + pos + " of type " + block.getTranslationKey());
                        String playerName = player.getName().getString();
                        Map<BlockPos, BlockData> blockOwners = userBlockOwners.get(playerName);
                        if (blockOwners != null && blockOwners.containsKey(pos)) {
                            BlockData blockData = blockOwners.get(pos);
                            player.sendMessage(Text.of("Block placed by: " + blockData.owner + " (Block: " + blockData.block.getTranslationKey() + ")"), true);
                            System.out.println("DEBUG: Block placed by: " + blockData.owner);
                        } else if (block != Blocks.AIR) {
                            player.sendMessage(Text.of("Block not tracked (Block: " + block.getTranslationKey() + ")"), true);
                            System.out.println("DEBUG: Block not tracked at: " + pos);
                        } else {
                            player.sendMessage(Text.of("Air block not tracked"), true);
                            System.out.println("DEBUG: Air block not tracked at: " + pos);
                        }
                    } else {
                        System.out.println("DEBUG: Ray tracing did not hit a block");
                    }
                    return new TypedActionResult<>(ActionResult.SUCCESS, player.getStackInHand(hand));
                } else {
                    System.out.println("DEBUG: Player " + player.getName().getString() + " is not an operator, ignoring event.");
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
        System.out.println("DEBUG: Ray trace result: " + result);
        return result;
    }

    private static void saveBlockData(String playerName) {
        File dataFile = new File(playerName + "BlockData.json");
        try (FileWriter writer = new FileWriter(dataFile)) {
            Map<BlockPos, BlockData> blockData = userBlockOwners.get(playerName);
            if (blockData != null) {
                GSON.toJson(blockData, writer);
                System.out.println("DEBUG: Block data saved to " + dataFile.getAbsolutePath());
            } else {
                System.out.println("DEBUG: No block data to save for player " + playerName);
            }
        } catch (IOException e) {
            System.err.println("DEBUG: Failed to save block data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void loadAllUserBlockData() {
        File[] files = new File(".").listFiles((dir, name) -> name.endsWith("BlockData.json"));
        if (files != null) {
            for (File file : files) {
                String playerName = file.getName().replace("BlockData.json", "");
                loadBlockData(playerName);
            }
        }
    }

    private static void loadBlockData(String playerName) {
        File dataFile = new File(playerName + "BlockData.json");
        if (dataFile.exists() && dataFile.length() > 0) {
            try (FileReader reader = new FileReader(dataFile)) {
                Type type = new TypeToken<Map<BlockPos, BlockData>>(){}.getType();
                Map<BlockPos, BlockData> data = GSON.fromJson(reader, type);
                if (data != null) {
                    userBlockOwners.put(playerName, data);
                    System.out.println("DEBUG: Block data loaded from " + dataFile.getAbsolutePath());
                }
            } catch (IOException | JsonSyntaxException e) {
                System.err.println("DEBUG: Failed to load block data: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("DEBUG: Data file is empty or does not exist, skipping loading for " + playerName);
        }
    }
}

package com.example;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BlockDisplayVisualizer {
    private static final Logger LOGGER = LoggerFactory.getLogger("blockowner");
    private static final Map<UUID, Set<ArmorStandEntity>> activeDisplays = new HashMap<>();
    private static final Map<UUID, Integer> playerRadii = new HashMap<>();
    private static final Map<UUID, String> targetPlayers = new HashMap<>();

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("blockowner")
                    .then(CommandManager.literal("visualize")
                            .then(CommandManager.argument("targetPlayer", EntityArgumentType.player())
                                    .then(CommandManager.argument("radius", IntegerArgumentType.integer(10))
                                            .executes(context -> startVisualization(
                                                    context.getSource().getPlayer(),
                                                    EntityArgumentType.getPlayer(context, "targetPlayer").getName().getString(),
                                                    IntegerArgumentType.getInteger(context, "radius")
                                            ))
                                    )
                            )
                    )
            );
        });

        ServerTickEvents.END_WORLD_TICK.register(BlockDisplayVisualizer::updateDisplays);
    }

    private static int startVisualization(ServerPlayerEntity executor, String targetPlayerName, int radius) {
        UUID executorId = executor.getUuid();
        cleanupDisplays(executorId); // Clean up any existing displays for this executor

        playerRadii.put(executorId, radius);
        targetPlayers.put(executorId, targetPlayerName);

        executor.sendMessage(
                Text.literal("[BlockOwner] ")
                        .formatted(Formatting.GREEN)
                        .append(Text.literal("Visualizing blocks placed by ")
                                .formatted(Formatting.GOLD))
                        .append(Text.literal(targetPlayerName)
                                .formatted(Formatting.AQUA))
                        .append(Text.literal(" Radius: ")
                                .formatted(Formatting.GREEN))
                        .append(Text.literal(String.valueOf(radius))
                                .formatted(Formatting.AQUA)),
                false
        );
        updateDisplays((ServerWorld) executor.getWorld(), executorId);
        return 1;
    }

    private static void updateDisplays(ServerWorld world) {
        for (UUID executorId : new HashSet<>(playerRadii.keySet())) {
            updateDisplays(world, executorId);
        }
    }

    private static void updateDisplays(ServerWorld world, UUID executorId) {
        ServerPlayerEntity executor = world.getServer().getPlayerManager().getPlayer(executorId);
        if (executor == null) {
            cleanupDisplays(executorId);
            return;
        }

        String targetPlayerName = targetPlayers.get(executorId);
        int radius = playerRadii.get(executorId);
        BlockPos executorPos = executor.getBlockPos();

        cleanupDisplays(executorId);
        activeDisplays.put(executorId, new HashSet<>());

        // Get all blocks placed by the target player
        Map<BlockPos, BlockData> playerBlocks = EventHandlers.userBlockOwners.get(targetPlayerName);
        if (playerBlocks != null) {
            for (Map.Entry<BlockPos, BlockData> entry : playerBlocks.entrySet()) {
                BlockPos blockPos = entry.getKey();
                if (blockPos.isWithinDistance(executorPos, radius)) {
                    ArmorStandEntity armorStand = createArmorStand(world, blockPos, entry.getValue());
                    world.spawnEntity(armorStand);
                    activeDisplays.get(executorId).add(armorStand);
                }
            }
        }
    }

    private static void cleanupDisplays(UUID executorId) {
        Set<ArmorStandEntity> displays = activeDisplays.get(executorId);
        if (displays != null) {
            for (ArmorStandEntity armorStand : displays) {
                armorStand.discard();
            }
            displays.clear();
        }
        activeDisplays.remove(executorId);
        playerRadii.remove(executorId);
        targetPlayers.remove(executorId);
    }

    private static ArmorStandEntity createArmorStand(ServerWorld world, BlockPos blockPos, BlockData blockData) {
        ArmorStandEntity armorStand = new ArmorStandEntity(EntityType.ARMOR_STAND, world);
        armorStand.setPosition(Vec3d.ofCenter(blockPos.up()));
        armorStand.setInvisible(true);
        armorStand.setGlowing(true);
        armorStand.setNoGravity(true);
        armorStand.setInvulnerable(true);
        armorStand.equipStack(EquipmentSlot.HEAD, blockData.block.getDefaultState().getBlock().asItem().getDefaultStack());

        return armorStand;
    }
}

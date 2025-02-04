package com.example;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.PlayerSkullBlock;
import net.minecraft.block.SkullBlock;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Proxy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

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
                                                    Objects.requireNonNull(context.getSource().getPlayer()),
                                                    EntityArgumentType.getPlayer(context, "targetPlayer").getName().getString(),
                                                    IntegerArgumentType.getInteger(context, "radius")
                                            ))
                                    )
                            )
                            .then(CommandManager.literal("clear")
                                    .executes(context -> {
                                        ServerPlayerEntity executor = context.getSource().getPlayer();
                                        UUID executorId = executor != null ? executor.getUuid() : null;
                                        cleanupDisplays(executor, executorId, false); // Non-silent cleanup
                                        return 1;
                                    })
                            )
                    )
            );
        });

        ServerTickEvents.END_WORLD_TICK.register(BlockDisplayVisualizer::updateDisplays);
    }

    private static int startVisualization(ServerPlayerEntity executor, String targetPlayerName, int radius) {
        UUID executorId = executor.getUuid();
        //cleanupDisplays(executor, executorId); // Clean up any existing displays for this executor

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
        updateDisplays((ServerWorld) executor.getWorld());
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
            cleanupDisplays(executor, executorId, true);
            return;
        }

        String targetPlayerName = targetPlayers.get(executorId);
        int radius = playerRadii.get(executorId);
        BlockPos executorPos = executor.getBlockPos();

        cleanupDisplays(executor, executorId, true);
        activeDisplays.put(executorId, new HashSet<>());

        // Get all blocks placed by the target player
        Map<BlockPos, BlockData> playerBlocks = EventHandlers.userBlockOwners.get(targetPlayerName);
        if (playerBlocks != null) {
            for (Map.Entry<BlockPos, BlockData> entry : playerBlocks.entrySet()) {
                BlockPos blockPos = entry.getKey();
                if (blockPos.isWithinDistance(executorPos, radius)) {
                    ArmorStandEntity armorStand = createArmorStand( world, blockPos, entry.getValue(), executorId, targetPlayerName);
                    world.spawnEntity(armorStand);
                    activeDisplays.get(executorId).add(armorStand);
                }
            }
        }
    }

    private static void cleanupDisplays(ServerPlayerEntity executor, UUID executorId, boolean silent) {
        Set<ArmorStandEntity> displays = activeDisplays.get(executorId);
        if (displays != null) {
            for (ArmorStandEntity armorStand : displays) {
                armorStand.discard();
            }
            displays.clear();
        }

        if (!silent && executor != null) {
            executor.sendMessage(
                    Text.literal("[BlockOwner] ")
                            .formatted(Formatting.GREEN)
                            .append(Text.literal("Stopped ")
                                    .formatted(Formatting.GOLD))
                            .append(Text.literal("Visualizing Blocks ")
                                    .formatted(Formatting.AQUA)),
                    false
            );
        }
        activeDisplays.remove(executorId);
        playerRadii.remove(executorId);
        targetPlayers.remove(executorId);
    }

    private static ArmorStandEntity createArmorStand(ServerWorld world, BlockPos blockPos, BlockData blockData, UUID playerUuid, String playerName) {
        ArmorStandEntity armorStand = new ArmorStandEntity(EntityType.ARMOR_STAND, world);
        armorStand.setPosition(Vec3d.ofCenter(blockPos.up()));
        armorStand.setInvisible(true);
        //armorStand.setGlowing(true);
        armorStand.setNoGravity(true);
        armorStand.setInvulnerable(true);

        // Create a player head item stack
        ItemStack playerHead = new ItemStack(Items.PLAYER_HEAD);

        // Set the SkullOwner data for the player head
        if (playerUuid != null) {
            // Create a ProfileComponent for the player
            GameProfile gameProfile = new GameProfile(playerUuid, playerName);

            // Fetch the skin data from Mojang's session server
            fetchSkinData(gameProfile);

            ProfileComponent profile = new ProfileComponent(gameProfile);

            // Set the SkullOwner component on the item stack
            playerHead.set(DataComponentTypes.PROFILE, profile);
        }

        // Equip the armor stand with the player head
        armorStand.equipStack(EquipmentSlot.HEAD, playerHead);
        //System.out.println("Used Player Head name: " + playerHead + " " + playerName);

        return armorStand;
    }
    private static void fetchSkinData(GameProfile profile) {
        try {
            // Create an HTTP client to send requests
            HttpClient client = HttpClient.newHttpClient();

            // Build the URL to fetch the player's profile
            String url = "https://sessionserver.mojang.com/session/minecraft/profile/" + profile.getId().toString().replace("-", "");

            // Create an HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            // Send the request and get the response
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Check if the response is valid
            if (response.statusCode() != 200) {
                System.err.println("Failed to fetch skin data for player: " + profile.getName() + " (HTTP " + response.statusCode() + ")");
                return;
            }

            // Parse the response JSON
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();

            // Debug: Print the entire JSON response
            //System.out.println("API Response: " + json);

            // Get the "properties" array from the response
            if (!json.has("properties")) {
                System.err.println("No 'properties' field in the response for player: " + profile.getName());
                return;
            }

            // Get the first property object
            JsonObject properties = json.getAsJsonArray("properties").get(0).getAsJsonObject();

            // Extract the texture value
            String textureValue = properties.get("value").getAsString();

            // Add the texture property to the GameProfile (without a signature)
            profile.getProperties().put("textures", new Property("textures", textureValue)); // No signature needed
        } catch (Exception e) {
            System.err.println("Failed to fetch skin data for player: " + profile.getName());
            e.printStackTrace();
        }
    }

}

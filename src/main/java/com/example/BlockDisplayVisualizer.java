package com.example;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        playerRadii.put(executorId, radius);
        targetPlayers.put(executorId, targetPlayerName);

        executor.sendMessage(
                Text.literal("[BlockOwner] ")
                        .formatted(Formatting.GREEN)
                        .append(Text.literal("Visualizing blocks by ")
                                .formatted(Formatting.GOLD))
                        .append(Text.literal(targetPlayerName)
                                .formatted(Formatting.AQUA))
                        .append(Text.literal(" Radius: ")
                                .formatted(Formatting.GREEN))
                        .append(Text.literal(String.valueOf(radius))
                                .formatted(Formatting.AQUA)),
                false
        );
        int radiusWave = 20; // or any desired value
        BlockPos playerPos = executor.getBlockPos();
        ServerWorld world = (ServerWorld) executor.getWorld();
        spawnSwirlingEffect(world, playerPos, radiusWave);
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
                BlockData blockDataEntry = entry.getValue();
                if (blockPos.isWithinDistance(executorPos, radius)) {
                    ArmorStandEntity armorStand = createArmorStands(world, blockPos, blockDataEntry, executorId, targetPlayerName);
                    // Armor stands are already spawned within createArmorStands
                    // No need to call spawnEntity or add them to activeDisplays here again

                    // Add some particle effects for visual appeal
                    world.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
                            blockPos.getX() + 0.5,
                            blockPos.getY() + 1.0,
                            blockPos.getZ() + 0.5,
                            10, 0.2, 0.2, 0.2, 0.1);

                    // Optionally, you can add a floating effect to the armor stands
                    armorStand.setVelocity(0, 0.1, 0); // This will make the armor stand float slightly
                }
            }
        }
    }


    private static void cleanupDisplays(ServerPlayerEntity executor, UUID executorId, boolean silent) {
        Set<ArmorStandEntity> displays = activeDisplays.get(executorId);
        if (displays != null) {
            for (ArmorStandEntity armorStand : displays) {
                armorStand.discard(); // Clean up the armor stand
            }
            displays.clear(); // Clear the set after discarding
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

        int radiusWave = 20; // or any desired value
        BlockPos playerPos = executor.getBlockPos();
        ServerWorld world = (ServerWorld) executor.getWorld();
        spawnEffectClear(world, playerPos, radiusWave);
    }

    private static ArmorStandEntity createArmorStands(ServerWorld world, BlockPos blockPos, BlockData blockData, UUID executorId, String playerName) {
        BlockState state = world.getBlockState(blockPos);
        String blockName = Registries.BLOCK.getId(state.getBlock()).toString();
        String timestamp = blockData.getFormattedTimestamp(); // Get the formatted timestamp


        Formatting color = getRandomColor(); // Random color for nametags

        // Create armor stand for block name
        ArmorStandEntity armorStandName = new ArmorStandEntity(EntityType.ARMOR_STAND, world);

        // Set the armor stand's position
        armorStandName.setPosition(Vec3d.ofCenter(blockPos.up(0)));
        armorStandName.setInvisible(true);
        armorStandName.setNoGravity(true);
        armorStandName.setInvulnerable(true);
        armorStandName.setCustomName(Text.literal(blockName).formatted(color));
        armorStandName.setCustomNameVisible(true);
        world.spawnEntity(armorStandName); // Spawn the armor stand

        // Create armor stand for the timestamp
        ArmorStandEntity armorStandTimestamp = new ArmorStandEntity(EntityType.ARMOR_STAND, world);
        armorStandTimestamp.setPosition(Vec3d.ofCenter(blockPos.up(-1))); // Position at the block
        armorStandTimestamp.setInvisible(true);
        armorStandTimestamp.setNoGravity(true);
        armorStandTimestamp.setInvulnerable(true);
        armorStandTimestamp.setCustomName(Text.literal(timestamp).formatted(color));
        armorStandTimestamp.setCustomNameVisible(true);

        // Create a player head item stack
        ItemStack playerHead = new ItemStack(Items.PLAYER_HEAD);
        // Set the SkullOwner data for the player head
        if (executorId != null) {
            // Create a ProfileComponent for the player
            GameProfile gameProfile = new GameProfile(executorId, playerName);
            // Fetch the skin data from Mojang's session server
            fetchSkinData(gameProfile);
            ProfileComponent profile = new ProfileComponent(gameProfile);
            // Set the SkullOwner component on the item stack
            playerHead.set(DataComponentTypes.PROFILE, profile);
        }
        // Equip the armor stand with the player head
        armorStandTimestamp.equipStack(EquipmentSlot.HEAD, playerHead);

        world.spawnEntity(armorStandTimestamp); // Spawn the timestamp armor stand

        // Store both armor stands in the active displays
        Set<ArmorStandEntity> displays = activeDisplays.computeIfAbsent(executorId, k -> new HashSet<>());
        displays.add(armorStandName);
        displays.add(armorStandTimestamp);

        return armorStandName; // You could also return both or use void if only spawning
    }


    private static Formatting getRandomColor() {
        Formatting[] colors = {
                Formatting.BLACK,
                Formatting.DARK_BLUE,
                Formatting.DARK_GREEN,
                Formatting.DARK_AQUA,
                Formatting.DARK_RED,
                Formatting.DARK_PURPLE,
                Formatting.GRAY,
                Formatting.DARK_GRAY,
                Formatting.BLUE,
                Formatting.GREEN,
                Formatting.AQUA,
                Formatting.RED,
                Formatting.LIGHT_PURPLE,
                Formatting.YELLOW,
                Formatting.WHITE
        };

        return colors[new Random().nextInt(colors.length)];
    }

    private static void spawnSwirlingEffect(ServerWorld world, BlockPos playerPos, int radius) {
        Random random = new Random();
        int particleCount = 120; // Number of particles to spawn

        for (int i = 0; i < particleCount; i++) {
            // Calculate random angle and distance
            double angle = random.nextDouble() * Math.PI * 2; // Random angle around the circle
            double distance = random.nextDouble() * radius; // Random distance from the player within given radius

            double angle_var = random.nextDouble() * Math.PI * 2;
            double distance_var = random.nextDouble() * radius;

            // Calculate particle position based on angle and distance
            double posX = playerPos.getX() + Math.cos(angle) * distance; // X position
            double posY = playerPos.getY() + random.nextDouble() * radius; // Some height variation
            double posZ = playerPos.getZ() + Math.sin(angle) * distance; // Z position

            double posX_var = playerPos.getX() + Math.cos(angle_var) * distance_var; // X position
            double posY_var = playerPos.getY() + random.nextDouble() * radius; // Some height variation
            double posZ_var = playerPos.getZ() + Math.sin(angle_var) * distance_var; // Z position


            world.spawnParticles(ParticleTypes.END_ROD,
                    posX, posY, posZ,
                    0, // Number of particles per spawn
                    0.2, 0.2, 0.2, 0.05);

            world.spawnParticles(ParticleTypes.WAX_ON,
                    posX_var, posY_var, posZ_var,
                    0, // Number of particles per spawn
                    0.2, 0.2, 0.2, 0.05);
        }
    }

    private static void spawnEffectClear(ServerWorld world, BlockPos playerPos, int radius) {
        Random random = new Random();
        int particleCount = 120; // Number of particles to spawn

        for (int i = 0; i < particleCount; i++) {
            // Calculate random angle and distance
            double angle = random.nextDouble() * Math.PI * 2; // Random angle around the circle
            double distance = random.nextDouble() * radius; // Random distance from the player within given radius

            // Calculate particle position based on angle and distance
            double posX = playerPos.getX() + Math.cos(angle) * distance; // X position
            double posY = playerPos.getY() + random.nextDouble() * radius; // Some height variation
            double posZ = playerPos.getZ() + Math.sin(angle) * distance; // Z position


            world.spawnParticles(ParticleTypes.CRIT,
                    posX, posY, posZ,
                    0, // Number of particles per spawn
                    0.2, 0.2, 0.2, 0.05);
        }
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

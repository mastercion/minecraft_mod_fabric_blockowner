package com.example;

import com.mojang.authlib.properties.PropertyMap;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.time.LocalDateTime;
import java.util.*;

public class BlockTrackerGui {

    public static final String VERSION;

    // --- Main Menu: Player List ---
    public static void openPlayerList(ServerPlayerEntity player) {
        // Create a generic 9x6 chest GUI
        SimpleGui gui = new SimpleGui(ScreenHandlerType.GENERIC_9X6, player, false);
        gui.setTitle(Text.literal("BlockOwner v" + VERSION + " - Players").formatted(Formatting.DARK_BLUE));

        // Get all players from your EventHandlers
        Set<String> playerNames = EventHandlers.userBlockOwners.keySet();
        List<String> sortedPlayers = new ArrayList<>(playerNames);
        Collections.sort(sortedPlayers);

        int slot = 0;
        for (String targetName : sortedPlayers) {
            if (slot >= 54) break; // Limit to 54 players for this example (add paging if needed)

            // Create Player Head
            ItemStack head = new ItemStack(Items.PLAYER_HEAD);

            // 1.21 way to set skin: ProfileComponent
            // We create a profile with just the name. The client usually resolves the skin.
            // For a perfect skin fetch, you'd need the UUID, but name often works on online servers.
            head.set(DataComponentTypes.PROFILE, new ProfileComponent(Optional.of(targetName), Optional.empty(), new PropertyMap()));

            gui.setSlot(slot, new GuiElementBuilder()
                    .setItem(head.getItem())
                    .setName(Text.literal(String.valueOf(targetName)).formatted(Formatting.GOLD))
                    .setLore(Collections.singletonList(Text.literal("Click to view blocks").formatted(Formatting.GRAY)))
                    .setCallback((index, type, action, topGui) -> {
                        // On click: Open that player's log
                        openPlayerLog(player, String.valueOf(targetName), 1);
                    })
            );
            slot++;
        }

        gui.open();
    }

    static {
        ModMetadata metadata = FabricLoader.getInstance().getModContainer("modid").get().getMetadata();
        String rawVersion = metadata.getVersion().getFriendlyString();

        if (rawVersion != null && rawVersion.length() > 6) {
            VERSION = rawVersion.substring(0, rawVersion.length() - 7);
        } else {
            VERSION = rawVersion;
        }
    }

    // --- Sub Menu: Block Log ---
    public static void openPlayerLog(ServerPlayerEntity viewer, String targetPlayerName, int page) {
        SimpleGui gui = new SimpleGui(ScreenHandlerType.GENERIC_9X6, viewer, false);
        gui.setTitle(Text.literal("Log: " + targetPlayerName + " (Page " + page + ")").formatted(Formatting.DARK_GREEN));

        // 1. Fetch Data
        // Note: We access the map directly. We assume insertion order = time order if it's a LinkedHashMap,
        // otherwise we might need a timestamp in BlockData to sort by "Latest".
        // Here we just reverse the map values to try and show "latest" first.
        Map<BlockPos, BlockData> rawBlocks = EventHandlers.userBlockOwners.get(targetPlayerName);
        List<Map.Entry<BlockPos, BlockData>> blockList = new ArrayList<>();
        if (rawBlocks != null) {
            blockList.addAll(rawBlocks.entrySet());

            blockList.sort((entry1, entry2) -> {
                LocalDateTime t1 = entry1.getValue().timestamp;
                LocalDateTime t2 = entry2.getValue().timestamp;

                // Handle potential nulls if you have old data saved without timestamps
                if (t1 == null) return 1; // Puts nulls at the bottom
                if (t2 == null) return -1;

                // Compare: t2.compareTo(t1) creates Descending Order (Newest first)
                return t2.compareTo(t1);
            });
        }

        // 2. Pagination Logic
        int itemsPerPage = 45; // 5 rows
        int totalPages = (int) Math.ceil((double) blockList.size() / itemsPerPage);
        if (totalPages == 0) totalPages = 1;

        // Clamp page
        final int currentPage = Math.max(1, Math.min(page, totalPages)); // Use final for lambda

        int startIndex = (currentPage - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, blockList.size());

        // 3. Populate Blocks (Rows 1-5)
        for (int i = startIndex; i < endIndex; i++) {
            Map.Entry<BlockPos, BlockData> entry = blockList.get(i);
            BlockPos pos = entry.getKey();
            BlockData data = entry.getValue();

            int guiSlot = i - startIndex;

            // Create the icon
            ItemStack icon = new ItemStack(data.block);

            // Add the item to the GUI with the Callback
            gui.setSlot(guiSlot, new GuiElementBuilder()
                    .setItem(icon.getItem())
                    // Set Name
                    .setName(Text.translatable(data.block.getTranslationKey()).formatted(Formatting.AQUA))
                    // Set Lore (Timestamp + Coords)
                    .setLore(Arrays.asList(
                            Text.literal("Time: " + data.getFormattedTimestamp()).formatted(Formatting.GRAY),
                            Text.literal("X: " + pos.getX() + " Y: " + pos.getY() + " Z: " + pos.getZ()).formatted(Formatting.YELLOW),
                            Text.literal("Dimension: " + data.dimension).formatted(Formatting.DARK_GRAY),
                            Text.literal("Mode: " + data.gamemode).formatted(Formatting.ITALIC),
                            Text.literal("").formatted(Formatting.RESET),
                            Text.literal("Click to Highlight").formatted(Formatting.GREEN)
                    ))
                    // --- NEW: This is where .setCallback goes ---
                    .setCallback((index, type, action, topGui) -> {
                        // 1. Close the GUI
                        viewer.closeHandledScreen();

                        // 2. Check if player is in same dimension/world for highlighting
                        if (viewer.getWorld() instanceof ServerWorld serverWorld) {

                            HighlightManager.highlightBlock(serverWorld, pos);

                            viewer.sendMessage(Text.literal("Highlighting block at " + pos.toShortString())
                                    .formatted(Formatting.GREEN), true);
                        }
                    })
            );
        }

        // 4. Navigation Bar (Row 6)
        // Fill empty spots with gray glass panes for aesthetics
        for (int i = 45; i < 54; i++) {
            gui.setSlot(i, new GuiElementBuilder().setItem(Items.GRAY_STAINED_GLASS_PANE).setName(Text.empty()));
        }

        // Previous Page Button (Bottom Left - Slot 45)
        if (currentPage > 1) {
            gui.setSlot(45, new GuiElementBuilder()
                    .setItem(Items.ARROW)
                    .setName(Text.literal("<- Previous Page").formatted(Formatting.YELLOW))
                    .setCallback((index, type, action, topGui) -> openPlayerLog(viewer, targetPlayerName, currentPage - 1))
            );
        }

        // Back to Player List (Middle - Slot 49)
        gui.setSlot(49, new GuiElementBuilder()
                .setItem(Items.BARRIER)
                .setName(Text.literal("Back to Player List").formatted(Formatting.RED))
                .setCallback((index, type, action, topGui) -> openPlayerList(viewer))
        );

        // Next Page Button (Bottom Right - Slot 53)
        if (currentPage < totalPages) {
            gui.setSlot(53, new GuiElementBuilder()
                    .setItem(Items.ARROW)
                    .setName(Text.literal("Next Page ->").formatted(Formatting.YELLOW))
                    .setCallback((index, type, action, topGui) -> openPlayerLog(viewer, targetPlayerName, currentPage + 1))
            );
        }

        gui.open();
    }
}
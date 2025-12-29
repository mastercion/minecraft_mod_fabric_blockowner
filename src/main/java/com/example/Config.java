package com.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Config {
    private static final File CONFIG_FILE = new File("config/blockowner/config/config.json");
    private static Config instance;
    private String inspectTool = "minecraft:wooden_hoe";
    private String displayFormat = "&0{Date} &6{Player} &1{Block}";
    private Permission permission = new Permission();
    private String logLevel = "minimal";


    // Singleton pattern
    public static Config getInstance() {
        if (instance == null) {
            instance = new Config();
            instance.load();
        }
        return instance;
    }

    Config() {
    }

    public String getInspectTool() {
        return inspectTool;
    }

    public void setInspectTool(String inspectTool) {
        this.inspectTool = inspectTool;
    }

    public String getDisplayFormat() {
        return displayFormat;
    }

    public void setDisplayFormat(String displayFormat) {
        this.displayFormat = displayFormat;
    }

    public Permission getPermission() {return permission;}

    public void setPermission(Permission permission) {this.permission = permission;}

    public void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                Gson gson = new GsonBuilder()
                        .registerTypeAdapter(Config.class, new ConfigSerializer())
                        .create();
                instance = gson.fromJson(reader, Config.class);
                instance.applySettings();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            save(); // Save default values if config file does not exist
            applySettings();
        }
    }

    private void applySettings() {
        // Apply the log level at startup
        if (logLevel != null) {
            switch (logLevel.toLowerCase()) {
                case "none" -> LoggerUtil.setLogLevel(LoggerUtil.LogLevel.NONE);
                case "minimal" -> LoggerUtil.setLogLevel(LoggerUtil.LogLevel.MINIMAL);
                case "all" -> LoggerUtil.setLogLevel(LoggerUtil.LogLevel.ALL);
                default -> LoggerUtil.setLogLevel(LoggerUtil.LogLevel.MINIMAL); // Default fallback
            }
        }
    }

    public void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(Config.class, new ConfigSerializer())
                    .setPrettyPrinting()
                    .create();
            gson.toJson(this, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Item getInspectToolItem() {
        Identifier itemId = Identifier.tryParse(inspectTool);
        if (itemId != null) {
            Item item = Registries.ITEM.get(itemId);
            if (item != null) {
                return item;
            } else {
                System.err.println("Invalid item identifier: " + inspectTool);
            }
        } else {
            System.err.println("Invalid item identifier format: " + inspectTool);
        }
        // Return a default item if the identifier is invalid or item does not exist
        return Items.AIR;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public static class Permission {
        private int commands = 2;        // Default OP level 2
        private int inspectTool = 2;     // Default OP level 2
        private List<String> allowedPlayers = new ArrayList<>(); // Default empty list

        public int getCommands() {
            return commands;
        }

        public void setCommands(int commands) {
            this.commands = commands;
        }

        public int getInspectTool() {
            return inspectTool;
        }

        public void setInspectTool(int inspectTool) {
            this.inspectTool = inspectTool;
        }

        public List<String> getAllowedPlayers() {
            return allowedPlayers;
        }

        public void setAllowedPlayers(List<String> allowedPlayers) {
            this.allowedPlayers = allowedPlayers;
        }
    }
}

package com.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class Config {
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Config.class, new ConfigSerializer())
            .setPrettyPrinting()
            .create();
    private static final File CONFIG_FILE = new File("config/blockowner/config/config.json");

    public String inspectTool = "minecraft:wooden_hoe";

    private static Config instance;

    private Config() {
        // Private constructor to prevent instantiation
    }

    public static Config getInstance() {
        if (instance == null) {
            instance = new Config();
            instance.load();
        }
        return instance;
    }

    public void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                Config loadedConfig = GSON.fromJson(reader, Config.class);
                if (loadedConfig != null) {
                    this.inspectTool = loadedConfig.inspectTool;
                    System.out.println("Config loaded: " + this.inspectTool);
                } else {
                    System.err.println("Loaded config is null, saving default config.");
                    save();
                }
            } catch (IOException | JsonIOException | JsonSyntaxException e) {
                System.err.println("Failed to load config: " + e.getMessage());
                e.printStackTrace();
                save(); // Save default config if loading fails
            }
        } else {
            save();  // Save default config if it does not exist
        }
    }

    public void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(this, writer);
            System.out.println("Config saved: " + this.inspectTool);
        } catch (IOException e) {
            System.err.println("Failed to save config: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

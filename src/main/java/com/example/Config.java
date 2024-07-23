package com.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class Config {
    private static final File CONFIG_FILE = new File("config/blockowner/config/config.json");
    private static Config instance;
    private String inspectTool = "minecraft:wooden_hoe";
    private String displayFormat = "&0{Date} &6{Player} &1{Block}";

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

    public void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                Gson gson = new GsonBuilder()
                        .registerTypeAdapter(Config.class, new ConfigSerializer())
                        .create();
                instance = gson.fromJson(reader, Config.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            save(); // Save default values if config file does not exist
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
        return Registries.ITEM.get(new Identifier(inspectTool));
    }
}

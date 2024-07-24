package com.example;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BlockDataAdapter implements JsonSerializer<BlockData>, JsonDeserializer<BlockData> {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public JsonElement serialize(BlockData src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj = new JsonObject();
        obj.addProperty("block", Registries.BLOCK.getId(src.block).toString());
        obj.addProperty("owner", src.owner);
        obj.addProperty("timestamp", src.timestamp.format(formatter));
        obj.addProperty("dimension", src.dimension);  // Add this line
        return obj;
    }

    @Override
    public BlockData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
        JsonObject obj = json.getAsJsonObject();
        String blockName = obj.get("block").getAsString();
        Identifier blockId = Identifier.tryParse(blockName);
        Block block = null;
        if (blockId != null) {
            block = Registries.BLOCK.get(blockId);
        } else {
            System.err.println("Invalid block identifier: " + blockName);
        }
        String owner = obj.get("owner").getAsString();
        LocalDateTime timestamp = LocalDateTime.parse(obj.get("timestamp").getAsString(), formatter);
        String dimension = obj.has("dimension") ? obj.get("dimension").getAsString() : "minecraft:overworld";
        return new BlockData(block, owner, timestamp, dimension);
    }
}
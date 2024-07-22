package com.example;

import com.google.gson.*;

import java.lang.reflect.Type;

import net.minecraft.util.math.BlockPos;

public class BlockPosAdapter implements JsonSerializer<BlockPos>, JsonDeserializer<BlockPos> {
    @Override
    public JsonElement serialize(BlockPos src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.getX() + "," + src.getY() + "," + src.getZ());
    }

    @Override
    public BlockPos deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        String[] parts = json.getAsString().split(",");
        if (parts.length != 3) {
            throw new JsonParseException("Invalid BlockPos format");
        }
        int x = Integer.parseInt(parts[0].trim());
        int y = Integer.parseInt(parts[1].trim());
        int z = Integer.parseInt(parts[2].trim());
        return new BlockPos(x, y, z);
    }
}

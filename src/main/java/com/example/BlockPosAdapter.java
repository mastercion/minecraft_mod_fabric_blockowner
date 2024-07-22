package com.example;

import com.google.gson.*;
import net.minecraft.util.math.BlockPos;

import java.lang.reflect.Type;

public class BlockPosAdapter implements JsonSerializer<BlockPos>, JsonDeserializer<BlockPos> {

    @Override
    public JsonElement serialize(BlockPos src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.getX() + "," + src.getY() + "," + src.getZ());
    }

    @Override
    public BlockPos deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        String[] parts = json.getAsString().split(",");
        if (parts.length != 3) {
            throw new JsonParseException("Invalid BlockPos string: " + json.getAsString());
        }
        int x = Integer.parseInt(parts[0]);
        int y = Integer.parseInt(parts[1]);
        int z = Integer.parseInt(parts[2]);
        return new BlockPos(x, y, z);
    }
}

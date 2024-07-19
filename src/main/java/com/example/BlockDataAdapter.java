package com.example;

import com.google.gson.*;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.lang.reflect.Type;

public class BlockDataAdapter implements JsonSerializer<BlockData>, JsonDeserializer<BlockData> {
    @Override
    public JsonElement serialize(BlockData src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj = new JsonObject();
        obj.addProperty("block", Registries.BLOCK.getId(src.block).toString());
        obj.addProperty("owner", src.owner);
        return obj;
    }

    @Override
    public BlockData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();
        Block block = Registries.BLOCK.get(new Identifier(obj.get("block").getAsString()));
        String owner = obj.get("owner").getAsString();
        return new BlockData(block, owner);
    }
}
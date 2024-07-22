package com.example;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;

import java.lang.reflect.Type;

public class ConfigSerializer implements JsonSerializer<Config>, JsonDeserializer<Config> {

    @Override
    public JsonElement serialize(Config src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("inspectTool", src.inspectTool);
        return jsonObject;
    }

    @Override
    public Config deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        String inspectTool = jsonObject.get("inspectTool").getAsString();
        Config config = Config.getInstance();
        config.inspectTool = inspectTool;
        return config;
    }
}

package com.example;

import com.google.gson.*;

import java.lang.reflect.Type;

public class ConfigSerializer implements JsonSerializer<Config>, JsonDeserializer<Config> {

    @Override
    public JsonElement serialize(Config src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("inspectTool", src.getInspectTool());
        jsonObject.addProperty("displayFormat", src.getDisplayFormat());
        return jsonObject;
    }

    @Override
    public Config deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        Config config = new Config();
        if (jsonObject.has("inspectTool")) {
            config.setInspectTool(jsonObject.get("inspectTool").getAsString());
        }
        if (jsonObject.has("displayFormat")) {
            config.setDisplayFormat(jsonObject.get("displayFormat").getAsString());
        }
        return config;
    }
}

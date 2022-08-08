package net.worldseed.multipart.parser.generator;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class TextureGenerator {
    public static Map<String,byte[]> generate(JsonArray textures) {
        Map<String,byte[]> textureMap = new HashMap<>();

        for (var texture : textures) {
            JsonObject textureObj = texture.getAsJsonObject();

            String id = textureObj.get("id").getAsString();
            byte[] data = Base64.getDecoder().decode(textureObj.get("source").getAsString().substring("data:image/png;base64,".length()));

            textureMap.put(id, data);
        }

        return textureMap;
    }
}

package net.worldseed.resourcepack.multipart.generator;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public class TextureGenerator {
    public static Map<String, TextureData> generate(JsonArray textures, Map<String, JsonObject> mcmetas, int width, int height) {
        Map<String, TextureData> textureMap = new LinkedHashMap<>();

        for (var texture : textures) {
            JsonObject textureObj = texture.asJsonObject();

            String id = textureObj.getString("id");
            byte[] data = Base64.getDecoder().decode(textureObj.getString("source").substring("data:image/png;base64,".length()));
            String name = textureObj.getString("name");

            JsonValue uuid = textureObj.get("uuid");
            JsonObject mcmeta = null;

            if (uuid instanceof JsonString uid && mcmetas.containsKey(uid.getString())) {
                mcmeta = mcmetas.get(uid.getString());
            }

            textureMap.put(id, new TextureData(data, width, height, name, id, mcmeta));
        }

        return textureMap;
    }

    public record TextureData(byte[] value, int width, int height, String name, String id, JsonObject mcmeta) {
    }
}

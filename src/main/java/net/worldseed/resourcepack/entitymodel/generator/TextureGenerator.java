package net.worldseed.resourcepack.entitymodel.generator;

import javax.json.JsonArray;
import javax.json.JsonObject;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class TextureGenerator {
    public record TextureData(byte[] value, int width, int height, String name, String id) {}

    public static Map<String, TextureData> generate(JsonArray textures, int width, int height) {
        Map<String, TextureData> textureMap = new HashMap<>();

        for (var texture : textures) {
            JsonObject textureObj = texture.asJsonObject();

            String id = textureObj.getString("id");
            byte[] data = Base64.getDecoder().decode(textureObj.getString("source").substring("data:image/png;base64,".length()));
            String name = textureObj.getString("name");

            textureMap.put(id, new TextureData(data, width, height, name, id));
        }

        return textureMap;
    }
}

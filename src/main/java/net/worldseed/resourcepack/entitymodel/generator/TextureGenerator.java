package net.worldseed.resourcepack.entitymodel.generator;

import com.google.gson.JsonElement;

import javax.json.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class TextureGenerator {
    public record TextureData(byte[] value, int width, int height, String name, String id, JsonObject mcmeta) {}

    public static Map<String, TextureData> generate(JsonArray textures, Map<String, JsonObject> mcmetas, int width, int height) {
        Map<String, TextureData> textureMap = new HashMap<>();

        for (var texture : textures) {
            JsonObject textureObj = texture.asJsonObject();

            String id = textureObj.getString("id");
            byte[] data = Base64.getDecoder().decode(textureObj.getString("source").substring("data:image/png;base64,".length()));
            String name = textureObj.getString("name");

            JsonValue uuid = textureObj.get("uuid");
            JsonObject mcmeta = null;

            if (uuid instanceof JsonString uid && mcmetas.containsKey(uid.getString())) {
                mcmeta = mcmetas.get(uid.getString());
            } else {
                JsonValue frameTime = textureObj.get("frame_time");
                JsonValue frame_interpolate = textureObj.get("frame_interpolate");

                boolean hasValues = false;
                JsonObjectBuilder builder = Json.createObjectBuilder();

                if (frameTime instanceof JsonNumber number) {
                    hasValues = true;
                    builder.add("frametime", number);
                }

                try {
                    if (frame_interpolate instanceof JsonElement bool) {
                        hasValues = true;
                        boolean b = bool.getAsBoolean();
                        builder.add("interpolate", b);
                    }
                } catch (Exception ignored) {}

                if (hasValues) {
                    mcmeta = Json.createObjectBuilder().add("animation", builder.build()).build();
                }
            }

            textureMap.put(id, new TextureData(data, width, height, name, id, mcmeta));
        }

        return textureMap;
    }
}

package net.worldseed.resourcepack.multipart.generator;

import javax.json.*;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public class TextureGenerator {
    public record TextureData(byte[] value, int width, int height, String name, String id, JsonObject mcmeta) {
    }

    public static Map<String, TextureData> generate(JsonArray textures, Map<String, JsonObject> mcmetas, int width, int height) {
        Map<String, TextureData> textureMap = new LinkedHashMap<>();

        for (var texture : textures) {
            JsonObject textureObj = texture.asJsonObject();
            String id = textureObj.getString("id");

            var layers = texture.asJsonObject().get("layers");
            if (layers != null) {
                for (var layer : layers.asJsonArray()) {
                    Map.Entry<String, TextureData> entry = parseLayer(id, layer, mcmetas, height, width);
                    textureMap.put(entry.getKey(), entry.getValue());
                }
            } else {
                Map.Entry<String, TextureData> entry = parseLayer(id, texture, mcmetas, height, width);
                textureMap.put(entry.getKey(), entry.getValue());
            }
        }

        return textureMap;
    }

    public static Map.Entry<String, TextureData> parseLayer(String id, JsonValue texture, Map<String, JsonObject> mcmetas, int height, int width) {
        JsonObject textureObj = texture.asJsonObject();

        String source = textureObj.getString("source", textureObj.getString("data_url", ""));
        // Robustly extract embedded PNG bytes: split at the data-URL comma instead of assuming an exact
        // "data:image/png;base64," prefix (a different mime, or a linked/relative texture, otherwise
        // corrupts the decode or throws — see issue #65). Non-embedded textures can't be baked in.
        int comma = source.indexOf(',');
        byte[] data;
        if (source.startsWith("data:") && comma >= 0) {
            data = Base64.getDecoder().decode(source.substring(comma + 1));
        } else {
            System.err.println("[WSEE] texture '" + textureObj.getString("name", "?")
                    + "' has no embedded base64 data (source not a data URL); it will be blank in the pack.");
            data = new byte[0];
        }
        String name = textureObj.getString("name");

        JsonValue uuid = textureObj.get("uuid");
        JsonObject mcmeta = null;

        if (uuid instanceof JsonString uid && mcmetas.containsKey(uid.getString())) {
            mcmeta = mcmetas.get(uid.getString());
        } else {
            JsonValue frameTime = textureObj.get("frame_time");
            JsonValue frame_interpolate = textureObj.get("frame_interpolate");

            JsonValue uv_height = textureObj.get("uv_height");
            JsonValue uv_width = textureObj.get("uv_width");
            JsonValue t_height = textureObj.get("height");
            JsonValue t_width = textureObj.get("width");

            boolean hsame = uv_height instanceof JsonNumber h && t_height instanceof JsonNumber t_h && h.intValue() == t_h.intValue();
            boolean wsame = uv_width instanceof JsonNumber w && t_width instanceof JsonNumber t_w && w.intValue() == t_w.intValue();

            boolean hasValues = false;
            JsonObjectBuilder builder = Json.createObjectBuilder();

            if (frameTime instanceof JsonNumber number) {
                hasValues = true;
                builder.add("frametime", number);
            }

            try {
                if (frame_interpolate == JsonValue.FALSE) {
                    hasValues = true;
                    builder.add("interpolate", false);
                } else if (frame_interpolate == JsonValue.TRUE) {
                    hasValues = true;
                    builder.add("interpolate", true);
                }
            } catch (Exception ignored) {}

            if (hasValues && (!hsame || !wsame)) {
                mcmeta = Json.createObjectBuilder().add("animation", builder.build()).build();
            }
        }

        return Map.entry(id, new TextureData(data, width, height, name, id, mcmeta));
    }
}

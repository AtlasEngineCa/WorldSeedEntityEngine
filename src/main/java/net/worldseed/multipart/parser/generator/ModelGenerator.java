package net.worldseed.multipart.parser.generator;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Map;

public class ModelGenerator {
    static JsonArray convertDatapoints(JsonObject obj) {
        JsonArray builder = new JsonArray();

        if (obj.get("x").getAsJsonPrimitive().isString())
            builder.add(Double.parseDouble(obj.get("x").getAsString()));
        else
            builder.add(obj.get("x"));

        if (obj.get("y").getAsJsonPrimitive().isString())
            builder.add(Double.parseDouble(obj.get("y").getAsString()));
        else
            builder.add(obj.get("y"));

        if (obj.get("z").getAsJsonPrimitive().isString())
            builder.add(Double.parseDouble(obj.get("z").getAsString()));
        else
            builder.add(obj.get("z"));

        return builder;
    }

    public record BBEntityModel(JsonObject geo, JsonObject animations, Map<String, byte[]> textures, String id) {}

    public static BBEntityModel generate(JsonObject model, String id) {
        JsonObject animations = AnimationGenerator.generate(model.get("animations").getAsJsonArray());
        JsonArray bones = GeoGenerator.generate(model.get("elements").getAsJsonArray(), model.get("outliner").getAsJsonArray());
        Map<String, byte[]> textures = TextureGenerator.generate(model.get("textures").getAsJsonArray());

        double width = 16;
        double height = 16;

        if (model.has("resolution")) {
            width = model.get("resolution").getAsJsonObject().get("width").getAsDouble();
            height = model.get("resolution").getAsJsonObject().get("height").getAsDouble();
        }

        JsonObject description = new JsonObject();
        description.addProperty("identifier", "geometry.unknown");
        description.addProperty("texture_width", width);
        description.addProperty("texture_height", height);

        JsonArray i = new JsonArray();
        JsonObject j = new JsonObject();

        j.add("description", description);
        j.add("bones", bones);
        i.add(j);

        JsonObject geometry = new JsonObject();
        geometry.addProperty("format_version", "1.12.0");
        geometry.add("minecraft:geometry", i);

        JsonObject anim = new JsonObject();
        anim.addProperty("format_version", "1.8.0");
        anim.add("animations", animations);

        return new BBEntityModel(geometry, anim, textures, id);
    }
}

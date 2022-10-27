package net.worldseed.resourcepack.entitymodel.generator;

import net.worldseed.resourcepack.PackBuilder;
import net.worldseed.resourcepack.entitymodel.AdditionalStates;

import javax.json.*;
import java.io.StringReader;
import java.util.Map;

public class ModelGenerator {
    static JsonArray convertDatapoints(JsonObject obj) {
        JsonArrayBuilder builder = Json.createArrayBuilder();

        if (obj.get("x") instanceof JsonString)
            builder.add(Double.parseDouble(obj.getString("x")));
        else
            builder.add(obj.getJsonNumber("x"));

        if (obj.get("y") instanceof JsonString)
            builder.add(Double.parseDouble(obj.getString("y")));
        else
            builder.add(obj.getJsonNumber("y"));

        if (obj.get("z") instanceof JsonString)
            builder.add(Double.parseDouble(obj.getString("z")));
        else
            builder.add(obj.getJsonNumber("z"));

        return builder.build();
    }

    public record BBEntityModel(JsonObject geo, JsonObject animations, Map<String, TextureGenerator.TextureData> textures, String id, AdditionalStates additionalStates) {}

    public static BBEntityModel generate(PackBuilder.Model modelObj) {
        JsonObject model = Json.createReader(new StringReader(modelObj.data())).readObject();

        JsonObject animations = AnimationGenerator.generate(model.getJsonArray("animations"));

        int width = 16;
        int height = 16;

        if (model.getJsonObject("resolution") != null) {
            width = model.getJsonObject("resolution").getJsonNumber("width").intValue();
            height = model.getJsonObject("resolution").getJsonNumber("height").intValue();
        }

        Map<String, TextureGenerator.TextureData> textures = TextureGenerator.generate(model.getJsonArray("textures"), width, height);
        JsonArray bones = GeoGenerator.generate(model.getJsonArray("elements"), model.getJsonArray("outliner"), textures);

        JsonObject description = Json.createObjectBuilder()
                .add("identifier", "geometry.unknown")
                .add("texture_width", width)
                .add("texture_height", height)
                .build();

        JsonObject geometry = Json.createObjectBuilder()
                .add("format_version", "1.12.0")
                .add("minecraft:geometry", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("description", description)
                                .add("bones", bones)
                        )
                ).build();

        var anim = Json.createObjectBuilder()
                .add("format_version", "1.8.0")
                .add("animations", animations)
                .build();

        if (modelObj.additionalStates() != null) {
            return new BBEntityModel(geometry, anim, textures, modelObj.name(), new AdditionalStates(modelObj.additionalStates(), textures));
        }

        return new BBEntityModel(geometry, anim, textures, modelObj.name(), AdditionalStates.EMPTY());
    }
}

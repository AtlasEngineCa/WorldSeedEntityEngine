package net.worldseed.resourcepack.multipart.generator;

import net.worldseed.resourcepack.PackBuilder;
import net.worldseed.resourcepack.multipart.AdditionalStates;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

public class ModelGenerator {
    public static BBEntityModel generate(PackBuilder.Model modelObj) {
        JsonObject model = Json.createReader(new StringReader(modelObj.data())).readObject();

        JsonObject animations = AnimationGenerator.generate(model.getJsonArray("animations"));

        int width = 16;
        int height = 16;

        if (model.getJsonObject("resolution") != null) {
            width = model.getJsonObject("resolution").getJsonNumber("width").intValue();
            height = model.getJsonObject("resolution").getJsonNumber("height").intValue();
        }

        Map<String, JsonObject> mcmetas = new HashMap<>();
        if (model.getJsonObject("mcmetas") != null) {
            for (var mcmeta : model.getJsonObject("mcmetas").entrySet()) {
                mcmetas.put(mcmeta.getKey(), mcmeta.getValue().asJsonObject());
            }
        }

        Map<String, TextureGenerator.TextureData> textures = TextureGenerator.generate(model.getJsonArray("textures"), mcmetas, width, height);
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

    public record BBEntityModel(JsonObject geo, JsonObject animations,
                                Map<String, TextureGenerator.TextureData> textures, String id,
                                AdditionalStates additionalStates) {
    }
}

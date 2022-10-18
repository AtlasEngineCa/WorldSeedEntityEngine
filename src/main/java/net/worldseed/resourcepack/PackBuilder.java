package net.worldseed.resourcepack;

import net.worldseed.resourcepack.entitymodel.generator.ModelGenerator;
import net.worldseed.resourcepack.entitymodel.parser.ModelParser;
import org.apache.commons.io.FileUtils;

import javax.imageio.ImageIO;
import javax.json.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

public class PackBuilder {

    public record Model(String data, String name, JsonObject additionalStates) {}
    public record ConfigJson(String modelMappings) {}
    private record BBenchResult(JsonObject json, Map<String, byte[]> textures) {}

    private static BBenchResult BBenchToJson(JsonObject bbmodel, int textureId) {
        JsonArrayBuilder fixedElements = Json.createArrayBuilder();
        JsonArray fixedTextureSize = Json.createArrayBuilder()
            .add(16)
            .add(16)
            .build();

        if (bbmodel.containsKey("resolution")) {
            var textureSize = bbmodel.getJsonObject("resolution");

            fixedTextureSize = Json.createArrayBuilder()
                .add(textureSize.getInt("width"))
                .add(textureSize.getInt("height"))
                .build();
        }

        for (JsonValue element : bbmodel.getJsonArray("elements")) {
            JsonObject elementJson = element.asJsonObject();
            JsonObjectBuilder facesFixed = Json.createObjectBuilder();

            double inflate = 0;

            try {
                inflate = Math.round(elementJson.getJsonNumber("inflate").doubleValue() * 10000) / 10000.0;
            } catch (Exception ignored) { }

            JsonObject faces = elementJson.getJsonObject("faces");
            for (var faceObj : faces.entrySet()) {
                String face = faceObj.getKey();
                JsonObject faceJson = faceObj.getValue().asJsonObject();
                JsonObjectBuilder faceFixed = Json.createObjectBuilder(faceJson);

                String texture = "#0";

                if (faceJson.containsKey("texture")) {
                    texture = "#" + faceJson.getInt("texture");
                }

                faceFixed.add("texture", texture);
                facesFixed.add(face, faceFixed.build());
            }

            JsonObject fixedRotation = Json.createObjectBuilder()
                .add("axis", "y")
                .add("angle", 0)
                .add("origin", elementJson.getJsonArray("origin"))
                .build();

            if (elementJson.containsKey("rotation")) {
                JsonArray rotation = elementJson.getJsonArray("rotation");

                String axis = "";
                double angle = 0;

                if (rotation.getJsonNumber(0).doubleValue() != 0) {
                    axis = "x";
                    angle = rotation.getJsonNumber(0).doubleValue();
                } else if (rotation.getJsonNumber(1).doubleValue() != 0) {
                    axis = "y";
                    angle = rotation.getJsonNumber(1).doubleValue();
                } else if (rotation.getJsonNumber(2).doubleValue() != 0) {
                    axis = "z";
                    angle = rotation.getJsonNumber(2).doubleValue();
                }

                fixedRotation = Json.createObjectBuilder()
                    .add("axis", axis)
                    .add("angle", angle)
                    .add("origin", elementJson.getJsonArray("origin"))
                    .build();
            }

            JsonObject newElement = Json.createObjectBuilder()
                    .add("from", applyInflate(elementJson.getJsonArray("from"), inflate))
                    .add("to", applyInflate(elementJson.getJsonArray("to"), -inflate))
                    .add("rotation", fixedRotation)
                    .add("faces", facesFixed.build())
                    .build();

            fixedElements.add(newElement);
        }

        int offsetLength = "data:image/png;base64,".length();

        Map<String, byte[]> textures = new HashMap<>();
        JsonObjectBuilder newTextures = Json.createObjectBuilder();

        for (JsonValue texture : bbmodel.getJsonArray("textures")) {
            JsonObject textureJson = texture.asJsonObject();
            byte[] textureData = Base64.getDecoder().decode(textureJson.getString("source").substring(offsetLength));
            textures.put(textureJson.getString("id"), textureData);
            newTextures.add(String.valueOf(textureJson.getString("id")), "worldseed:item/" + textureId + "/" + textureJson.getString("id"));
        }

        return new BBenchResult(Json.createObjectBuilder()
            .add("textures", newTextures.build())
            .add("elements", fixedElements.build())
            .add("texture_size", fixedTextureSize)
            .add("display", bbmodel.getJsonObject("display"))
                .build(), textures);
    }

    public static JsonArray applyInflate(JsonArray from, double inflate) {
        JsonArrayBuilder inflated = Json.createArrayBuilder();
        for (int i = 0; i < from.size(); ++i) {
            double val = from.getJsonNumber(i).doubleValue() + inflate;
            inflated.add(val);
        }
        return inflated.build();
    }

    public static ConfigJson Generate(Path bbmodel, Path resourcepack, Path modelDataPath) throws Exception {
        Map<String, JsonObject> additionalStateFiles = new HashMap<>();
        Arrays.stream(bbmodel.toFile().listFiles()).filter(File::isFile)
                .filter(file -> file.getName().endsWith(".states"))
                .forEach(file -> {
                    try {
                        JsonObject m = Json.createReader(new FileInputStream(file)).readObject();
                        additionalStateFiles.put(file.getName().replace(".states", ""), m);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

        List<Model> entityModels = Arrays.stream(bbmodel.toFile().listFiles())
                .filter(File::isFile)
                .filter(file -> file.getName().endsWith(".bbmodel"))
                .map(entityModel -> {
            try {
                return new Model(FileUtils.readFileToString(entityModel, StandardCharsets.UTF_8), entityModel.getName(), additionalStateFiles.get(entityModel.getName()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).toList();

        Path texturePathMobs = resourcepack.resolve("assets/worldseed/textures/mobs/");
        Path modelPathMobs = resourcepack.resolve("assets/worldseed/models/mobs/");
        Path baseModelPath = resourcepack.resolve("assets/minecraft/models/item/");

        texturePathMobs.toFile().mkdirs();
        modelPathMobs.toFile().mkdirs();
        resourcepack.toFile().mkdirs();

        JsonObject modelMappings = writeCustomModels(entityModels, modelDataPath, texturePathMobs, modelPathMobs, baseModelPath);

        return new ConfigJson(modelMappings.toString());
    }

    public static String buildPredicate(JsonObject predicate) {
        List<String> res = new ArrayList<>();

        for (var entry : predicate.entrySet()) {
            String key = entry.getKey();
            String value = switch (((JsonString) entry.getValue()).getString()) {
                case "bass_drum" -> "basedrum";
                case "snare_drum" -> "snare";
                case "bass_guitar" -> "bass";
                default -> ((JsonString) entry.getValue()).getString();
            };

            res.add(key + "=" + value);
        }

        return String.join(",", res);
    }

    private static JsonObject writeCustomModels(List<Model> entityModels, Path modelDataPath, Path texturePathMobs, Path modelPathMobs, Path baseModelPath) throws Exception {
        List<ModelGenerator.BBEntityModel> res = new ArrayList<>();
        JsonObjectBuilder thumbnailMap = Json.createObjectBuilder();
        thumbnailMap.add("parent", "item/generated");
        thumbnailMap.add("textures", Json.createObjectBuilder().add("layer0", "minecraft:item/ink_sac").build());

        JsonArrayBuilder overrides = Json.createArrayBuilder();

        for (Model entityModel : entityModels) {
            ModelGenerator.BBEntityModel bbModel = ModelGenerator.generate(entityModel);
            FileUtils.writeStringToFile(modelDataPath.resolve(bbModel.id() + "/model.animation.json").toFile(), bbModel.animations().toString(), Charset.defaultCharset());
            FileUtils.writeStringToFile(modelDataPath.resolve(bbModel.id() + "/model.geo.json").toFile(), bbModel.geo().toString(), Charset.defaultCharset());

            res.add(bbModel);
        }

        thumbnailMap.add("overrides", overrides.build());
        FileUtils.writeStringToFile(baseModelPath.resolve("ink_sac.json").toFile(), thumbnailMap.build().toString(), Charset.defaultCharset());

        ModelParser.ModelEngineFiles modelData = ModelParser.parse(res, modelPathMobs);

        modelData.models().forEach(model -> {
            for (var entry : model.textures().entrySet()) {
                Path resolvedPath = texturePathMobs.resolve(model.id() + "/" + model.state().name() + "/" + entry.getKey());

                try {
                    BufferedImage buf = ImageIO.read(new ByteArrayInputStream(entry.getValue()));
                    int h = buf.getHeight();
                    int w = buf.getWidth();

                    double scale = ((double) h) / w;
                    boolean animated = (int) scale == scale;

                    if (animated && scale > 1 && h != model.textureHeight()) {
                        int frametime = 2;
                        JsonObjectBuilder builder = Json.createObjectBuilder();
                        builder.add("animation",
                                Json.createObjectBuilder().add("frametime", frametime).build());

                        FileUtils.writeStringToFile(new File(resolvedPath + ".png.mcmeta"), builder.build().toString(), StandardCharsets.UTF_8);
                    }

                    FileUtils.writeByteArrayToFile(new File(resolvedPath + ".png"), entry.getValue());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            for (var entry : model.bones().entrySet()) {
                try {
                    FileUtils.writeStringToFile(modelPathMobs.resolve(model.id() + "/" + model.state().name() + "/" + entry.getKey()).toFile(), entry.getValue().toString(), Charset.defaultCharset());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        FileUtils.writeStringToFile(baseModelPath.resolve("leather_horse_armor.json").toFile(), modelData.binding().toString(), Charset.defaultCharset());
        return modelData.mappings();
    }
}

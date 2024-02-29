package net.worldseed.resourcepack;

import net.worldseed.multipart.ModelEngine;
import net.worldseed.resourcepack.entitymodel.generator.ModelGenerator;
import net.worldseed.resourcepack.entitymodel.generator.TextureGenerator;
import net.worldseed.resourcepack.entitymodel.parser.ModelParser;
import org.apache.commons.io.FileUtils;

import javax.json.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PackBuilder {
    public record Model(String data, String name, JsonObject additionalStates) {}
    public record ConfigJson(String modelMappings) {}

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

        List<Model> entityModels = recursiveFileSearch(bbmodel, Path.of(""), additionalStateFiles);

        Path texturePathMobs = resourcepack.resolve("assets/worldseed/textures/mobs/");
        Path modelPathMobs = resourcepack.resolve("assets/worldseed/models/mobs/");
        Path baseModelPath = resourcepack.resolve("assets/minecraft/models/item/");

        texturePathMobs.toFile().mkdirs();
        modelPathMobs.toFile().mkdirs();
        resourcepack.toFile().mkdirs();

        JsonObject modelMappings = writeCustomModels(entityModels, modelDataPath, texturePathMobs, modelPathMobs, baseModelPath);

        return new ConfigJson(modelMappings.toString());
    }

    private static List<Model> recursiveFileSearch(Path path, Path subPath, Map<String, JsonObject> additionalStateFiles) {
        var files = Arrays.stream(path.toFile().listFiles())
                .filter(File::isFile)
                .filter(file -> file.getName().endsWith(".bbmodel"))
                .map(entityModel -> {
                    try {
                        String pathName = subPath.resolve(entityModel.getName()).toString();
                        File stateFile = subPath.resolve(path).resolve(Path.of(entityModel.getName() + ".states")).toFile();

                        if (stateFile.exists()) {
                            JsonObject m = Json.createReader(new FileInputStream(stateFile)).readObject();
                            additionalStateFiles.put(pathName, m);
                        }

                        return new Model(FileUtils.readFileToString(entityModel, StandardCharsets.UTF_8), pathName, additionalStateFiles.get(entityModel.getName()));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).toList();

        var dirs = Arrays.stream(path.toFile().listFiles())
                .filter(File::isDirectory)
                .map(file -> recursiveFileSearch(file.toPath(), subPath.resolve(file.getName()), additionalStateFiles))
                .flatMap(List::stream)
                .toList();

        return List.of(files, dirs).stream().flatMap(List::stream).toList();
    }

    private static JsonObject writeCustomModels(List<Model> entityModels, Path modelDataPath, Path texturePathMobs, Path modelPathMobs, Path baseModelPath) throws Exception {
        Map<String, ModelGenerator.BBEntityModel> res = new HashMap<>();
        JsonObjectBuilder thumbnailMap = Json.createObjectBuilder();
        thumbnailMap.add("parent", "item/generated");
        thumbnailMap.add("textures", Json.createObjectBuilder().add("layer0", "minecraft:item/ink_sac").build());

        JsonArrayBuilder overrides = Json.createArrayBuilder();

        for (Model entityModel : entityModels) {
            ModelGenerator.BBEntityModel bbModel = ModelGenerator.generate(entityModel);
            FileUtils.writeStringToFile(modelDataPath.resolve(bbModel.id() + "/model.animation.json").toFile(), bbModel.animations().toString(), Charset.defaultCharset());
            FileUtils.writeStringToFile(modelDataPath.resolve(bbModel.id() + "/model.geo.json").toFile(), bbModel.geo().toString(), Charset.defaultCharset());

            res.put(bbModel.id(), bbModel);
        }

        thumbnailMap.add("overrides", overrides.build());
        FileUtils.writeStringToFile(baseModelPath.resolve("ink_sac.json").toFile(), thumbnailMap.build().toString(), Charset.defaultCharset());

        ModelParser.ModelEngineFiles modelData = ModelParser.parse(res.values(), modelPathMobs);

        modelData.models().forEach(model -> {
            var textureData = res.get(model.id()).textures();

            for (var entry : model.textures().entrySet()) {
                TextureGenerator.TextureData found = textureData.get(entry.getKey());
                Path resolvedPath = texturePathMobs.resolve(model.id() + "/" + model.state().name() + "/" + entry.getKey());

                try {
                    if (found.mcmeta() != null) {
                        FileUtils.writeStringToFile(new File(resolvedPath + ".png.mcmeta"), found.mcmeta().toString(), StandardCharsets.UTF_8);
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

        final String itemName = ModelEngine.getModelMaterial().name().replace("minecraft:", "");
        FileUtils.writeStringToFile(baseModelPath.resolve(itemName + ".json").toFile(), modelData.binding().toString(), Charset.defaultCharset());
        return modelData.mappings();
    }
}

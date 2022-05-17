package net.worldseed.multipart.parser;

import com.google.gson.*;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.worldseed.multipart.ModelEngine;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import javax.naming.SizeLimitExceededException;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class ModelParser {
    protected static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static int index = 0;

    private static final Map<String, MappingEntry> mappings = new HashMap<>();
    private static final List<JsonObject> predicates = new ArrayList<>();

    final static JsonObject display = new JsonObject();
    static {
        JsonArray translation = new JsonArray();
        translation.add(0);
        translation.add(0);
        translation.add(0);

        JsonArray scale = new JsonArray();
        scale.add(-4);
        scale.add(4);
        scale.add(-4);

        JsonObject head = new JsonObject();
        head.add("translation", translation);
        head.add("scale", scale);
        display.add("head", head);
    }

    public static void parse(Path outputPath, Path modelsPath, Path dataPath) throws IOException, NoSuchAlgorithmException, SizeLimitExceededException {
        createFiles("gem_golem", modelsPath, outputPath);

        var textures = new JsonObject();
        textures.addProperty("layer0", "minecraft:item/leather_horse_armor");

        var leather_armour_file = new JsonObject();
        leather_armour_file.addProperty("parent", "item/generated");
        leather_armour_file.add("textures", textures);
        leather_armour_file.add("overrides", predicatesToJson());

        var output = outputPath.resolve("../minecraft/models/item/leather_horse_armor.json").toFile();
        output.getParentFile().mkdirs();
        try (var writer = new FileWriter(output)) {
            GSON.toJson(leather_armour_file, writer);
        }

        var mappingsFile = dataPath.resolve("model_mappings.json").toFile();
        try (var writer = new FileWriter(mappingsFile)) {
            GSON.toJson(mappingsToJson(), writer);
        }
    }

    private static UV convertUV(UV uv, int width, int height, boolean inverse) {
        double sx = uv.x1 * (16.0 / width);
        double sy = uv.y1 * (16.0 / height);
        double ex = uv.x2 * (16.0 / width);
        double ey = uv.y2 * (16.0 / height);

        if (inverse)
            return new UV(ex+sx, ey+sy, sx, sy);
        return new UV(sx, sy, ex + sx, ey + sy);
    }

    private static JsonObject mappingsToJson() {
        JsonObject res = new JsonObject();

        for (Map.Entry<String, MappingEntry> entry : ModelParser.mappings.entrySet()) {
            var id = entry.getKey();
            var mapping = entry.getValue();

            res.add(id, mappingEntryToJson(mapping));
        }

        return res;
    }

    private static JsonElement mappingEntryToJson(MappingEntry mapping) {
        JsonObject res = new JsonObject();

        res.add("id", entrySetToJson(mapping.map));
        res.add("offset", pointAsJson(mapping.offset));
        res.add("diff", pointAsJson(mapping.diff));

        return res;
    }

    private static JsonElement entrySetToJson(Map<String, Integer> map) {
        JsonObject res = new JsonObject();

        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            res.addProperty(entry.getKey(), entry.getValue());
        }

        return res;
    }

    private static JsonElement predicatesToJson() {
        var array = new JsonArray();
        for (var predicate : ModelParser.predicates) {
            array.add(predicate);
        }
        return array;
    }

    record Cube(Point origin, Point size, Point pivot, Point rotation, Map<TextureFace, UV> uv) {}
    record MappingEntry(Map<String, Integer> map, Point offset, Point diff) {}
    record Bone(String name, List<Cube> cubes) {}
    record ItemId(String name, String bone, Point offset, Point diff, int id) {}
    record UV(double x1, double y1, double x2, double y2) {}
    record RotationInfo(double angle, String axis, Point origin) {}
    record Element(Point from, Point to, Map<TextureFace, UV> faces, RotationInfo rotation) {
        public JsonObject asJson() {
            JsonObject res = new JsonObject();
            res.add("from", pointAsJson(from));
            res.add("to", pointAsJson(to));
            res.add("faces", facesAsJson(faces));
            res.add("rotation", rotationAsJson(rotation));

            return res;
        }
    }

    private static JsonElement rotationAsJson(RotationInfo rotation) {
        JsonObject res = new JsonObject();
        res.addProperty("angle", rotation.angle);
        res.addProperty("axis", rotation.axis);
        res.add("origin", pointAsJson(rotation.origin));
        return res;
    }

    private static JsonElement facesAsJson(Map<TextureFace, UV> faces) {
        JsonObject res = new JsonObject();
        for (TextureFace face : faces.keySet()) {
            res.add(face.name(), uvAsJson(faces.get(face)));
        }
        return res;
    }

    private static JsonElement uvAsJson(UV uv) {
        JsonArray els = new JsonArray();
        els.add(new JsonPrimitive(uv.x1));
        els.add(new JsonPrimitive(uv.y1));
        els.add(new JsonPrimitive(uv.x2));
        els.add(new JsonPrimitive(uv.y2));

        JsonObject res = new JsonObject();

        res.add("uv", els);
        res.addProperty("texture", "#0");
        return res;
    }

    private static JsonArray pointAsJson(Point from) {
        JsonArray res = new JsonArray();
        res.add(from.x());
        res.add(from.y());
        res.add(from.z());
        return res;
    }

    public static Map<ModelParser.TextureFace, ModelParser.UV> getUV(JsonObject uv) {
        Map<TextureFace, UV> res = new HashMap<>();

        for (TextureFace face : TextureFace.values()) {
            String faceName = face.name();

            JsonObject north = uv.get(faceName).getAsJsonObject();
            JsonArray north_uv = north.get("uv").getAsJsonArray();
            JsonArray north_size = north.get("uv_size").getAsJsonArray();
            UV uvNorth = new UV(north_uv.get(0).getAsDouble(), north_uv.get(1).getAsDouble(), north_size.get(0).getAsDouble(), north_size.get(1).getAsDouble());
            res.put(face, uvNorth);
        }

        return res;
    }

    private static void createFiles(@NotNull String modelName, Path modelPath, Path outputPath) throws IOException, NoSuchAlgorithmException, SizeLimitExceededException {
        List<TextureState> toGenerate = List.of(TextureState.hit, TextureState.normal);
        HashMap<String, JsonObject> modelInfo = new HashMap<>();

        Path geoFile = modelPath.resolve(modelName).resolve("model.geo.json");
        Path texturePath = modelPath.resolve(modelName).resolve("texture.png");

        BufferedImage texture = ImageIO.read(texturePath.toFile());

        int textureHeight = 16;
        int textureWidth = 16;

        JsonObject modelGeoFile = GSON
                .fromJson(new InputStreamReader(new FileInputStream(geoFile.toFile())), JsonObject.class)
                .get("minecraft:geometry").getAsJsonArray().get(0).getAsJsonObject();

        JsonArray bonesJson = modelGeoFile.get("bones").getAsJsonArray();
        JsonObject description = modelGeoFile.get("description").getAsJsonObject();

        if (description.has("texture_height"))
            textureHeight = description.get("texture_height").getAsInt();
        if (description.has("texture_width"))
            textureWidth = description.get("texture_width").getAsInt();

        JsonArray textureSize = new JsonArray();
        textureSize.add(textureWidth);
        textureSize.add(textureHeight);

        List<Bone> bones = new ArrayList<>();
        for (JsonElement bone : bonesJson) {
            if (!bone.getAsJsonObject().has("cubes")) continue;
            String name = bone.getAsJsonObject().get("name").getAsString();

            List<Cube> cubes = new ArrayList<>();
            for (JsonElement cubeJson : bone.getAsJsonObject().get("cubes").getAsJsonArray()) {
                Optional<Point> origin = ModelEngine.getPos(cubeJson.getAsJsonObject().get("origin").getAsJsonArray());
                Optional<Point> size = ModelEngine.getPos(cubeJson.getAsJsonObject().get("size").getAsJsonArray());

                Optional<Point> pivot = Optional.empty();
                if (cubeJson.getAsJsonObject().has("pivot"))
                    pivot = ModelEngine.getPos(cubeJson.getAsJsonObject().get("pivot").getAsJsonArray());

                Optional<Point> rotation = Optional.empty();
                if (cubeJson.getAsJsonObject().has("rotation"))
                    rotation = ModelEngine.getPos(cubeJson.getAsJsonObject().get("rotation").getAsJsonArray());

                Map<TextureFace, UV> uv = getUV(cubeJson.getAsJsonObject().get("uv").getAsJsonObject());

                if (origin.isPresent() && size.isPresent()) {
                    Cube cube = new Cube(origin.get().withX(-origin.get().x() - size.get().x()), size.get(), pivot.orElse(Pos.ZERO), rotation.orElse(Pos.ZERO), uv);
                    cubes.add(cube);
                }
            }

            if (cubes.size() > 0) {
                bones.add(new Bone(name, cubes));
            }
        }

        for (TextureState state : toGenerate) {
            List<ItemId> itemIds = new ArrayList<>();

            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] toHash = (modelName + "/" + state.ordinal()).getBytes(StandardCharsets.UTF_8);
            byte[] array = md.digest(toHash);

            StringBuilder sb = new StringBuilder();
            for (byte b : array) {
                sb.append(Integer.toHexString((b & 0xFF) | 0x100), 1, 3);
            }

            String uuid = sb.toString();
            Path outputTexturePath = outputPath.resolve("textures/mobs/" + modelName + "/" + state.name());
            Path outputModelPath = outputPath.resolve("models/mobs/" + modelName + "/" + state.name());

            outputModelPath.toFile().mkdirs();
            outputTexturePath.toFile().mkdirs();

            JsonObject modelTextureJson = new JsonObject();
            modelTextureJson.addProperty("0", "wsee:mobs/" + modelName + "/" + state.name() + "/" + uuid);

            BufferedImage stateTexture = state.multiplyColour(texture);
            ImageIO.write(stateTexture, "png", outputTexturePath.resolve(uuid + ".png").toFile());

            for (Bone bone : bones) {
                String boneName = bone.name;

                List<Element> elements = new ArrayList<>();
                double cubeMinX = 100000;
                double cubeMinY = 100000;
                double cubeMinZ = 100000;

                double cubeMaxX = -100000;
                double cubeMaxY = -100000;
                double cubeMaxZ = -100000;

                for (Cube cube : bone.cubes) {
                    Point cubeOrigin = cube.origin;
                    Point cubeSize = cube.size;

                    cubeMinX = Math.min(cubeMinX, cubeOrigin.x());
                    cubeMinY = Math.min(cubeMinY, cubeOrigin.y());
                    cubeMinZ = Math.min(cubeMinZ, cubeOrigin.z());

                    cubeMaxX = Math.max(cubeMaxX, cubeOrigin.x() + cubeSize.x());
                    cubeMaxY = Math.max(cubeMaxY, cubeOrigin.y() + cubeSize.y());
                    cubeMaxZ = Math.max(cubeMaxZ, cubeOrigin.z() + cubeSize.z());
                }

                final Point cubeMid = new Vec((cubeMaxX + cubeMinX) / 2 - 8, (cubeMaxY + cubeMinY) / 2 - 8, (cubeMaxZ + cubeMinZ) / 2 - 8);
                final Point cubeDiff = new Vec(cubeMid.x() - cubeMinX + 16, cubeMid.y() - cubeMinY + 16, cubeMid.z() - cubeMinZ + 16);

                if (cubeMaxX > 47)
                    throw new SizeLimitExceededException("Cube size exceeded: " + boneName + " max X");
                if (cubeMaxY > 47)
                    throw new SizeLimitExceededException("Cube size exceeded: " + boneName + " max Y");
                if (cubeMaxZ > 47)
                    throw new SizeLimitExceededException("Cube size exceeded: " + boneName + " max Z");
                if (cubeMinX < -15)
                    throw new SizeLimitExceededException("Cube size exceeded: " + boneName + " min X");
                if (cubeMinY < -15)
                    throw new SizeLimitExceededException("Cube size exceeded: " + boneName + " min Y");
                if (cubeMinZ < -15)
                    throw new SizeLimitExceededException("Cube size exceeded: " + boneName + " min Z");

                for (Cube cube : bone.cubes()) {
                    Point cubePivot = new Vec(-(cube.pivot().x() + cubeMid.x()), cube.pivot.y() - cubeMid.y(), cube.pivot.z() - cubeMid.z());
                    Point cubeSize = cube.size;
                    Point cubeOrigin = cube.origin;

                    Point cubeFrom = new Vec(cubeOrigin.x() - cubeMid.x(), cubeOrigin.y() - cubeMid.y(), cubeOrigin.z() - cubeMid.z());
                    Point cubeTo = new Vec(cubeFrom.x() + cubeSize.x(), cubeFrom.y() + cubeSize.y(), cubeFrom.z() + cubeSize.z());

                    Point cubeRotation = new Vec(-cube.rotation.x(), -cube.rotation.y(), cube.rotation.z());
                    HashMap<TextureFace, UV> uvs = new HashMap<>();

                    for (TextureFace face : cube.uv.keySet()) {
                        UV newUv = convertUV(cube.uv.get(face), textureWidth, textureHeight, face == TextureFace.up || face == TextureFace.down);
                        uvs.put(face, newUv);
                    }

                    double rotationAmount = 0;
                    String rotationAxis = "z";

                    if (cubeRotation.x() != 45 && cubeRotation.x() != -22.5 && cubeRotation.x() != 22.5 && cubeRotation.x() != -45 && cubeRotation.x() != 0) {
                        throw new IllegalArgumentException("Invalid rotation: " + boneName + " X");
                    }
                    if (cubeRotation.y() != 45 && cubeRotation.y() != -22.5 && cubeRotation.y() != 22.5 && cubeRotation.y() != -45 && cubeRotation.y() != 0) {
                        throw new IllegalArgumentException("Invalid rotation: " + boneName + " Y");
                    }
                    if (cubeRotation.z() != 45 && cubeRotation.z() != -22.5 && cubeRotation.z() != 22.5 && cubeRotation.z() != -45 && cubeRotation.z() != 0) {
                        throw new IllegalArgumentException("Invalid rotation: " + boneName + " Z");
                    }

                    if (cubeRotation.x() != 0) {
                        rotationAmount = cubeRotation.x();
                        rotationAxis = "x";
                    }

                    if (cubeRotation.y() != 0) {
                        if (rotationAmount != 0) {
                            throw new IllegalArgumentException("Cannot rotate on multiple axis: " + boneName + " Y");
                        }

                        rotationAmount = cubeRotation.y();
                        rotationAxis = "y";
                    }

                    if (cubeRotation.z() != 0) {
                        if (rotationAmount != 0) {
                            throw new IllegalArgumentException("Cannot rotate on multiple axis: " + boneName + " Z");
                        }

                        rotationAmount = cubeRotation.z();
                        rotationAxis = "z";
                    }

                    RotationInfo rotationInfo = new RotationInfo(rotationAmount, rotationAxis, cubePivot);
                    Element newElement = new Element(cubeFrom, cubeTo, uvs, rotationInfo);
                    elements.add(newElement);

                    itemIds.add(
                        new ItemId(modelName,
                        boneName,
                        new Vec(cubeMinX + cubeDiff.x() - 8, cubeMinY + cubeDiff.y() - 8, cubeMinZ + cubeDiff.z() - 8),
                        cubeDiff,
                        index)
                    );

                    index++;

                    JsonObject boneInfo = new JsonObject();
                    boneInfo.add("textures", modelTextureJson);
                    boneInfo.add("elements", elementsToJson(elements));
                    boneInfo.add("texture_size", textureSize);
                    boneInfo.add("display", display);
                    modelInfo.put(boneName + ".json", boneInfo);
                }
            }

            for (Map.Entry<String, JsonObject> modelData : modelInfo.entrySet()) {
                String fileName = modelData.getKey();
                JsonObject modelJson = modelData.getValue();

                File modelFile = outputModelPath.resolve(fileName).toFile();
                try (FileWriter fileWriter = new FileWriter(modelFile)) {
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    gson.toJson(modelJson, fileWriter);

                    fileWriter.flush();
                }
            }

            for (var item : itemIds) {
                if (!mappings.containsKey((item.name + "/" + item.bone))) {
                    mappings.put(item.name + "/" + item.bone, new MappingEntry(new HashMap<>(), item.offset, item.diff));
                }

                mappings.get(item.name + "/" + item.bone).map.put(state.name(), item.id);
                predicates.add(createPredicate(item.id, item.name, state.name(), item.bone));
            }
        }
    }

    private static JsonObject createPredicate(int id, String name, String state, String bone) {
        JsonObject res = new JsonObject();

        JsonObject customModelData = new JsonObject();
        customModelData.addProperty("custom_model_data", id);

        res.add("predicate", customModelData);
        res.addProperty("model", "wsee:mobs/" + name + "/" + state + "/" + bone);

        return res;
    }

    private static JsonElement elementsToJson(List<Element> elements) {
        JsonArray res = new JsonArray();

        for (Element element : elements) {
            res.add(element.asJson());
        }

        return res;
    }

    private enum TextureFace {
        north,
        east,
        south,
        west,
        up,
        down
    }

    private enum TextureState {
        normal(1.0,1.0,1.0),
        hit(2.0,0.7,0.7);

        final private double r;
        final private double g;
        final private double b;

        TextureState(double r, double g, double b) {
            this.r = r;
            this.g = g;
            this.b = b;
        }

        private BufferedImage multiplyColour(BufferedImage oldImg) {
            ColorModel cm = oldImg.getColorModel();
            boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
            WritableRaster raster = oldImg.copyData(null);
            BufferedImage img = new BufferedImage(cm, raster, isAlphaPremultiplied, null);

            for (int i = 0; i < img.getWidth(); i++) {
                for (int j = 0; j < img.getHeight(); j++) {
                    int rgb = img.getRGB(i, j);
                    int ir = (rgb >> 16) & 0xFF;
                    int ig = (rgb >> 8) & 0xFF;
                    int ib = rgb & 0xFF;
                    int ia = (rgb >> 24) & 0xFF;
                    img.setRGB(i, j, (ia << 24) | (Math.min((int)(r * ir), 255) << 16) | (Math.min((int)(g * ig), 255) << 8) | Math.min((int)(b * ib), 255));
                }
            }

            return img;
        }
    }
}

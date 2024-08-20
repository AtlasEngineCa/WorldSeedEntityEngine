package net.worldseed.resourcepack.multipart.parser;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.worldseed.resourcepack.multipart.generator.ModelGenerator;
import net.worldseed.resourcepack.multipart.generator.TextureGenerator;
import org.apache.commons.io.FileUtils;

import javax.imageio.ImageIO;
import javax.json.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.*;

public class ModelParser {
    private static final Map<String, MappingEntry> mappings = new HashMap<>();
    private static final List<JsonObject> predicates = new ArrayList<>();
    private static int index = 0;

    private static JsonObject display(Point offset) {
        JsonArrayBuilder translationHead = Json.createArrayBuilder();
        translationHead.add(offset.x() * -4);
        translationHead.add(offset.y() * 4 - 6.5);
        translationHead.add(offset.z() * -4);

        JsonArrayBuilder translationDisplay = Json.createArrayBuilder();
        translationDisplay.add(offset.x() * -4);
        translationDisplay.add(offset.y() * 4);
        translationDisplay.add(offset.z() * -4);

        JsonArrayBuilder translationArm = Json.createArrayBuilder();
        translationArm.add(offset.x() * -4 - 1);
        translationArm.add(offset.z() * 4 - 2);
        translationArm.add(offset.y() * 4 + 10);

        JsonArray translationHeadBuilt = translationHead.build();
        JsonArray translationArmBuilt = translationArm.build();
        JsonArray translationDisplayBuilt = translationDisplay.build();

        JsonArrayBuilder scaleHead = Json.createArrayBuilder();
        scaleHead.add(-4);
        scaleHead.add(4);
        scaleHead.add(-4);

        JsonArrayBuilder scaleArm = Json.createArrayBuilder();
        scaleArm.add(4);
        scaleArm.add(4);
        scaleArm.add(4);

        JsonArray scaleHeadBuilt = scaleHead.build();
        JsonArray scaleArmBuilt = scaleArm.build();

        JsonArrayBuilder rotationArm = Json.createArrayBuilder();
        rotationArm.add(90);
        rotationArm.add(180);
        rotationArm.add(0);

        JsonObjectBuilder head = Json.createObjectBuilder();
        head.add("translation", translationHeadBuilt);
        head.add("scale", scaleHeadBuilt);

        JsonObjectBuilder arm = Json.createObjectBuilder();
        arm.add("rotation", rotationArm);
        arm.add("translation", translationArmBuilt);
        arm.add("scale", scaleArmBuilt);

        JsonObjectBuilder display = Json.createObjectBuilder();
        display.add("translation", translationDisplayBuilt);
        display.add("scale", scaleHeadBuilt);

        JsonObject builtHead = head.build();
        JsonObject builtArm = arm.build();
        JsonObject builtDisplay = display.build();

        return Json.createObjectBuilder()
                .add("head", builtHead)
                .add("thirdperson_righthand", builtArm)
                .add("thirdperson_lefthand", builtDisplay)
                .build();
    }

    public static Optional<Point> getPos(JsonArray pivot) {
        if (pivot == null) return Optional.empty();
        else {
            JsonArray arr = pivot.asJsonArray();
            return Optional.of(new Vec(arr.getJsonNumber(0).doubleValue(), arr.getJsonNumber(1).doubleValue(), arr.getJsonNumber(2).doubleValue()));
        }
    }

    public static ModelEngineFiles parse(Collection<ModelGenerator.BBEntityModel> data, Path modelPathMobs) throws Exception {
        List<ModelFile> models = new ArrayList<>();

        index = 0;
        predicates.clear();
        mappings.clear();

        for (var folder : data) {
            models.addAll(createFiles(folder, modelPathMobs));
        }

        var textures = Json.createObjectBuilder();
        textures.add("layer0", "minecraft:item/leather_horse_armor");

        var leather_armour_file = Json.createObjectBuilder();
        leather_armour_file.add("parent", "item/generated");
        leather_armour_file.add("textures", textures);
        leather_armour_file.add("overrides", predicatesToJson());

        JsonObject armourFile = leather_armour_file.build();

        return new ModelEngineFiles(mappingsToJson(), armourFile, models);
    }

    private static Map<String, JsonObject> createIndividualModels(List<Bone> bones, int textureWidth, int textureHeight, ModelGenerator.BBEntityModel bbModel, JsonObject builtTextures, JsonArray textureSize, TextureState state) {
        HashMap<String, JsonObject> modelInfo = new HashMap<>();

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
            final Point trueMid = bone.pivot.mul(-1, 1, 1).sub(8, 8, 8);
            final Point midOffset = cubeMid.sub(trueMid);

            final Point cubeDiff = new Vec(trueMid.x() - cubeMinX + 16, trueMid.y() - cubeMinY + 16, trueMid.z() - cubeMinZ + 16);

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
                    throw new IllegalArgumentException("Invalid rotation: " + boneName + " X " + bbModel.id() + " " + cubeRotation.x());
                }
                if (cubeRotation.y() != 45 && cubeRotation.y() != -22.5 && cubeRotation.y() != 22.5 && cubeRotation.y() != -45 && cubeRotation.y() != 0) {
                    throw new IllegalArgumentException("Invalid rotation: " + boneName + " Y " + bbModel.id() + " " + cubeRotation.y());
                }
                if (cubeRotation.z() != 45 && cubeRotation.z() != -22.5 && cubeRotation.z() != 22.5 && cubeRotation.z() != -45 && cubeRotation.z() != 0) {
                    throw new IllegalArgumentException("Invalid rotation: " + boneName + " Z " + bbModel.id() + " " + cubeRotation.z());
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

                if (!mappings.containsKey(bbModel.id() + "/" + boneName) || mappings.get(bbModel.id() + "/" + boneName).map.get(state.name()) == null) {
                    var item =
                            new ItemId(
                                    bbModel.id(),
                                    boneName,
                                    new Vec(cubeMinX + cubeDiff.x() - 8, cubeMinY + cubeDiff.y() - 8, cubeMinZ + cubeDiff.z() - 8),
                                    cubeDiff,
                                    ++index
                            );

                    if (mappings.get(item.name + "/" + item.bone) == null)
                        mappings.put(item.name + "/" + item.bone, new MappingEntry(new HashMap<>(), item.offset, item.diff));

                    mappings.get(item.name + "/" + item.bone).map.put(state.name(), item.id);
                    predicates.add(createPredicate(item.id, bbModel.id(), state.name(), item.bone));
                }

                JsonObjectBuilder boneInfo = Json.createObjectBuilder();
                boneInfo.add("textures", builtTextures);
                boneInfo.add("elements", elementsToJson(elements));
                boneInfo.add("texture_size", textureSize);
                boneInfo.add("display", display(midOffset));
                modelInfo.put(boneName + ".json", boneInfo.build());
            }
        }

        return modelInfo;
    }

    private static ModelFile generateModelFile(TextureState state, ModelGenerator.BBEntityModel bbModel, List<Bone> bones, JsonArray textureSize, int textureWidth, int textureHeight) throws IOException {
        Map<String, byte[]> textures = new HashMap<>();

        JsonObjectBuilder modelTextureJson = Json.createObjectBuilder();
        for (Map.Entry<String, TextureGenerator.TextureData> t : bbModel.textures().entrySet()) {
            modelTextureJson.add(t.getKey(), "worldseed:mobs/" + bbModel.id() + "/" + state.name() + "/" + t.getKey());

            byte[] textureByte = t.getValue().value();
            BufferedImage texture = ImageIO.read(new BufferedInputStream(new ByteArrayInputStream(textureByte)));
            BufferedImage stateTexture = state.multiplyColour(texture);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(stateTexture, "png", baos);
            textures.put(t.getKey(), baos.toByteArray());
        }

        Map<String, JsonObject> modelInfo = createIndividualModels(bones, textureWidth, textureHeight, bbModel, modelTextureJson.build(), textureSize, state);

        return new ModelFile(modelInfo, textures, bbModel.id(), state, textureWidth, textureHeight);
    }

    private static List<ModelFile> createFiles(ModelGenerator.BBEntityModel bbModel, Path modelPathMobs) throws Exception {
        List<ModelFile> res = new ArrayList<>();

        int textureHeight = 16;
        int textureWidth = 16;

        JsonObject modelGeoFile = bbModel.geo().getJsonArray("minecraft:geometry").get(0).asJsonObject();

        JsonArray bonesJson = modelGeoFile.getJsonArray("bones");
        JsonObject description = modelGeoFile.getJsonObject("description");

        if (description != null) {
            textureHeight = description.getInt("texture_height");
            textureWidth = description.getInt("texture_width");
        }

        JsonArray textureSize = Json.createArrayBuilder()
                .add(textureWidth)
                .add(textureHeight).build();

        List<Bone> bones = new ArrayList<>();

        for (JsonValue bone : bonesJson) {
            if (bone.asJsonObject().getJsonArray("cubes") == null) continue;
            String name = bone.asJsonObject().getString("name");
            Point bonePivot = getPos(bone.asJsonObject().get("pivot").asJsonArray()).orElse(Vec.ZERO);

            List<Cube> cubes = new ArrayList<>();
            for (JsonValue cubeJson : bone.asJsonObject().getJsonArray("cubes")) {
                Optional<Point> origin = getPos(cubeJson.asJsonObject().getJsonArray("origin"));
                Optional<Point> size = getPos(cubeJson.asJsonObject().getJsonArray("size"));

                Optional<Point> pivot = Optional.empty();
                if (cubeJson.asJsonObject().getJsonArray("pivot") != null)
                    pivot = getPos(cubeJson.asJsonObject().getJsonArray("pivot"));

                Optional<Point> rotation = Optional.empty();
                if (cubeJson.asJsonObject().getJsonArray("rotation") != null)
                    rotation = getPos(cubeJson.asJsonObject().getJsonArray("rotation"));


                Map<TextureFace, UV> uv = getUV(cubeJson.asJsonObject().getJsonObject("uv"));

                if (origin.isPresent() && size.isPresent()) {
                    Cube cube = new Cube(origin.get().withX(-origin.get().x() - size.get().x()), size.get(), pivot.orElse(Vec.ZERO), rotation.orElse(Vec.ZERO), uv);
                    cubes.add(cube);
                }
            }

            if (!cubes.isEmpty()) {
                bones.add(new Bone(name, cubes, bonePivot));
            }
        }

        for (var state : List.of(TextureState.HIT, TextureState.NORMAL)) {
            var modelFile = generateModelFile(state, bbModel, bones, textureSize, textureWidth, textureHeight);
            res.add(modelFile);

            for (var substate : bbModel.additionalStates().states.entrySet()) {
                for (var subBone : substate.getValue().boneTextureMappings().entrySet()) {
                    JsonObjectBuilder modelTextureJson = Json.createObjectBuilder();

                    for (var t : modelFile.textures.keySet()) {
                        modelTextureJson.add(t, "worldseed:mobs/" + bbModel.id() + "/" + state.name + "/" + subBone.getValue());
                    }

                    var subbones = bones.stream()
                            .filter(bone -> subBone.getKey().equals(bone.name))
                            .toList();

                    var toWrite = createIndividualModels(subbones, textureWidth, textureHeight, bbModel, modelTextureJson.build(), textureSize, substate.getValue().state());

                    for (var w : toWrite.entrySet()) {
                        FileUtils.writeStringToFile(modelPathMobs.resolve(bbModel.id() + "/" + substate.getKey() + "/" + w.getKey()).toFile(), w.getValue().toString(), Charset.defaultCharset());
                    }
                }
            }
        }

        return res;
    }

    private static UV convertUV(UV uv, int width, int height, boolean inverse) {
        double sx = uv.x1 * (16.0 / width);
        double sy = uv.y1 * (16.0 / height);
        double ex = uv.x2 * (16.0 / width);
        double ey = uv.y2 * (16.0 / height);

        if (inverse)
            return new UV(ex + sx, ey + sy, sx, sy, uv.texture, uv.rotation);
        return new UV(sx, sy, ex + sx, ey + sy, uv.texture, uv.rotation);
    }

    private static JsonObject mappingsToJson() {
        JsonObjectBuilder res = Json.createObjectBuilder();

        for (Map.Entry<String, MappingEntry> entry : ModelParser.mappings.entrySet()) {
            String id = entry.getKey();
            MappingEntry mapping = entry.getValue();

            res.add(id, mapping.asJson());
        }

        return res.build();
    }

    private static JsonObject entrySetToJson(Map<String, Integer> map) {
        JsonObjectBuilder res = Json.createObjectBuilder();

        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            res.add(entry.getKey(), entry.getValue());
        }

        return res.build();
    }

    private static JsonArray predicatesToJson() {
        var array = Json.createArrayBuilder();
        for (var predicate : ModelParser.predicates) {
            array.add(predicate);
        }

        return array.build();
    }

    private static JsonArray elementsToJson(List<Element> elements) {
        JsonArrayBuilder res = Json.createArrayBuilder();

        for (Element element : elements) {
            res.add(element.asJson());
        }

        return res.build();
    }

    private static JsonObject facesAsJson(Map<TextureFace, UV> faces) {
        JsonObjectBuilder res = Json.createObjectBuilder();
        for (TextureFace face : faces.keySet()) {
            res.add(face.name(), faces.get(face).asJson());
        }

        return res.build();
    }

    private static JsonArray pointAsJson(Point from) {
        JsonArrayBuilder res = Json.createArrayBuilder();
        res.add(Math.round(from.x() * 10000) / 10000.0);
        res.add(Math.round(from.y() * 10000) / 10000.0);
        res.add(Math.round(from.z() * 10000) / 10000.0);
        return res.build();
    }

    private static Map<TextureFace, UV> getUV(JsonObject uv) {
        Map<TextureFace, UV> res = new HashMap<>();

        for (TextureFace face : TextureFace.values()) {
            String faceName = face.name().toLowerCase(Locale.ROOT);

            JsonObject north = uv.getJsonObject(faceName);
            JsonArray north_uv = north.getJsonArray("uv");
            JsonArray north_size = north.getJsonArray("uv_size");
            String texture = north.getString("texture");
            int rotation = north.get("rotation") == null ? 0 : north.getInt("rotation");

            UV uvNorth = new UV(north_uv.getJsonNumber(0).doubleValue(), north_uv.getJsonNumber(1).doubleValue(), north_size.getJsonNumber(0).doubleValue(), north_size.getJsonNumber(1).doubleValue(), texture, rotation);
            res.put(face, uvNorth);
        }

        return res;
    }

    private static JsonObject createPredicate(int id, String name, String state, String bone) {
        JsonObjectBuilder res = Json.createObjectBuilder();

        JsonObjectBuilder customModelData = Json.createObjectBuilder();
        customModelData.add("custom_model_data", id);

        res.add("predicate", customModelData);
        res.add("model", "worldseed:mobs/" + name + "/" + state + "/" + bone);

        return res.build();
    }

    private enum TextureFace {
        north,
        east,
        south,
        west,
        up,
        down
    }

    public record TextureState(double r, double g, double b, double ar, double ag, double ab, String name) {
        public static final TextureState NORMAL = new TextureState(1.0, 1.0, 1.0, 0, 0, 0, "normal");
        public static final TextureState HIT = new TextureState(0.7, 0.7, 0.7, 255, 0, 0, "hit");

        BufferedImage multiplyColour(BufferedImage oldImg) {
            ColorModel cm = oldImg.getColorModel();
            boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
            WritableRaster raster = oldImg.copyData(null);
            BufferedImage img = new BufferedImage(cm, raster, isAlphaPremultiplied, null);

            for (int i = 0; i < img.getWidth(); i++) {
                for (int j = 0; j < img.getHeight(); j++) {
                    int rgb = img.getRGB(i, j);
                    double ir = (((rgb >> 16) & 0xFF) * r);
                    double ig = (((rgb >> 8) & 0xFF) * g);
                    double ib = ((rgb & 0xFF) * b);
                    int ia = (rgb >> 24) & 0xFF;

                    int lerpR = (int) (ir + ar * (1 - r));
                    int lerpG = (int) (ig + ag * (1 - g));
                    int lerpB = (int) (ib + ab * (1 - b));

                    img.setRGB(i, j, (ia << 24) | (Math.min(lerpR, 255) << 16) | (Math.min(lerpG, 255) << 8) | Math.min(lerpB, 255));
                }
            }

            return img;
        }

    }

    public record ModelFile(Map<String, JsonObject> bones, Map<String, byte[]> textures, String id, TextureState state,
                            int textureWidth, int textureHeight) {
    }

    public record ModelEngineFiles(JsonObject mappings, JsonObject binding, List<ModelFile> models) {
    }

    record Cube(Point origin, Point size, Point pivot, Point rotation, Map<TextureFace, UV> uv) {
    }

    record Bone(String name, List<Cube> cubes, Point pivot) {
    }

    record ItemId(String name, String bone, Point offset, Point diff, int id) {
    }

    record MappingEntry(Map<String, Integer> map, Point offset, Point diff) {
        public JsonObject asJson() {
            JsonObjectBuilder res = Json.createObjectBuilder();

            res.add("id", entrySetToJson(map));
            res.add("offset", pointAsJson(offset));
            res.add("diff", pointAsJson(diff));

            return res.build();
        }
    }

    record UV(double x1, double y1, double x2, double y2, String texture, int rotation) {
        public JsonObject asJson() {
            JsonArrayBuilder els = Json.createArrayBuilder();
            els.add(x1);
            els.add(y1);
            els.add(x2);
            els.add(y2);

            JsonObjectBuilder res = Json.createObjectBuilder();

            res.add("uv", els);
            res.add("texture", texture);
            if (rotation != 0) res.add("rotation", rotation);
            return res.build();
        }
    }

    record RotationInfo(double angle, String axis, Point origin) {
        public JsonObject asJson() {
            JsonObjectBuilder res = Json.createObjectBuilder();
            res.add("angle", angle);
            res.add("axis", axis);
            res.add("origin", pointAsJson(origin));
            return res.build();
        }
    }

    record Element(Point from, Point to, Map<TextureFace, UV> faces, RotationInfo rotation) {
        public JsonObject asJson() {
            JsonObjectBuilder res = Json.createObjectBuilder();
            res.add("from", pointAsJson(from));
            res.add("to", pointAsJson(to));
            res.add("faces", facesAsJson(faces));
            res.add("rotation", rotation.asJson());

            return res.build();
        }
    }
}

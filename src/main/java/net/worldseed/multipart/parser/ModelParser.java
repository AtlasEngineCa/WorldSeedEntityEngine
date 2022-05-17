package net.worldseed.multipart.parser;

import com.google.gson.*;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.worldseed.multipart.ModelEngine;

import javax.imageio.ImageIO;
import javax.naming.SizeLimitExceededException;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class ModelParser {
    protected static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static final String MODEL_PATH = "models/";
    public static final String OUTPUT_PATH = "resourcepack/";
    private static int index = 0;

    // private static mappings = {};
    // private static predicates = [];

    private static UV convertUV(UV uv, int width, int height, boolean inverse) {
        double sx = uv.x1 * (16.0 / width);
        double sy = uv.y1 * (16.0 / height);
        double ex = uv.x2 * (16.0 / width);
        double ey = uv.y2 * (16.0 / height);

        if (inverse)
            return new UV(ex+sx, ey+sy, sx, sy);
        return new UV(sx, sy, ex + sx, ey + sy);
    }

    public static void parse() throws IOException, NoSuchAlgorithmException, SizeLimitExceededException {
        // leather_armour_file = {"parent": "item/generated",
        //     "textures": {"layer0": "minecraft:item/leather_horse_armor"},
        //     "overrides": predicates
        // }

        // with open(outputPath + '/resourcepack/assets/minecraft/models/item/leather_horse_armor.json', 'w', encoding='utf-8') as f:
        //     json.dump(leather_armour_file, f, ensure_ascii=False)

        // with open(outputPath + '/model_mappings.json', 'w', encoding='utf-8') as f:
        //     json.dump(mappings, f, ensure_ascii=False)

        createFiles("gem_golem");
    }

    record Cube(Point origin, Point size, Point pivot, Point rotation, Map<TextureFace, UV> uv) {}
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

    private static void createFiles(String modelName) throws IOException, NoSuchAlgorithmException, SizeLimitExceededException {
        List<TextureState> toGenerate = List.of(TextureState.hit, TextureState.normal);
        HashMap<String, JsonObject> modelInfo = new HashMap<>();

        String geoFile = MODEL_PATH + modelName + "/model.geo.json";
        String texturePath = MODEL_PATH + modelName + "/texture.png";

        BufferedImage texture = ImageIO.read(new File(texturePath));

        int textureHeight = 16;
        int textureWidth = 16;

        JsonObject modelGeoFile = GSON.fromJson(
            new InputStreamReader(new FileInputStream(geoFile)),
            JsonObject.class
        ).get("minecraft:geometry").getAsJsonArray().get(0).getAsJsonObject();

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
                Optional<Pos> origin = ModelEngine.getPos(cubeJson.getAsJsonObject().get("origin").getAsJsonArray());
                Optional<Pos> size = ModelEngine.getPos(cubeJson.getAsJsonObject().get("size").getAsJsonArray());

                Optional<Pos> pivot = Optional.empty();
                if (cubeJson.getAsJsonObject().has("pivot"))
                    pivot = ModelEngine.getPos(cubeJson.getAsJsonObject().get("pivot").getAsJsonArray());

                Optional<Pos> rotation = Optional.empty();
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
                sb.append(Integer.toHexString((b & 0xFF) | 0x100).substring(1, 3));
            }

            String uuid = sb.toString();
            String outputTexturePath = OUTPUT_PATH + "textures/mobs/" + modelName + "/" + state.name() + "/";
            String outputModelPath = OUTPUT_PATH + "models/mobs/" + modelName + "/" + state.name() + "/";

            new File(outputTexturePath).mkdirs();
            new File(outputModelPath).mkdirs();

            JsonObject modelTextureJson = new JsonObject();
            modelTextureJson.addProperty("0", "mobs/" + modelName + "/" + state.name() + "/" + uuid);

            BufferedImage stateTexture = state.multiplyColour(texture);
            ImageIO.write(stateTexture, "png", new File(outputTexturePath + "/" + uuid + ".png"));

            for (Bone bone : bones) {
                String boneName = bone.name;

                List<Element> elements = new ArrayList<>();
                double cubeMinX = Double.MAX_VALUE;
                double cubeMinY = Double.MAX_VALUE;
                double cubeMinZ = Double.MAX_VALUE;

                double cubeMaxX = Double.MIN_VALUE;
                double cubeMaxY = Double.MIN_VALUE;
                double cubeMaxZ = Double.MIN_VALUE;

                Point cubeMid;
                Point cubeDiff;

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

                cubeMid = new Pos((cubeMaxX + cubeMinX) / 2 - 8, (cubeMaxY + cubeMinY) / 2 - 8, (cubeMaxZ + cubeMinZ) / 2 - 8);
                cubeDiff = new Pos(cubeMid.x() - cubeMinX, cubeMid.y() - cubeMinY, cubeMid.z() - cubeMinZ);

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
                    Point cubePivot = new Pos(-(cube.pivot().x() + cubeMid.x()), cube.pivot.y() - cubeMid.y(), cube.pivot.z() - cubeMid.z());
                    Point cubeSize = cube.size;
                    Point cubeOrigin = cube.origin;

                    Point cubeFrom = new Pos(cubeOrigin.x() - cubeMid.x(), cubeOrigin.y() - cubeMid.y(), cubeOrigin.z() - cubeMid.z());
                    Point cubeTo = new Pos(cubeFrom.x() + cubeSize.x(), cubeFrom.y() + cubeSize.y(), cubeFrom.z() + cubeSize.z());

                    Point cubeRotation = new Pos(-cube.rotation.x(), -cube.rotation.y(), cube.rotation.z());
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

                    itemIds.add(new ItemId(modelName, boneName, new Pos(cubeMinX + cubeDiff.x() - 8, cubeMinY + cubeDiff.y() - 8, cubeMinZ + cubeDiff.z() - 8), cubeDiff, index));
                    index++;

                    JsonObject boneInfo = new JsonObject();
                    boneInfo.add("texture", modelTextureJson);
                    boneInfo.add("elements", elementsToJson(elements));
                    boneInfo.add("texture_size", textureSize);
                    boneInfo.add("display", display);
                    modelInfo.put(boneName + ".json", boneInfo);
                }
            }

            for (Map.Entry<String, JsonObject> modelData : modelInfo.entrySet()) {
                String fileName = modelData.getKey();
                JsonObject modelJson = modelData.getValue();

                File modelFile = new File(outputModelPath, fileName);
                // write modelJson to modelFile
                try (FileWriter fileWriter = new FileWriter(modelFile)) {
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    gson.toJson(modelJson, fileWriter);

                    fileWriter.flush();
                }

            }
        }
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

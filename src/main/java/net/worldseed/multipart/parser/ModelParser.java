package net.worldseed.multipart.parser;

import com.google.gson.*;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.worldseed.multipart.ModelEngine;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ModelParser {
    protected static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static final String MODEL_PATH = "models/";
    public static final String OUTPUT_PATH = "resourcepack/";
    private static int index = 0;

    // private static mappings = {};
    // private static predicates = [];

    // private static float convertUV(obj, tw, th, invert) {
    // }

    public static void parse() throws IOException, NoSuchAlgorithmException {
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

    record Cube(Point origin, Point size, Point pivot) {}
    record Bone(String name, List<Cube> cubes) {}

    record ItemId(String name, String bone, Point offset, Point diff, int id) {}
    record Quad(float w, float x, float y, float z) {}
    record RotationInfo(Point angle, Point axis, Point origin) {}
    record Element(Point from, Point to, Quad faces, RotationInfo rotation) {}

    private static void createFiles(String modelName) throws IOException, NoSuchAlgorithmException {
        List<TextureState> toGenerate = List.of(TextureState.hit, TextureState.normal);

        String geoFile = MODEL_PATH + modelName + "/model.geo.json";
        String texturePath = MODEL_PATH + modelName + "/texture.png";

        System.out.println(texturePath);

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

        List<Bone> bones = new ArrayList<>();
        for (JsonElement bone : bonesJson) {
            if (!bone.getAsJsonObject().has("cubes")) continue;
            String name = bone.getAsJsonObject().get("name").getAsString();

            List<Cube> cubes = new ArrayList<>();
            for (JsonElement cubeJson : bone.getAsJsonObject().get("cubes").getAsJsonArray()) {
                Optional<Pos> origin = ModelEngine.getPos(cubeJson.getAsJsonObject().get("origin").getAsJsonArray());
                Optional<Pos> size = ModelEngine.getPos(cubeJson.getAsJsonObject().get("size").getAsJsonArray());
                Optional<Pos> pivot = ModelEngine.getPos(cubeJson.getAsJsonObject().get("pivot").getAsJsonArray());

                if (origin.isPresent() && size.isPresent()) {
                    Cube cube = new Cube(origin.get().withX(-origin.get().x() - size.get().x()), size.get(), pivot.orElse(Pos.ZERO));
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
        }

        for (Bone bone : bones) {
            String boneName = bone.name;

            List<Element> elements = new ArrayList<>();
            double cubeMinX = Double.MAX_VALUE;
            double cubeMinY = Double.MAX_VALUE;
            double cubeMinZ = Double.MAX_VALUE;

            double cubeMaxX = Double.MIN_VALUE;
            double cubeMaxY = Double.MIN_VALUE;
            double cubeMaxZ = Double.MIN_VALUE;

            Point cubeMid = Pos.ZERO;
            Point cubeDiff = Pos.ZERO;

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

            cubeMid = new Pos((cubeMaxX - cubeMinX) / 2, (cubeMaxY - cubeMinY) / 2, (cubeMaxZ - cubeMinZ) / 2);
            cubeDiff = new Pos(cubeMid.x() - (cubeMinX-8), cubeMid.y() - (cubeMinY-8), cubeMid.z() - (cubeMinZ-8));
        }
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

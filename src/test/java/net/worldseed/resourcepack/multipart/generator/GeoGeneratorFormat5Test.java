package net.worldseed.resourcepack.multipart.generator;

import net.worldseed.resourcepack.PackBuilder;
import org.junit.jupiter.api.Test;

import javax.json.JsonArray;
import javax.json.JsonObject;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeoGeneratorFormat5Test {
    private static final String CUBE_UUID = "09000000-0000-0000-0000-000000000001";
    private static final String BODY_UUID = "09000000-0000-0000-0000-000000000002";
    private static final String HEAD_UUID = "09000000-0000-0000-0000-000000000003";

    @Test
    void format5OutlinerStubsResolveBoneDataFromGroups() {
        ModelGenerator.BBEntityModel model = ModelGenerator.generate(new PackBuilder.Model(format5Model(), "format5_test", null));

        JsonObject head = bone(model, "head");
        assertNotNull(head, "bone must exist under its real name, not its uuid");
        assertArrayEquals(new double[]{0.0, 6.0, 1.0}, pivot(head), 1e-6);
        assertArrayEquals(new double[]{-10.0, 20.0, 30.0}, rotation(head), 1e-6);
        assertEquals("body", head.getString("parent"));
        assertEquals(1, head.getJsonArray("cubes").size(), "the cube must attach to the head bone");

        JsonObject body = bone(model, "body");
        assertNotNull(body);
        assertArrayEquals(new double[]{0.0, 3.0, 0.0}, pivot(body), 1e-6);

        assertNull(bone(model, HEAD_UUID), "bones must never be registered under raw uuids");
    }

    @Test
    void format5AnimationChannelsMatchGeoBoneNames() {
        ModelGenerator.BBEntityModel model = ModelGenerator.generate(new PackBuilder.Model(format5Model(), "format5_test", null));

        JsonObject bones = model.animations().getJsonObject("animations")
                .getJsonObject("nod").getJsonObject("bones");
        assertTrue(bones.containsKey("head"), "animation channel must be keyed by the real bone name");
        assertNotNull(bone(model, "head"), "geo must expose a bone for the animation channel to bind to");
    }

    @Test
    void preFormat5OutlinerStillParsesUnchanged() {
        // pre-5.0 files embed name/origin/rotation in the outliner itself and carry no "groups" key
        ModelGenerator.BBEntityModel model = ModelGenerator.generate(new PackBuilder.Model(format4Model(), "legacy", null));

        JsonObject head = bone(model, "head");
        assertNotNull(head);
        assertArrayEquals(new double[]{0.0, 6.0, 1.0}, pivot(head), 1e-6);
        assertArrayEquals(new double[]{-10.0, 20.0, 30.0}, rotation(head), 1e-6);
        assertEquals(1, head.getJsonArray("cubes").size());
    }

    private static JsonObject bone(ModelGenerator.BBEntityModel model, String name) {
        JsonArray bones = model.geo()
                .getJsonArray("minecraft:geometry").getJsonObject(0)
                .getJsonArray("bones");
        for (JsonObject bone : bones.getValuesAs(JsonObject.class)) {
            if (name.equals(bone.getString("name", null))) return bone;
        }
        return null;
    }

    private static double[] pivot(JsonObject bone) {
        return toArray(bone.getJsonArray("pivot"));
    }

    private static double[] rotation(JsonObject bone) {
        return toArray(bone.getJsonArray("rotation"));
    }

    private static double[] toArray(JsonArray array) {
        return new double[]{
                array.getJsonNumber(0).doubleValue(),
                array.getJsonNumber(1).doubleValue(),
                array.getJsonNumber(2).doubleValue()
        };
    }

    private static String format5Model() {
        return """
                {
                  "meta": {"format_version": "5.0", "model_format": "bedrock", "box_uv": true},
                  "name": "format5_test",
                  "resolution": {"width": 64, "height": 64},
                  "elements": [%s],
                  "groups": [
                    {"name": "body", "uuid": "%s", "origin": [0, 12, 0], "rotation": [0, 0, 0], "children": []},
                    {"name": "head", "uuid": "%s", "origin": [0, 24, 4], "rotation": [10, -20, 30], "children": []}
                  ],
                  "outliner": [
                    {"uuid": "%s", "isOpen": true, "children": [
                      {"uuid": "%s", "isOpen": true, "children": ["%s"]}
                    ]}
                  ],
                  "textures": [],
                  "animations": [
                    {
                      "uuid": "09000000-0000-0000-0000-000000000004",
                      "name": "nod",
                      "length": 1,
                      "animators": {
                        "%s": {
                          "name": "head",
                          "type": "bone",
                          "keyframes": [
                            {"channel": "rotation", "time": 0, "interpolation": "linear",
                             "data_points": [{"x": 5, "y": 10, "z": -15}]}
                          ]
                        }
                      }
                    }
                  ]
                }
                """.formatted(cubeJson(), BODY_UUID, HEAD_UUID, BODY_UUID, HEAD_UUID, CUBE_UUID, HEAD_UUID);
    }

    private static String format4Model() {
        return """
                {
                  "meta": {"format_version": "4.10", "model_format": "bedrock", "box_uv": true},
                  "name": "legacy",
                  "resolution": {"width": 64, "height": 64},
                  "elements": [%s],
                  "outliner": [
                    {
                      "uuid": "%s", "name": "body", "origin": [0, 12, 0], "rotation": [0, 0, 0],
                      "children": [
                        {
                          "uuid": "%s", "name": "head", "origin": [0, 24, 4], "rotation": [10, -20, 30],
                          "children": ["%s"]
                        }
                      ]
                    }
                  ],
                  "textures": []
                }
                """.formatted(cubeJson(), BODY_UUID, HEAD_UUID, CUBE_UUID);
    }

    private static String cubeJson() {
        return """
                {
                  "name": "head_cube", "box_uv": true,
                  "from": [-4, 24, -4], "to": [4, 32, 4], "origin": [0, 24, 0],
                  "uv_offset": [0, 0],
                  "faces": {
                    "north": {"uv": [8, 8, 16, 16], "texture": 0},
                    "east": {"uv": [0, 8, 8, 16], "texture": 0},
                    "south": {"uv": [16, 8, 24, 16], "texture": 0},
                    "west": {"uv": [24, 8, 32, 16], "texture": 0},
                    "up": {"uv": [8, 8, 16, 0], "texture": 0},
                    "down": {"uv": [16, 0, 24, 8], "texture": 0}
                  },
                  "type": "cube", "uuid": "%s"
                }
                """.formatted(CUBE_UUID);
    }
}

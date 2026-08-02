package net.worldseed.resourcepack.multipart.generator;

import org.junit.jupiter.api.Test;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimationRendererParityTest {
    @Test
    void detectsTheSameLegacyVersionsAsTheRustRenderer() {
        assertTrue(ModelGenerator.usesLegacyAnimationCoordinates("4.10"));
        assertFalse(ModelGenerator.usesLegacyAnimationCoordinates("5.0"));
        assertFalse(ModelGenerator.usesLegacyAnimationCoordinates(""));
    }

    @Test
    void modernCoordinatesCancelTheRuntimeLegacyMirror() {
        JsonObject animation = animationWithPoints(
                Json.createObjectBuilder().add("x", 10).add("y", 20).add("z", 30).build(),
                Json.createObjectBuilder().add("x", "query.anim_time * 2").add("y", 2).add("z", 3).build());

        JsonObject generated = AnimationGenerator.generate(
                Json.createArrayBuilder().add(animation).build(), false);
        JsonObject rotation = point(generated, "rotation");
        JsonObject position = point(generated, "position");

        assertEquals(-10, rotation.getInt("x"));
        assertEquals(-20, rotation.getInt("y"));
        assertEquals(30, rotation.getInt("z"));
        assertEquals("-(query.anim_time * 2)", position.getString("x"));
        assertEquals(2, position.getInt("y"));
        assertEquals(3, position.getInt("z"));
    }

    @Test
    void legacyCoordinatesPassThroughForTheRuntimeMirror() {
        JsonObject animation = animationWithPoints(
                Json.createObjectBuilder().add("x", 10).add("y", 20).add("z", 30).build(),
                Json.createObjectBuilder().add("x", 1).add("y", 2).add("z", 3).build());

        JsonObject generated = AnimationGenerator.generate(
                Json.createArrayBuilder().add(animation).build(), true);

        assertEquals(10, point(generated, "rotation").getInt("x"));
        assertEquals(1, point(generated, "position").getInt("x"));
    }

    private static JsonObject animationWithPoints(JsonObject rotation, JsonObject position) {
        JsonArray keyframes = Json.createArrayBuilder()
                .add(keyframe("rotation", rotation))
                .add(keyframe("position", position))
                .build();
        JsonObject animator = Json.createObjectBuilder()
                .add("name", "arm")
                .add("type", "bone")
                .add("keyframes", keyframes)
                .build();
        return Json.createObjectBuilder()
                .add("name", "wave")
                .add("length", 1)
                .add("animators", Json.createObjectBuilder().add("uuid", animator))
                .build();
    }

    private static JsonObject keyframe(String channel, JsonObject point) {
        return Json.createObjectBuilder()
                .add("channel", channel)
                .add("time", 0)
                .add("interpolation", "linear")
                .add("data_points", Json.createArrayBuilder().add(point))
                .build();
    }

    private static JsonObject point(JsonObject generated, String channel) {
        return generated.getJsonObject("wave")
                .getJsonObject("bones")
                .getJsonObject("arm")
                .getJsonObject(channel)
                .getJsonObject("0.0")
                .getJsonArray("post")
                .getJsonObject(0);
    }
}

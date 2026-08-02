package net.worldseed.multipart.animations;

import net.worldseed.resourcepack.multipart.generator.AnimationGenerator;
import org.junit.jupiter.api.Test;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Blockbench "effects" animator (sound/particle/timeline) must survive AnimationGenerator into the
 *  generated animation JSON so the runtime can fire it. (Bone animators used to be the only ones kept.) */
class AnimationEffectsTest {

    private static JsonObject dataPoint(String key, String value) {
        return Json.createObjectBuilder().add(key, value).build();
    }

    private JsonObject generateFrom(JsonArray animators) {
        JsonArray animations = Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add("name", "test")
                        .add("length", 2.0)
                        .add("loop", "loop")
                        .add("animators", animators.isEmpty()
                                ? Json.createObjectBuilder()
                                : Json.createObjectBuilder().add("fx", animators.getJsonObject(0))))
                .build();
        return AnimationGenerator.generate(animations);
    }

    @Test
    void soundAndParticleEffectsSurviveGeneration() {
        JsonObject effectAnimator = Json.createObjectBuilder()
                .add("name", "effects")
                .add("type", "effect")
                .add("keyframes", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("channel", "sound").add("time", 0.5)
                                .add("data_points", Json.createArrayBuilder().add(dataPoint("effect", "minecraft:block.anvil.land"))))
                        .add(Json.createObjectBuilder()
                                .add("channel", "particle").add("time", 1.0)
                                .add("data_points", Json.createArrayBuilder()
                                        .add(Json.createObjectBuilder().add("effect", "minecraft:flame").add("locator", "muzzle")))))
                .build();

        JsonObject out = generateFrom(Json.createArrayBuilder().add(effectAnimator).build());
        JsonArray effects = out.getJsonObject("test").getJsonArray("effects");

        assertEquals(2, effects.size(), "both effect keyframes should be emitted");
        assertEquals("sound", effects.getJsonObject(0).getString("channel"));
        assertEquals("minecraft:block.anvil.land", effects.getJsonObject(0).getString("effect"));
        assertEquals(0.5, effects.getJsonObject(0).getJsonNumber("time").doubleValue());
        assertEquals("particle", effects.getJsonObject(1).getString("channel"));
        assertEquals("muzzle", effects.getJsonObject(1).getString("locator"));
    }

    @Test
    void modelWithNoEffectsGetsEmptyEffectsArray() {
        JsonObject out = generateFrom(Json.createArrayBuilder().build());
        assertTrue(out.getJsonObject("test").getJsonArray("effects").isEmpty());
    }
}

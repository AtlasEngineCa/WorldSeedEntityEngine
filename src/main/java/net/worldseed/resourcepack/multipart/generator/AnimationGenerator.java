package net.worldseed.resourcepack.multipart.generator;

import javax.json.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class AnimationGenerator {
    public static JsonObject generate(JsonArray animationRaw) {
        return generate(animationRaw, true);
    }

    /**
     * Convert Blockbench animations to the runtime animation format.
     *
     * @param legacyCoordinates {@code true} for pre-5.0 bbmodels, whose animation values use the
     *                          legacy Bedrock coordinate convention
     */
    public static JsonObject generate(JsonArray animationRaw, boolean legacyCoordinates) {
        JsonObjectBuilder animations = Json.createObjectBuilder();
        if (animationRaw == null) return animations.build();

        for (int i = 0; i < animationRaw.size(); i++) {
            JsonObject animation = animationRaw.getJsonObject(i);

            String name = animation.getString("name", null);
            if (name == null) continue;
            JsonNumber lengthNumber = animation.getJsonNumber("length");
            double length = lengthNumber == null ? 0 : lengthNumber.doubleValue();

            JsonObjectBuilder bones = Json.createObjectBuilder();
            JsonArrayBuilder effects = Json.createArrayBuilder();

            var foundAnimations = animation.getJsonObject("animators");
            if (foundAnimations == null) continue;

            Collection<JsonValue> animators = foundAnimations.values();

            for (var animator_ : animators) {
                JsonObject animator = animator_.asJsonObject();

                String type = animator.getString("type", "bone");

                if (type.equals("effect")) {
                    JsonArray effectKeyframes = animator.getJsonArray("keyframes");
                    if (effectKeyframes != null) {
                        for (int k = 0; k < effectKeyframes.size(); k++) {
                            JsonObject keyframe = effectKeyframes.getJsonObject(k);
                            String channel = keyframe.getString("channel", "");
                            if (!channel.equals("sound") && !channel.equals("particle") && !channel.equals("timeline")) continue;
                            JsonNumber effectTime = keyframe.getJsonNumber("time");
                            if (effectTime == null) continue;
                            JsonArray dataPoints = keyframe.getJsonArray("data_points");
                            JsonObject data = (dataPoints != null && !dataPoints.isEmpty()) ? dataPoints.getJsonObject(0) : JsonValue.EMPTY_JSON_OBJECT;
                            JsonObjectBuilder effect = Json.createObjectBuilder()
                                    .add("time", effectTime.doubleValue())
                                    .add("channel", channel);
                            if (data.containsKey("effect")) effect.add("effect", data.getString("effect", ""));
                            if (data.containsKey("locator")) effect.add("locator", data.getString("locator", ""));
                            if (data.containsKey("script")) effect.add("script", data.getString("script", ""));
                            effects.add(effect.build());
                        }
                    }
                    continue;
                }

                if (!type.equals("bone")) continue;
                String boneName = animator.getString("name", null);
                if (boneName == null) continue; // malformed animator without a bone name

                List<Map.Entry<Double, JsonObject>> rotation = new ArrayList<>();
                List<Map.Entry<Double, JsonObject>> position = new ArrayList<>();
                List<Map.Entry<Double, JsonObject>> scale = new ArrayList<>();

                JsonArray keyframes = animator.getJsonArray("keyframes");
                if (keyframes == null) continue;

                for (int k = 0; k < keyframes.size(); k++) {
                    JsonObject keyframe = keyframes.getJsonObject(k);
                    String channel = keyframe.getString("channel", null);
                    JsonNumber timeNumber = keyframe.getJsonNumber("time");
                    JsonArray dataPoints = keyframe.getJsonArray("data_points");
                    if (channel == null || timeNumber == null || dataPoints == null) continue; // skip malformed keyframe

                    double time = timeNumber.doubleValue();
                    String interpolation = keyframe.getString("interpolation", "linear");

                    JsonObject built = Json.createObjectBuilder()
                            .add("post", legacyCoordinates ? dataPoints : mirrorForRuntime(dataPoints, channel))
                            .add("lerp_mode", interpolation)
                            .build();

                    switch (channel) {
                        case "rotation" -> rotation.add(Map.entry(time, built));
                        case "position" -> position.add(Map.entry((time), built));
                        case "scale" -> scale.add(Map.entry((time), built));
                    }
                }

                rotation.sort(Map.Entry.comparingByKey());
                position.sort(Map.Entry.comparingByKey());
                scale.sort(Map.Entry.comparingByKey());

                JsonObjectBuilder rotationJson = Json.createObjectBuilder();
                JsonObjectBuilder positionJson = Json.createObjectBuilder();
                JsonObjectBuilder scaleJson = Json.createObjectBuilder();

                for (var rotation_ : rotation) {
                    rotationJson.add(rotation_.getKey().toString(), rotation_.getValue());
                }

                for (var position_ : position) {
                    positionJson.add(position_.getKey().toString(), position_.getValue());
                }

                for (var scale_ : scale) {
                    scaleJson.add(scale_.getKey().toString(), scale_.getValue());
                }

                JsonObject built = Json.createObjectBuilder()
                        .add("rotation", rotationJson)
                        .add("position", positionJson)
                        .add("scale", scaleJson)
                        .build();

                bones.add(boneName, built);
            }

            JsonObject built = Json.createObjectBuilder()
                    .add("loop", animation.getString("loop", "once").equals("loop"))
                    .add("animation_length", length)
                    .add("bones", bones)
                    .add("effects", effects)
                    .build();

            animations.add(name, built);
        }

        return animations.build();
    }

    private static JsonArray mirrorForRuntime(JsonArray points, String channel) {
        boolean mirrorX = channel.equals("rotation") || channel.equals("position");
        boolean mirrorY = channel.equals("rotation");
        if (!mirrorX && !mirrorY) return points;

        JsonArrayBuilder result = Json.createArrayBuilder();
        for (JsonValue value : points) {
            if (!(value instanceof JsonObject point)) {
                result.add(value);
                continue;
            }
            JsonObjectBuilder mirrored = Json.createObjectBuilder(point);
            if (mirrorX && point.containsKey("x")) mirrored.add("x", negate(point.get("x")));
            if (mirrorY && point.containsKey("y")) mirrored.add("y", negate(point.get("y")));
            result.add(mirrored);
        }
        return result.build();
    }

    private static JsonValue negate(JsonValue value) {
        if (value instanceof JsonNumber number) {
            return Json.createValue(-number.doubleValue());
        }
        if (value instanceof JsonString string) {
            return Json.createValue("-(" + string.getString() + ")");
        }
        return value;
    }
}

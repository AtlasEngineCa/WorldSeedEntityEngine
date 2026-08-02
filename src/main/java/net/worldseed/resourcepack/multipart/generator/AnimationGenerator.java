package net.worldseed.resourcepack.multipart.generator;

import javax.json.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class AnimationGenerator {
    public static JsonObject generate(JsonArray animationRaw) {
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
                            .add("post", dataPoints)
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
}

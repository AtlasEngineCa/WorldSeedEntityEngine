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

            String name = animation.getString("name");
            double length = animation.getJsonNumber("length").doubleValue();

            JsonObjectBuilder bones = Json.createObjectBuilder();

            var foundAnimations = animation.getJsonObject("animators");
            if (foundAnimations == null) continue;

            Collection<JsonValue> animators = foundAnimations.values();

            for (var animator_ : animators) {
                JsonObject animator = animator_.asJsonObject();

                String type = animator.getString("type", "bone");

                if (!type.equals("bone")) continue;
                String boneName = animator.getString("name");

                List<Map.Entry<Double, JsonObject>> rotation = new ArrayList<>();
                List<Map.Entry<Double, JsonObject>> position = new ArrayList<>();
                List<Map.Entry<Double, JsonObject>> scale = new ArrayList<>();

                JsonArray keyframes = animator.getJsonArray("keyframes");

                for (int k = 0; k < keyframes.size(); k++) {
                    JsonObject keyframe = keyframes.getJsonObject(k);
                    String channel = keyframe.getString("channel");

                    double time = keyframe.getJsonNumber("time").doubleValue();

                    String interpolation = keyframe.getString("interpolation");

                    JsonObject built = Json.createObjectBuilder()
                            .add("post", keyframe.getJsonArray("data_points"))
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
                    .add("loop", animation.getString("loop").equals("loop"))
                    .add("animation_length", length)
                    .add("bones", bones)
                    .build();

            animations.add(name, built);
        }

        return animations.build();
    }
}

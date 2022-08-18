package net.worldseed.multipart.parser.generator;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.*;

public class AnimationGenerator {
    public static JsonObject generate(JsonArray animationRaw) {
        JsonObject animations = new JsonObject();

        for (int i = 0; i < animationRaw.size(); i++) {
            JsonObject animation = animationRaw.get(i).getAsJsonObject();

            String name = animation.get("name").getAsString();
            double length = animation.get("length").getAsDouble();

            JsonObject bones = new JsonObject();
            Set<Map.Entry<String, JsonElement>> animators = animation.get("animators").getAsJsonObject().entrySet();

            for (var animator_ : animators) {
                JsonObject animator = animator_.getValue().getAsJsonObject();

                String boneName = animator.get("name").getAsString();

                List<Map.Entry<Double, JsonObject>> rotation = new ArrayList<>();
                List<Map.Entry<Double, JsonObject>> position = new ArrayList<>();

                JsonArray keyframes = animator.get("keyframes").getAsJsonArray();

                for (int k = 0; k < keyframes.size(); k++) {
                    JsonObject keyframe = keyframes.get(k).getAsJsonObject();
                    String channel = keyframe.get("channel").getAsString();

                    double time = keyframe.get("time").getAsDouble();
                    JsonArray dataPoints = ModelGenerator.convertDatapoints(keyframe.get("data_points").getAsJsonArray().get(0).getAsJsonObject());

                    String interpolation = keyframe.get("interpolation").getAsString();

                    JsonObject built = new JsonObject();
                    built.add("post", dataPoints);
                    built.addProperty("lerp_mode", interpolation);

                    if (channel.equals("rotation")) {
                        rotation.add(Map.entry(time, built));
                    } else if (channel.equals("position")) {
                        position.add(Map.entry(time, built));
                    }
                }

                rotation.sort(Map.Entry.comparingByKey());
                position.sort(Map.Entry.comparingByKey());

                JsonObject rotationJson = new JsonObject();
                JsonObject positionJson = new JsonObject();

                for (var rotation_ : rotation) {
                    rotationJson.add(rotation_.getKey().toString(), rotation_.getValue());
                }

                for (var position_ : position) {
                    positionJson.add(position_.getKey().toString(), position_.getValue());
                }

                JsonObject built = new JsonObject();
                built.add("rotation", rotationJson);
                built.add("position", positionJson);

                bones.add(boneName, built);
            }

            JsonObject built = new JsonObject();
            built.addProperty("loop", animation.get("loop").getAsString().equals("loop"));
            built.addProperty("animation_length", length);
            built.add("bones", bones);

            animations.add(name, built);
        }

        return animations;
    }
}

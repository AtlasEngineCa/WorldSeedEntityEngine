package net.worldseed.multipart.animations;

import com.google.gson.JsonElement;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.worldseed.multipart.ModelLoader;
import net.worldseed.multipart.model_bones.ModelBone;
import net.worldseed.multipart.ModelEngine;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ModelAnimation {
    private final ModelLoader.AnimationType type;

    private static final Point RotationMul = new Vec(-1, -1, 1);
    private static final Point TranslationMul = new Vec(-1, 1, 1);

    private final Map<Short, Point> interpolationCache;

    private boolean playing = false;
    private final int length;
    private short tick = 0;

    private final String name;
    private AnimationHandlerImpl.AnimationDirection direction = AnimationHandlerImpl.AnimationDirection.FORWARD;

    public ModelLoader.AnimationType getType() {
        return type;
    }

    public boolean isPlaying() {
        return playing;
    }

    void tick() {
        if (playing) {
            if (direction == AnimationHandlerImpl.AnimationDirection.FORWARD) {
                tick++;
                if (tick > length) tick = 0;
            } else if (direction == AnimationHandlerImpl.AnimationDirection.BACKWARD) {
                tick--;
                if (tick < 0) tick = (short) length;
            }
        }
    }

    Point calculateTransform(int tick, LinkedHashMap<Double, PointInterpolation> transform) {
        double toInterpolate = tick * 50.0 / 1000;

        if (this.type == ModelLoader.AnimationType.ROTATION) {
            return Interpolator.interpolateRotation(toInterpolate, transform, length).mul(RotationMul);
        }

        return Interpolator.interpolateTranslation(toInterpolate, transform, length).mul(TranslationMul);
    }

    public Point getTransform() {
        if (!this.playing) return Pos.ZERO;
        return this.interpolationCache.getOrDefault(tick, Pos.ZERO);
    }

    public Point getTransformAtTime(int time) {
        return this.interpolationCache.getOrDefault((short) time, Pos.ZERO);
    }

    public void setDirection(AnimationHandlerImpl.AnimationDirection direction) {
        this.direction = direction;
    }

    record PointInterpolation(Point p, String lerp) {}
    ModelAnimation(String modelName, String animationName, String boneName, ModelBone bone, JsonElement keyframes, ModelLoader.AnimationType animationType, double length) {
        this.type = animationType;
        this.length = (int) (length * 20);
        this.name = animationName;

        Map<Short, Point> found;
        if (this.type == ModelLoader.AnimationType.ROTATION) {
            found = ModelLoader.getCacheRotation(modelName, bone.getName() + "/" + animationName);
        } else {
            found = ModelLoader.getCacheTranslation(modelName, bone.getName() + "/" + animationName);
        }

        if (found == null) {
            LinkedHashMap<Double, PointInterpolation> transform = new LinkedHashMap<>();

            try {
                for (Map.Entry<String, JsonElement> entry : keyframes.getAsJsonObject().entrySet()) {
                    try {
                        double time = Double.parseDouble(entry.getKey());
                        Point point = ModelEngine.getPos(entry.getValue().getAsJsonArray()).orElse(Pos.ZERO);
                        transform.put(time, new PointInterpolation(point, "linear"));
                    } catch (IllegalStateException e2) {
                        double time = Double.parseDouble(entry.getKey());
                        Point point = ModelEngine.getPos(entry.getValue().getAsJsonObject().get("post").getAsJsonArray()).orElse(Pos.ZERO);
                        String lerp = entry.getValue().getAsJsonObject().get("lerp_mode").getAsString();
                        transform.put(time, new PointInterpolation(point, lerp));
                    }
                }
            } catch (IllegalStateException e) {
                Point point = ModelEngine.getPos(keyframes.getAsJsonArray().getAsJsonArray()).orElse(Pos.ZERO);
                transform.put(0.0, new PointInterpolation(point, "linear"));
            }

            if (this.type == ModelLoader.AnimationType.ROTATION) {
                found = calculateAllTransforms(length, transform);
                ModelLoader.addToRotationCache(modelName, bone.getName() + "/" + animationName, found);
            } else {
                found = calculateAllTransforms(length, transform);
                ModelLoader.addToTranslationCache(modelName, bone.getName() + "/" + animationName, found);
            }
        }

        this.interpolationCache = found;
        bone.addAnimation(this);
    }

    private Map<Short, Point> calculateAllTransforms(double animationTime, LinkedHashMap<Double, PointInterpolation> t) {
        Map<Short, Point> transform = new HashMap<>();
        int ticks = (int) (animationTime * 20);

        for (int i = 0; i <= ticks; i++) {
            transform.put((short)i, calculateTransform(i, t));
        }

        return transform;
    }

    void stop() {
        this.tick = 0;
        this.playing = false;
        this.direction = AnimationHandler.AnimationDirection.FORWARD;
    }

    void play() {
        if (this.direction == AnimationHandler.AnimationDirection.FORWARD) this.tick = 0;
        else if (this.direction == AnimationHandler.AnimationDirection.BACKWARD) this.tick = (short) (length - 1);
        this.playing = true;
    }

    public String name() {
        return name;
    }
}

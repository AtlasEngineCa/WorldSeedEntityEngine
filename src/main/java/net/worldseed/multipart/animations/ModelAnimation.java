package net.worldseed.multipart.animations;

import com.google.gson.JsonElement;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.worldseed.multipart.ModelBone;
import net.worldseed.multipart.ModelEngine;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ModelAnimation {
    private final double animationTime;

    private final AnimationLoader.AnimationType type;

    private static final Point RotationMul = new Vec(-1, -1, 1);
    private static final Point TranslationMul = new Vec(-1, 1, 1);

    private final Map<Short, Point> interpolationCache;

    private boolean playing;

    public AnimationLoader.AnimationType getType() {
        return type;
    }

    public boolean isPlaying() {
        return playing;
    }

    public Point calculateTransform(int tick, LinkedHashMap<Double, Point> transform) {
        double toInterpolate = tick * 50.0 / 1000;
        return Interpolator.interpolate(toInterpolate, transform, animationTime).mul(this.type == AnimationLoader.AnimationType.ROTATION ? RotationMul : TranslationMul);
    }

    public Point getTransform(short tick) {
        if (!this.playing) return Pos.ZERO;
        return this.interpolationCache.getOrDefault(tick, Pos.ZERO);
    }

    public ModelAnimation(String modelName, String animationName, String boneName, ModelBone bone, JsonElement keyframes, AnimationLoader.AnimationType animationType, double animationTime) {
        this.type = animationType;
        this.animationTime = animationTime;

        if (bone == null) {
            throw new IllegalArgumentException("Cannot find bone " + boneName + " in model " + modelName + " for animation " + animationName);
        }

        Map<Short, Point> found;
        if (this.type == AnimationLoader.AnimationType.ROTATION) {
            found = AnimationLoader.getCacheRotation(modelName, bone.getName() + "/" + animationName);
        } else {
            found = AnimationLoader.getCacheTranslation(modelName, bone.getName() + "/" + animationName);
        }

        if (found == null) {
            LinkedHashMap<Double, Point> transform = new LinkedHashMap<>();

            try {
                for (Map.Entry<String, JsonElement> entry : keyframes.getAsJsonObject().entrySet()) {
                    try {
                        double time = Double.parseDouble(entry.getKey());
                        Point point = ModelEngine.getPos(entry.getValue().getAsJsonArray()).orElse(Pos.ZERO);
                        transform.put(time, point);
                    } catch (IllegalStateException e2) {
                        double time = Double.parseDouble(entry.getKey());
                        Point point = ModelEngine.getPos(entry.getValue().getAsJsonObject().get("post").getAsJsonArray()).orElse(Pos.ZERO);
                        transform.put(time, point);
                    }
                }
            } catch (IllegalStateException e) {
                Point point = ModelEngine.getPos(keyframes.getAsJsonArray().getAsJsonArray()).orElse(Pos.ZERO);
                transform.put(0.0, point);
            }

            if (this.type == AnimationLoader.AnimationType.ROTATION) {
                found = calculateAllTransforms(animationTime, transform);
                AnimationLoader.addToRotationCache(modelName, bone.getName() + "/" + animationName, found);
            } else {
                found = calculateAllTransforms(animationTime, transform);
                AnimationLoader.addToTranslationCache(modelName, bone.getName() + "/" + animationName, found);
            }
        }

        this.interpolationCache = found;
        bone.addAnimation(this);
    }

    private Map<Short, Point> calculateAllTransforms(double animationTime, LinkedHashMap<Double, Point> t) {
        Map<Short, Point> transform = new HashMap<>();
        int ticks = (int) (animationTime * 20);

        for (int i = 0; i <= ticks; i++) {
            transform.put((short)i, calculateTransform(i, t));
        }

        return transform;
    }

    public void cancel() {
        this.playing = false;
    }

    public void play() {
        this.playing = true;
    }

    public void destroy() {
        cancel();
    }
}

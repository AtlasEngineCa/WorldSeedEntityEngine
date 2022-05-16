package net.worldseed.multipart.animations;

import com.google.gson.JsonElement;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.worldseed.multipart.ModelBone;
import net.worldseed.multipart.ModelEngine;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ModelAnimation {
    private final double animationTime;

    private final AnimationLoader.AnimationType type;

    private static final Point RotationMul = new Pos(-1, -1, 1);
    private static final Point TranslationMul = new Pos(-1, 1, 1);

    private final Map<Short, Point> interpolationCache;

    private boolean playing;

    public AnimationLoader.AnimationType getType() {
        return type;
    }

    public boolean isPlaying() {
        return playing;
    }

    record StartEnd (Point s, Point e, double st, double et) {}
    private StartEnd getStartEnd(double time, LinkedHashMap<Double, Point> transform) {
        Point lastPoint = Pos.ZERO;
        double lastTime = 0;

        for (Double keyTime : transform.keySet()) {
            if (keyTime > time) {
                return new StartEnd(lastPoint, transform.get(keyTime), lastTime, keyTime);
            }

            lastPoint = transform.get(keyTime);
            lastTime = keyTime;
        }

        return new StartEnd(lastPoint, lastPoint, lastTime, animationTime);
    }

    private Point interpolate(double time, LinkedHashMap<Double, Point> transform) {
        StartEnd points = getStartEnd(time, transform);

        double timeDiff = points.et - points.st;

        if (timeDiff == 0)
            return points.s;

        double timePercent = (time - points.st) / timeDiff;

        return points.e.sub(points.s).mul(timePercent).add(points.s);
    }

    public Point calculateTransform(int tick, LinkedHashMap<Double, Point> transform) {
        double toInterpolate = tick * 50.0 / 1000;
        return interpolate(toInterpolate, transform).mul(this.type == AnimationLoader.AnimationType.ROTATION ? RotationMul : TranslationMul);
    }

    public Point getTransform(short tick) {
        if (!this.playing) return Pos.ZERO;
        return this.interpolationCache.get(tick);
    }

    public ModelAnimation(String modelName, String animationName, ModelBone bone, JsonElement keyframes, AnimationLoader.AnimationType animationType, double animationTime, float timeScalar) {
        this.type = animationType;
        this.animationTime = animationTime * timeScalar;

        if (bone == null) {
            throw new IllegalArgumentException("Bone name cannot be null");
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
                    double time = Double.parseDouble(entry.getKey()) * timeScalar;
                    Point point = ModelEngine.getPos(entry.getValue().getAsJsonArray()).orElse(Pos.ZERO);

                    transform.put(time, point);
                }
            } catch (IllegalStateException e) {
                // Not a json object
                double time = 0;

                try {
                    Point point = ModelEngine.getPos(keyframes.getAsJsonArray()).orElse(Pos.ZERO);
                    transform.put(time, point);
                } catch (IllegalStateException e2) {
                    // json object, lerp_mode thing
                    for (Map.Entry<String, JsonElement> entry : keyframes.getAsJsonObject().entrySet()) {
                        time = Double.parseDouble(entry.getKey()) * timeScalar;
                        Point point = ModelEngine.getPos(entry.getValue().getAsJsonObject().get("post").getAsJsonArray()).orElse(Pos.ZERO);

                        transform.put(time, point);
                    }
                }
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

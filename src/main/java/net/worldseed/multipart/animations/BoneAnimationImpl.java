package net.worldseed.multipart.animations;

import com.google.gson.JsonElement;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.worldseed.multipart.ModelEngine;
import net.worldseed.multipart.ModelLoader;
import net.worldseed.multipart.model_bones.ModelBone;
import net.worldseed.multipart.mql.MQLPoint;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map;

public class BoneAnimationImpl implements BoneAnimation {
    private final ModelLoader.AnimationType type;

    private final FrameProvider frameProvider;

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

    public void tick() {
        if (playing) {
            if (direction == AnimationHandlerImpl.AnimationDirection.FORWARD) {
                tick++;
                if (tick > length && length != 0) tick = 0;
            } else if (direction == AnimationHandlerImpl.AnimationDirection.BACKWARD) {
                tick--;
                if (tick < 0 && length != 0) tick = (short) length;
            }
        }
    }

    public Point getTransform() {
        if (!this.playing) return Pos.ZERO;
        return this.frameProvider.getFrame(tick);
    }

    public Point getTransformAtTime(int time) {
        return this.frameProvider.getFrame(time);
    }

    public void setDirection(AnimationHandlerImpl.AnimationDirection direction) {
        this.direction = direction;
    }

    record PointInterpolation(MQLPoint p, String lerp) {}
    BoneAnimationImpl(String modelName, String animationName, String boneName, ModelBone bone, JsonElement keyframes, ModelLoader.AnimationType animationType, double length) {
        this.type = animationType;
        this.length = (int) (length * 20);
        this.name = animationName;

        FrameProvider found;
        if (this.type == ModelLoader.AnimationType.ROTATION) {
            found = ModelLoader.getCacheRotation(modelName, bone.getName() + "/" + animationName);
        } else {
            found = ModelLoader.getCacheTranslation(modelName, bone.getName() + "/" + animationName);
        }

        if (found == null) {
            if (length != 0) {
                found = computeCachedTransforms(keyframes);

                if (this.type == ModelLoader.AnimationType.ROTATION) {
                    ModelLoader.addToRotationCache(modelName, bone.getName() + "/" + animationName, found);
                } else {
                    ModelLoader.addToTranslationCache(modelName, bone.getName() + "/" + animationName, found);
                }
            } else {
                found = computeMathTransforms(keyframes);

                if (this.type == ModelLoader.AnimationType.ROTATION) {
                    ModelLoader.addToRotationCache(modelName, bone.getName() + "/" + animationName, found);
                } else {
                    ModelLoader.addToTranslationCache(modelName, bone.getName() + "/" + animationName, found);
                }
            }
        }

        this.frameProvider = found;
        bone.addAnimation(this);
    }


    private FrameProvider computeMathTransforms(JsonElement keyframes) {
        LinkedHashMap<Double, PointInterpolation> transform = new LinkedHashMap<>();

        try {
            for (Map.Entry<String, JsonElement> entry : keyframes.getAsJsonObject().entrySet()) {
                double time = Double.parseDouble(entry.getKey());
                MQLPoint point = ModelEngine.getMQLPos(entry.getValue().getAsJsonObject().get("post").getAsJsonArray().get(0).getAsJsonObject()).orElse(MQLPoint.ZERO);
                String lerp = entry.getValue().getAsJsonObject().get("lerp_mode").getAsString();
                transform.put(time, new PointInterpolation(point, lerp));
            }
        } catch (IllegalStateException | InvocationTargetException | NoSuchMethodException |
                 InstantiationException | IllegalAccessException e) {
            try {
                e.printStackTrace();
                MQLPoint point = ModelEngine.getMQLPos(keyframes.getAsJsonObject()).orElse(MQLPoint.ZERO);
                transform.put(0.0, new PointInterpolation(point, "linear"));
            } catch (Exception e2) {
                e.printStackTrace();
            }
        }

        return new ComputedFrameProvider(transform, type, length);
    }

    private FrameProvider computeCachedTransforms(JsonElement keyframes) {
        LinkedHashMap<Double, PointInterpolation> transform = new LinkedHashMap<>();

        try {
            for (Map.Entry<String, JsonElement> entry : keyframes.getAsJsonObject().entrySet()) {
                double time = Double.parseDouble(entry.getKey());
                MQLPoint point = ModelEngine.getMQLPos(entry.getValue().getAsJsonObject().get("post").getAsJsonArray().get(0).getAsJsonObject()).orElse(MQLPoint.ZERO);
                String lerp = entry.getValue().getAsJsonObject().get("lerp_mode").getAsString();
                transform.put(time, new PointInterpolation(point, lerp));
            }
        } catch (IllegalStateException | InvocationTargetException | NoSuchMethodException |
                 InstantiationException | IllegalAccessException e) {
            try {
                e.printStackTrace();
                MQLPoint point = ModelEngine.getMQLPos(keyframes.getAsJsonObject()).orElse(MQLPoint.ZERO);
                transform.put(0.0, new PointInterpolation(point, "linear"));
            } catch (Exception e2) {
                e.printStackTrace();
            }
        }

        return new CachedFrameProvider(length, transform, type);
    }

    public void stop() {
        this.tick = 0;
        this.playing = false;
        this.direction = AnimationHandler.AnimationDirection.FORWARD;
    }

    public void play() {
        if (this.direction == AnimationHandler.AnimationDirection.FORWARD) this.tick = 0;
        else if (this.direction == AnimationHandler.AnimationDirection.BACKWARD) this.tick = (short) (length - 1);
        this.playing = true;
    }

    public String name() {
        return name;
    }
}

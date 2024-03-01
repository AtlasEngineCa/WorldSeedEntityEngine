package net.worldseed.multipart.animations;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.worldseed.multipart.ModelLoader;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class CachedFrameProvider implements FrameProvider {
    private final Map<Short, Point> interpolationCache;

    public CachedFrameProvider(int length, LinkedHashMap<Double, BoneAnimationImpl.PointInterpolation> transform, ModelLoader.AnimationType type) {
        this.interpolationCache = calculateAllTransforms(length, transform, type);
    }

    private Map<Short, Point> calculateAllTransforms(double animationTime, LinkedHashMap<Double, BoneAnimationImpl.PointInterpolation> t, ModelLoader.AnimationType type) {
        Map<Short, Point> transform = new HashMap<>();
        int ticks = (int) (animationTime * 20);

        for (int i = 0; i <= ticks; i++) {
            var p = calculateTransform(i, t, type, animationTime);
            if (type == ModelLoader.AnimationType.TRANSLATION) p = p.div(4);
            transform.put((short)i, p);
        }

        return transform;
    }

    private Point calculateTransform(int tick, LinkedHashMap<Double, BoneAnimationImpl.PointInterpolation> transform, ModelLoader.AnimationType type, double length) {
        double toInterpolate = tick * 50.0 / 1000;

        if (type == ModelLoader.AnimationType.ROTATION) {
            return Interpolator.interpolateRotation(toInterpolate, transform, length).mul(RotationMul);
        }

        return Interpolator.interpolateTranslation(toInterpolate, transform, length).mul(TranslationMul);
    }

    @Override
    public Point getFrame(int tick) {
        return interpolationCache.getOrDefault((short) tick, Vec.ZERO);
    }
}

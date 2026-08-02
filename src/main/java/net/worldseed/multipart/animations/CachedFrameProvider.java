package net.worldseed.multipart.animations;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.worldseed.multipart.ModelLoader;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class CachedFrameProvider implements FrameProvider {
    private final Map<Short, Point> interpolationCache;
    private final ModelLoader.AnimationType type;

    public CachedFrameProvider(int length, LinkedHashMap<Double, BoneAnimationImpl.PointInterpolation> transform, ModelLoader.AnimationType type) {
        this.interpolationCache = calculateAllTransforms(length, transform, type);
        this.type = type;
    }

    private Map<Short, Point> calculateAllTransforms(double animationTime, LinkedHashMap<Double, BoneAnimationImpl.PointInterpolation> t, ModelLoader.AnimationType type) {
        Map<Short, Point> transform = new HashMap<>();
        // animationTime is already in ticks (BoneAnimationImpl.length = length_seconds*20). The old
        // (animationTime*20) precomputed ~20x too many frames (and could overflow the short tick key
        // for long animations). We only ever read ticks 0..length, so cache exactly that range.
        int ticks = (int) animationTime;

        for (int i = 0; i <= ticks; i++) {
            var p = calculateTransform(i, t, type, animationTime);
            // GeoGenerator stores bones at 1/4 of the source Blockbench coordinates, then the
            // generated item model's display transform scales them back by 4. Animation keyframes
            // are still raw Blockbench coordinates, so convert them to that same 1/4 geometry
            // space here. ModelBonePartDisplay's /4 then converts both to Minecraft blocks:
            // source units * 1/4 / 4 = source units / 16.
            transform.put((short) i, p);
        }

        return transform;
    }

    private Point calculateTransform(int tick, LinkedHashMap<Double, BoneAnimationImpl.PointInterpolation> transform, ModelLoader.AnimationType type, double length) {
        double toInterpolate = tick * 50.0 / 1000;

        if (type == ModelLoader.AnimationType.ROTATION) {
            return Interpolator.interpolateRotation(toInterpolate, transform, length).mul(RotationMul);
        } else if (type == ModelLoader.AnimationType.SCALE) {
            return Interpolator.interpolateScale(toInterpolate, transform, length);
        } else if (type == ModelLoader.AnimationType.TRANSLATION) {
            return Interpolator.interpolateTranslation(toInterpolate, transform, length)
                    .mul(TranslationMul)
                    .mul(0.25);
        }

        return Vec.ZERO;
    }

    @Override
    public Point getFrame(int tick) {
        return interpolationCache.getOrDefault((short) tick, switch (type) {
            case TRANSLATION, ROTATION -> Vec.ZERO;
            case SCALE -> Vec.ONE;
        });
    }
}

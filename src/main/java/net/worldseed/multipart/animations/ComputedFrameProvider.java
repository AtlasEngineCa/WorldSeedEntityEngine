package net.worldseed.multipart.animations;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.worldseed.multipart.ModelLoader;

import java.util.LinkedHashMap;

public class ComputedFrameProvider implements FrameProvider {
    private final LinkedHashMap<Double, BoneAnimationImpl.PointInterpolation> transform;
    private final ModelLoader.AnimationType type;
    private final Double lowestKey;

    public ComputedFrameProvider(LinkedHashMap<Double, BoneAnimationImpl.PointInterpolation> transform, ModelLoader.AnimationType type, int length) {
        this.transform = transform;
        this.type = type;
        this.lowestKey = transform.keySet().stream().min(Double::compareTo).orElse(0.0);
    }

    @Override
    public Point getFrame(int tick) {
        var first = transform.get(lowestKey);
        if (first == null) return switch (type) {
            case TRANSLATION, ROTATION -> Vec.ZERO;
            case SCALE -> Vec.ONE;
        };

        double toInterpolate = tick * 50.0 / 1000;
        var point = first.p().evaluate(toInterpolate);

        if (type == ModelLoader.AnimationType.ROTATION) return point.mul(RotationMul);
        // Match GeoGenerator's 1/4 geometry space. ModelBonePartDisplay performs the remaining /4
        // block conversion, yielding the standard 16 Blockbench units per Minecraft block.
        if (type == ModelLoader.AnimationType.TRANSLATION) return point.mul(TranslationMul).mul(0.25);
        return point;
    }
}

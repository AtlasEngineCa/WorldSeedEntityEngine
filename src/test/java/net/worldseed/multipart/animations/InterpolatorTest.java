package net.worldseed.multipart.animations;

import net.worldseed.multipart.animations.BoneAnimationImpl.PointInterpolation;
import net.worldseed.multipart.mql.MQLPoint;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Blockbench-faithful interpolation behaviour (validated end-to-end against the bbrender oracle;
 *  these unit assertions lock the per-mode math in without needing the oracle at CI time). */
class InterpolatorTest {

    private static PointInterpolation kf(double v, String lerp) {
        return new PointInterpolation(new MQLPoint(v, 0, 0), lerp);
    }

    private static LinkedHashMap<Double, PointInterpolation> map(Object... kv) {
        var m = new LinkedHashMap<Double, PointInterpolation>();
        for (int i = 0; i < kv.length; i += 2) m.put((Double) kv[i], (PointInterpolation) kv[i + 1]);
        return m;
    }

    @Test
    void linearInterpolatesBetweenKeyframes() {
        var t = map(0.0, kf(0, "linear"), 1.0, kf(10, "linear"));
        assertEquals(5.0, Interpolator.interpolateTranslation(0.5, t, 1.0).x(), 1e-9);
    }

    @Test
    void stepHoldsStartUntilNextKeyframe() {
        var t = map(0.0, kf(0, "step"), 1.0, kf(10, "step"));
        assertEquals(0.0, Interpolator.interpolateTranslation(0.5, t, 1.0).x(), 1e-9, "step must hold, not smooth");
        assertEquals(10.0, Interpolator.interpolateTranslation(1.0, t, 1.0).x(), 1e-9);
    }

    @Test
    void catmullRomMatchesUniformSpline() {
        // 4 keyframes 0,0,10,10; segment [1,2] at alpha 0.5 -> uniform Catmull-Rom = 5.0
        var t = map(0.0, kf(0, "catmullrom"), 1.0, kf(0, "catmullrom"),
                    2.0, kf(10, "catmullrom"), 3.0, kf(10, "catmullrom"));
        assertEquals(5.0, Interpolator.interpolateTranslation(1.5, t, 3.0).x(), 1e-9);
    }

    @Test
    void translationAndScaleRespectInterpolationMode() {
        // previously translation/scale ignored lerp_mode (always linear); step must now hold for them too
        var t = map(0.0, kf(2, "step"), 1.0, kf(8, "step"));
        assertEquals(2.0, Interpolator.interpolateScale(0.5, t, 1.0).x(), 1e-9);
    }

    @Test
    void rotationInterpolatesEulerComponentsNotQuaternionSlerp() {
        // a full-turn spin 0 -> -360 must read -180 at the midpoint (component lerp), not collapse via slerp
        var t = map(0.0, kf(0, "linear"), 1.0, kf(-360, "linear"));
        assertEquals(-180.0, Interpolator.interpolateRotation(0.5, t, 1.0).x(), 1e-9);
    }

    @Test
    void bezierFallsBackToLinearWithoutExportedHandles() {
        var t = map(0.0, kf(0, "bezier"), 1.0, kf(0, "bezier"), 2.0, kf(10, "bezier"), 3.0, kf(20, "bezier"));
        assertEquals(5.0, Interpolator.interpolateTranslation(1.5, t, 3.0).x(), 1e-9);
    }

    @Test
    void discontinuousKeyframeUsesLeavingValue() {
        var t = new LinkedHashMap<Double, BoneAnimationImpl.PointInterpolation>();
        // keyframe at t=0 jumps: approached as 0, leaves as 5; then linear to 10 at t=1
        t.put(0.0, new BoneAnimationImpl.PointInterpolation(new MQLPoint(0, 0, 0), new MQLPoint(5, 0, 0), "linear"));
        t.put(1.0, new BoneAnimationImpl.PointInterpolation(new MQLPoint(10, 0, 0), "linear"));
        // segment leaves keyframe 0 at 5, approaches keyframe 1 at 10 -> midpoint 7.5
        assertEquals(7.5, Interpolator.interpolateTranslation(0.5, t, 1.0).x(), 1e-9);
    }
}

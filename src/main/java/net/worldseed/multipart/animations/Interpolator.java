package net.worldseed.multipart.animations;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Blockbench-faithful keyframe interpolation for a single animation channel (rotation / translation /
 * scale), evaluated per component. Supports:
 * <ul>
 *   <li><b>step</b> – hold the start keyframe value until the next keyframe,</li>
 *   <li><b>linear</b> – straight lerp between the two bracketing keyframes,</li>
 *   <li><b>catmullrom</b> ("smooth") – uniform Catmull-Rom through the 4-keyframe neighbourhood,
 *       clamped at the ends. A segment is smooth if <i>either</i> of its endpoint keyframes is
 *       catmullrom (matches Blockbench and the reference renderer).</li>
 * </ul>
 * Bezier keyframes are approximated as linear (their tangent handles are not carried through the
 * generated animation JSON). Rotations are interpolated on their Euler components exactly like
 * Blockbench – no quaternion slerp – so multi-turn spins and eased rotations match the editor.
 */
public class Interpolator {

    static Point interpolateRotation(double time, LinkedHashMap<Double, BoneAnimationImpl.PointInterpolation> transform, double animationTime) {
        return interpolate(time, transform, Vec.ZERO);
    }

    static Point interpolateTranslation(double time, LinkedHashMap<Double, BoneAnimationImpl.PointInterpolation> transform, double animationTime) {
        return interpolate(time, transform, Vec.ZERO);
    }

    public static Point interpolateScale(double time, LinkedHashMap<Double, BoneAnimationImpl.PointInterpolation> transform, double animationTime) {
        return interpolate(time, transform, Vec.ONE);
    }

    private static Point interpolate(double time, LinkedHashMap<Double, BoneAnimationImpl.PointInterpolation> transform, Point fallback) {
        if (transform.isEmpty()) return fallback;
        List<Double> times = new ArrayList<>(transform.keySet());   // insertion order == ascending keyframe time
        int n = times.size();

        // find segment [i, i+1] with times[i] <= time < times[i+1]
        int i = 0;
        while (i + 1 < n && times.get(i + 1) <= time) i++;

        if (time <= times.get(0) || i >= n - 1) return pre(transform, times.get(i), time); // before first / past last -> hold

        BoneAnimationImpl.PointInterpolation a = transform.get(times.get(i));
        BoneAnimationImpl.PointInterpolation b = transform.get(times.get(i + 1));
        double ta = times.get(i), tb = times.get(i + 1);
        double alpha = tb == ta ? 0 : (time - ta) / (tb - ta);

        Vec start = post(transform, times.get(i), time);            // value leaving keyframe i
        if ("step".equals(a.lerp())) return start;                  // hold the leaving value until next keyframe

        Vec end = pre(transform, times.get(i + 1), time);           // value approaching keyframe i+1
        if (isSmooth(a.lerp()) || isSmooth(b.lerp())) {
            Vec before = post(transform, times.get(Math.max(0, i - 1)), time);
            Vec after = pre(transform, times.get(Math.min(n - 1, i + 2)), time);
            return catmullRom(before, start, end, after, alpha);
        }
        return start.lerp(end, alpha);                              // linear
    }

    private static boolean isSmooth(String lerp) {
        return "catmullrom".equals(lerp);
    }

    // value approaching a keyframe (its primary / "pre" data point)
    private static Vec pre(LinkedHashMap<Double, BoneAnimationImpl.PointInterpolation> transform, double key, double time) {
        return transform.get(key).p().evaluate(time).asVec();
    }

    // value leaving a keyframe (its second "post" data point on a discontinuity, else same as pre)
    private static Vec post(LinkedHashMap<Double, BoneAnimationImpl.PointInterpolation> transform, double key, double time) {
        return transform.get(key).post().evaluate(time).asVec();
    }

    /** Uniform Catmull-Rom through p1..p2 with neighbours p0,p3 (matches Blockbench / bbrender). */
    static Vec catmullRom(Vec p0, Vec p1, Vec p2, Vec p3, double t) {
        return new Vec(cr(p0.x(), p1.x(), p2.x(), p3.x(), t),
                       cr(p0.y(), p1.y(), p2.y(), p3.y(), t),
                       cr(p0.z(), p1.z(), p2.z(), p3.z(), t));
    }

    private static double cr(double p0, double p1, double p2, double p3, double t) {
        double t2 = t * t, t3 = t2 * t;
        return 0.5 * ((2 * p1)
                + (-p0 + p2) * t
                + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2
                + (-p0 + 3 * p1 - 3 * p2 + p3) * t3);
    }
}

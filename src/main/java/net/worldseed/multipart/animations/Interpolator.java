package net.worldseed.multipart.animations;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.worldseed.multipart.Quaternion;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;

public class Interpolator {
    private static @Nullable StartEnd getStartEnd(double time, LinkedHashMap<Double, BoneAnimationImpl.PointInterpolation> transform, double animationTime) {
        if (transform.isEmpty()) return null;
        BoneAnimationImpl.PointInterpolation lastPoint = transform.get(transform.keySet().iterator().next());
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

    static Quaternion slerp(Quaternion qa, Quaternion qb, double t) {
        // quaternion to return
        // Calculate angle between them.
        double cosHalfTheta = qa.w() * qb.w() + qa.x() * qb.x() + qa.y() * qb.y() + qa.z() * qb.z();
        // if qa=qb or qa=-qb then theta = 0 and we can return qa
        if (Math.abs(cosHalfTheta) >= 1.0) {
            double qmw = qa.w();
            double qmx = qa.x();
            double qmy = qa.y();
            double qmz = qa.z();
            return new Quaternion(qmx, qmy, qmz, qmw);
        }
        // Calculate temporary values.
        double halfTheta = Math.acos(cosHalfTheta);
        double sinHalfTheta = Math.sqrt(1.0 - cosHalfTheta * cosHalfTheta);
        // if theta = 180 degrees then result is not fully defined
        // we could rotate around any axis normal to qa or qb
        if (Math.abs(sinHalfTheta) < 0.001) { // fabs is floating point absolute
            double qmw = (qa.w() * 0.5 + qb.w() * 0.5);
            double qmx = (qa.x() * 0.5 + qb.x() * 0.5);
            double qmy = (qa.y() * 0.5 + qb.y() * 0.5);
            double qmz = (qa.z() * 0.5 + qb.z() * 0.5);
            return new Quaternion(qmx, qmy, qmz, qmw);
        }
        double ratioA = Math.sin((1 - t) * halfTheta) / sinHalfTheta;
        double ratioB = Math.sin(t * halfTheta) / sinHalfTheta;
        //calculate Quaternion.
        double qmw = (qa.w() * ratioA + qb.w() * ratioB);
        double qmx = (qa.x() * ratioA + qb.x() * ratioB);
        double qmy = (qa.y() * ratioA + qb.y() * ratioB);
        double qmz = (qa.z() * ratioA + qb.z() * ratioB);
        return new Quaternion(qmx, qmy, qmz, qmw);
    }

    static Point interpolateRotation(double time, LinkedHashMap<Double, BoneAnimationImpl.PointInterpolation> transform, double animationTime) {
        StartEnd points = getStartEnd(time, transform, animationTime);
        if (points == null) return Vec.ZERO;

        double timeDiff = points.et - points.st;

        if (timeDiff == 0)
            return points.s.p().evaluate(time);

        double timePercent = (time - points.st) / timeDiff;

        if (points.s.lerp().equals("linear")) {
            Vec ps = Vec.fromPoint(points.s.p().evaluate(time));
            Vec pe = Vec.fromPoint(points.e.p().evaluate(time));

            return ps.lerp(pe, timePercent);
        } else {
            Quaternion qa = new Quaternion(points.s.p().evaluate(time).div(5));
            Quaternion qb = new Quaternion(points.e.p().evaluate(time).div(5));
            return slerp(qa, qb, timePercent).toEuler().mul(5);
        }
    }

    static Point interpolateTranslation(double time, LinkedHashMap<Double, BoneAnimationImpl.PointInterpolation> transform, double animationTime) {
        StartEnd points = getStartEnd(time, transform, animationTime);
        if (points == null) return Vec.ZERO;

        double timeDiff = points.et - points.st;

        if (timeDiff == 0) return points.s.p().evaluate(time);
        double timePercent = (time - points.st) / timeDiff;

        Vec ps = Vec.fromPoint(points.s.p().evaluate(time));
        Vec pe = Vec.fromPoint(points.e.p().evaluate(time));

        return ps.lerp(pe, timePercent);
    }

    public static Point interpolateScale(double time, LinkedHashMap<Double, BoneAnimationImpl.PointInterpolation> transform, double animationTime) {
        StartEnd points = getStartEnd(time, transform, animationTime);
        if (points == null) return Vec.ONE;

        double timeDiff = points.et - points.st;

        if (timeDiff == 0) return points.s.p().evaluate(time);
        double timePercent = (time - points.st) / timeDiff;

        Vec ps = Vec.fromPoint(points.s.p().evaluate(time));
        Vec pe = Vec.fromPoint(points.e.p().evaluate(time));

        return ps.lerp(pe, timePercent);
    }

    record StartEnd(BoneAnimationImpl.PointInterpolation s, BoneAnimationImpl.PointInterpolation e, double st,
                    double et) {
    }
}

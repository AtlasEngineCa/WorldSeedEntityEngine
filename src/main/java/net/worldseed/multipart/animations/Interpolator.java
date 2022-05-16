package net.worldseed.multipart.animations;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;

import java.util.LinkedHashMap;

public class Interpolator {
    record StartEnd (Point s, Point e, double st, double et) {}
    private static StartEnd getStartEnd(double time, LinkedHashMap<Double, Point> transform, double animationTime) {
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

    static Point interpolate(double time, LinkedHashMap<Double, Point> transform, double animationTime) {
        StartEnd points = getStartEnd(time, transform, animationTime);

        double timeDiff = points.et - points.st;

        if (timeDiff == 0)
            return points.s;

        double timePercent = (time - points.st) / timeDiff;

        return points.e.sub(points.s).mul(timePercent).add(points.s);
    }

}

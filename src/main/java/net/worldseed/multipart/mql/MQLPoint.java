package net.worldseed.multipart.mql;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;

import java.lang.reflect.InvocationTargetException;

public class MQLPoint {
    public static final MQLPoint ZERO = new MQLPoint();
    MQLEvaluator molangX = null;
    MQLEvaluator molangY = null;
    MQLEvaluator molangZ = null;
    double x = 0;
    double y = 0;
    double z = 0;
    MQLData data = new MQLData();

    public MQLPoint() {
        x = 0;
        y = 0;
        z = 0;
    }

    public MQLPoint(double x_, double y_, double z_) {
        x = x_;
        y = y_;
        z = z_;
    }

    public MQLPoint(JsonObject json) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        JsonElement fx = json.get("x");
        if (fx != null) {
            try {
                x = fx.getAsDouble();
            } catch (Exception ignored) {
                molangX = fromString(fx.getAsString());
            }
        }

        JsonElement fy = json.get("y");
        if (fy != null) {
            try {
                y = fy.getAsDouble();
            } catch (Exception ignored) {
                molangY = fromString(fy.getAsString());
            }
        }

        JsonElement fz = json.get("z");
        if (fz != null) {
            try {
                z = fz.getAsDouble();
            } catch (Exception ignored) {
                molangZ = fromString(fz.getAsString());
            }
        }
    }

    public MQLPoint(JsonArray arr) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        JsonElement fx = arr.get(0);
        if (fx != null) {
            try {
                x = fx.getAsDouble();
            } catch (Exception ignored) {
                molangX = fromString(fx.getAsString());
            }
        }

        JsonElement fy = arr.get(1);
        if (fy != null) {
            try {
                y = fy.getAsDouble();
            } catch (Exception ignored) {
                molangY = fromString(fy.getAsString());
            }
        }

        JsonElement fz = arr.get(2);
        if (fz != null) {
            try {
                z = fz.getAsDouble();
            } catch (Exception ignored) {
                molangZ = fromString(fz.getAsString());
            }
        }
    }

    static MQLEvaluator fromDouble(double value) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        return fromString(Double.toString(value));
    }

    // Compile each expression once into a reusable, pure Molang evaluator (case handled inside Molang) and
    // cache by source so identical keyframe expressions across a model share one compiled tree.
    private static final java.util.Map<String, MQLEvaluator> COMPILE_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    static MQLEvaluator fromString(String s) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        if (s == null || s.isBlank()) return env -> 0;
        // key by the lower-cased source so case-only variants (Math./math., Query./query.) share one compiled tree
        return COMPILE_CACHE.computeIfAbsent(s.trim().toLowerCase(java.util.Locale.ROOT), Molang::compile);
    }

    public Point evaluate(double time) {
        data.setTime(time);
        data.setLifeTime(time); // approximated by anim_time until a real life time is threaded through

        double evaluatedX = x;
        if (molangX != null) {
            try {
                evaluatedX = molangX.evaluate(data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        double evaluatedY = y;
        if (molangY != null) {
            try {
                evaluatedY = molangY.evaluate(data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        double evaluatedZ = z;
        if (molangZ != null) {
            try {
                evaluatedZ = molangZ.evaluate(data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return new Vec(evaluatedX, evaluatedY, evaluatedZ);
    }

    @Override
    public String toString() {
        if (molangX != null || molangY != null || molangZ != null) {
            return "MQLPoint{" +
                    "x=" + molangX +
                    ", y=" + molangY +
                    ", z=" + molangZ +
                    '}';
        }

        return "MQLPoint{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                '}';
    }
}

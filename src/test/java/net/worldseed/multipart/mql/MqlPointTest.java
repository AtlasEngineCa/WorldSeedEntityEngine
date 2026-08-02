package net.worldseed.multipart.mql;

import com.google.gson.JsonObject;
import net.minestom.server.coordinate.Point;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/** Molang keyframe expressions: case-insensitive namespace handling + compiled-expression caching. */
class MqlPointTest {

    @Test
    void namespacesAreCaseInsensitiveAndEvaluate() throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("x", "Query.anim_time"); // capital Q normalizes to query.
        json.addProperty("y", "q.anim_time");      // short form
        json.addProperty("z", "2 + 3");            // literal arithmetic

        Point result = new MQLPoint(json).evaluate(4.0);

        assertEquals(4.0, result.x(), 1e-9);
        assertEquals(4.0, result.y(), 1e-9);
        assertEquals(5.0, result.z(), 1e-9);
    }

    @Test
    void identicalExpressionsShareOneCompiledEvaluator() throws Exception {
        MQLEvaluator a = MQLPoint.fromString("query.anim_time * 2");
        MQLEvaluator b = MQLPoint.fromString("Query.anim_time * 2"); // same after case-normalization

        assertSame(a, b, "identical (case-insensitive) expressions must reuse the cached compiled class");
    }

    @Test
    void mathTernaryAndPrecedence() throws Exception {
        MQLData env = new MQLData();
        env.setTime(2.0);

        assertEquals(1.0, MQLPoint.fromString("math.cos(0)").evaluate(env), 1e-9);
        assertEquals(1.0, MQLPoint.fromString("math.sin(90)").evaluate(env), 1e-9, "trig is in degrees");
        assertEquals(14.0, MQLPoint.fromString("2 + 3 * 4").evaluate(env), 1e-9, "precedence");
        assertEquals(10.0, MQLPoint.fromString("query.anim_time > 1 ? 10 : 20").evaluate(env), 1e-9);

        env.setTime(0.0);
        assertEquals(20.0, MQLPoint.fromString("query.anim_time > 1 ? 10 : 20").evaluate(env), 1e-9);
    }
}

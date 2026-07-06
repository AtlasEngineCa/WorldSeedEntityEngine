package net.worldseed.multipart.mql;

/** A compiled Molang keyframe expression. Pure and reusable — the {@link MQLData} is supplied per call. */
@FunctionalInterface
public interface MQLEvaluator {
    double evaluate(MQLData data);
}

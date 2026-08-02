package net.worldseed.multipart.mql;

/**
 * The Molang query environment exposed to keyframe expressions as {@code query.*} / {@code q.*}, read by the
 * {@link Molang} evaluator. {@code life_time} is threaded when available and otherwise approximated by
 * {@code anim_time}.
 */
public class MQLData {
    private double time;
    private double lifeTime;

    public void setTime(double time) {
        this.time = time;
    }

    public void setLifeTime(double lifeTime) {
        this.lifeTime = lifeTime;
    }

    /** Seconds since the current animation started (resets each loop). */
    public double anim_time() {
        return time;
    }

    /** Alias of {@link #anim_time()}. */
    public double time() {
        return time;
    }

    /** Seconds since the model spawned (does not reset on loop); approximated by anim_time if not supplied. */
    public double life_time() {
        return lifeTime;
    }

    /** Length of one server tick in seconds. */
    public double delta_time() {
        return 1.0 / 20.0;
    }
}

package net.worldseed.multipart.animations;

import java.util.Set;

public interface ModelAnimation {
    int priority();

    int animationTime();

    String name();

    AnimationHandler.AnimationDirection direction();
    Set<String> getAnimatedBones();

    void setDirection(AnimationHandler.AnimationDirection direction);

    void stop();

    void stop(Set<String> animatedBones);

    void play(boolean resume);

    void tick();

    /** Current playback position in ticks while playing, or -1 if not playing. Used to time animation effects. */
    default int currentTick() {
        return -1;
    }

    /** Whether this animation loops (Blockbench loop mode). Non-looping animations hold their last frame. */
    default boolean loops() {
        return true;
    }

    /** Current blend weight in [0,1]. */
    default double weight() {
        return 1.0;
    }

    /** Set the blend weight immediately. */
    default void setWeight(double weight) {
    }

    /** Ramp the blend weight toward {@code target} over {@code ticks} ticks (0 = instant). */
    default void blendTo(double target, int ticks) {
    }

    /** True once this animation has finished blending out (weight and target both ~0) — safe to stop. */
    default boolean fadedOut() {
        return false;
    }

}

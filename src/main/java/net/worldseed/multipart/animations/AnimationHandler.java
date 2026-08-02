package net.worldseed.multipart.animations;

import com.google.gson.JsonElement;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public interface AnimationHandler {
    void registerAnimation(String name, JsonElement animation, int priority);

    void registerAnimation(ModelAnimation animator);

    /**
     * Play an animation on repeat
     *
     * @param animation name of animation to play
     */
    void playRepeat(String animation) throws IllegalArgumentException;

    void playRepeat(String animation, AnimationDirection direction) throws IllegalArgumentException;

    /**
     * Crossfade to a repeating animation over {@code blendTicks} ticks: it blends in while whatever is
     * currently playing blends out (weighted blend). {@code blendTicks <= 0} is a hard swap.
     *
     * @param animation name of the animation to blend to
     * @param blendTicks blend duration in ticks
     */
    void playRepeat(String animation, int blendTicks) throws IllegalArgumentException;

    void playRepeat(String animation, AnimationDirection direction, int blendTicks) throws IllegalArgumentException;

    /**
     * Stop a repeating animation
     *
     * @param animation name of animation to stop
     */
    void stopRepeat(String animation) throws IllegalArgumentException;


    /**
     * Play an animation once
     *
     * @param animation name of animation to play
     * @param override If true (default), fully overrides repeating background animations. If false, overrides only bones used in new animation.
     * @param cb       callback to call when animation is finished
     */
    void playOnce(String animation, boolean override, Runnable cb) throws IllegalArgumentException;

    /**
     * Play an animation once
     *
     * @param animation name of animation to play
     * @param cb        callback to call when animation is finished
     */
    void playOnce(String animation, Runnable cb) throws IllegalArgumentException;

    /**
     * Crossfade from the current repeat into a one-shot, then crossfade back over the final
     * {@code blendTicks}. This is the only supported overlap: weights are complementary and no
     * unrelated full-strength animations can run concurrently.
     */
    void playOnce(String animation, int blendTicks, Runnable cb) throws IllegalArgumentException;

    void playOnce(String animation, AnimationHandlerImpl.AnimationDirection direction, boolean override, Runnable cb) throws IllegalArgumentException;

    /**
     * Destroy the animation handler
     */
    void destroy();

    /**
     * Get the current animation
     *
     * @return current animation
     */
    @Nullable String getPlaying();


    /**
     * Get the current repeating animation
     *
     * @return current repeating animation
     */
    @Nullable String getRepeating();

    /**
     * Get an animation by key
     *
     * @return animation object
     */
    @Nullable ModelAnimation getAnimation(String animation);

    Map<String, Integer> animationPriorities();

    /** The built-in effect handler: plays the sound / particle whose id is a valid Minecraft key at the
     *  effect's locator (or model origin) for the model's viewers. Timeline effects are ignored. */
    AnimationEffectHandler DEFAULT_EFFECT_HANDLER = AnimationHandlerImpl::playDefaultEffect;

    /**
     * Set how Blockbench animation effects (sound / particle / timeline) are handled when they fire.
     * Replace the {@link #DEFAULT_EFFECT_HANDLER} to map effect ids to your own sounds/particles/logic.
     *
     * @param handler the handler, or null to ignore all effects
     */
    void setEffectHandler(AnimationEffectHandler handler);

    enum AnimationDirection {
        FORWARD,
        BACKWARD,
        PAUSE
    }
}

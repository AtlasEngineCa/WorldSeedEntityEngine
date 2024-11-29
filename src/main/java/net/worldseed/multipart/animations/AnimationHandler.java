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

    enum AnimationDirection {
        FORWARD,
        BACKWARD,
        PAUSE
    }
}

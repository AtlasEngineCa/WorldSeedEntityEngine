package net.worldseed.multipart.animations;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minestom.server.coordinate.Point;

import java.util.Map;
import java.util.function.Consumer;

public interface AnimationHandler {
    enum AnimationDirection {
        FORWARD,
        BACKWARD,
        PAUSE
    }

    void registerAnimation(String name, JsonElement animation, int priority);

    /**
     * Play an animation on repeat
     * @param animation name of animation to play
     */
    void playRepeat(String animation);
    void playRepeat(String animation, AnimationDirection direction);

    /**
     * Stop a repeating animation
     * @param animation name of animation to stop
     */
    void stopRepeat(String animation);

    /**
     * Play an animation once
     * @param animation name of animation to play
     * @param cb callback to call when animation is finished
     */
    void playOnce(String animation, Consumer<Void> cb);
    void playOnce(String animation, AnimationHandlerImpl.AnimationDirection direction, Consumer<Void> cb);

    /**
     * Destroy the animation handler
     */
    void destroy();

    /**
     * Get the current animation
     * @return current animation
     */
    String getPlaying();

    Map<String, Integer> animationPriorities();
}
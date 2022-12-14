package net.worldseed.multipart.animations;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minestom.server.coordinate.Point;

import java.util.Map;
import java.util.function.Consumer;

public interface AnimationHandler {
    void registerAnimation(String name, JsonElement animation, int priority);

    /**
     * Play an animation on repeat
     * @param animation name of animation to play
     */
    void playRepeat(String animation);

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

    /**
     * Play an animation once without stopping other playing animations.
     * @param animation name of animation to play
     * @param cb callback to call when animation is finished
     */
    void playOnceConcurrent(String animation, Consumer<Void> cb);

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
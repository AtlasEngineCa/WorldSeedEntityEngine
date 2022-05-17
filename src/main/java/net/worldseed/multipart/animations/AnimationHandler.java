package net.worldseed.multipart.animations;

import java.util.function.Consumer;

public interface AnimationHandler {
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
     * Destroy the animation handler
     */
    void destroy();

    /**
     * Get the current animation
     * @return current animation
     */
    String getPlaying();

    /**
     * Toggle the animation handler on or off
     */
    void setUpdates(boolean updates);
}
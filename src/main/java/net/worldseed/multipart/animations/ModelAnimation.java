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

}

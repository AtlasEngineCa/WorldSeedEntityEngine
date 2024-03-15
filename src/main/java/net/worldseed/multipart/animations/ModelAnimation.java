package net.worldseed.multipart.animations;

public interface ModelAnimation {
    int priority();

    int animationTime();

    String name();

    AnimationHandler.AnimationDirection direction();

    void setDirection(AnimationHandler.AnimationDirection direction);

    void stop();

    void play();

    void tick();
}

package net.worldseed.multipart.animations;

import net.minestom.server.coordinate.Point;
import net.worldseed.multipart.ModelLoader;

public interface BoneAnimation {
    String name();

    String boneName();

    ModelLoader.AnimationType getType();

    Point getTransformAtTime(int time);

    boolean isPlaying();

    Point getTransform();

    void setDirection(AnimationHandler.AnimationDirection direction);

    void stop();

    void play();

    void tick();
    void resume(short tick);
    short getTick();
}

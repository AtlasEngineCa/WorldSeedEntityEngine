package net.worldseed.gestures;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.worldseed.multipart.ModelLoader;
import net.worldseed.multipart.animations.AnimationHandler;
import net.worldseed.multipart.animations.BoneAnimation;

final class ProceduralBoneAnimation implements BoneAnimation {
    private final String boneName;
    private final ModelLoader.AnimationType type;
    private volatile Point transform;

    ProceduralBoneAnimation(String boneName, ModelLoader.AnimationType type) {
        this.boneName = boneName;
        this.type = type;
        this.transform = type == ModelLoader.AnimationType.SCALE ? Vec.ONE : Vec.ZERO;
    }

    void setTransform(Point transform) {
        this.transform = transform;
    }

    @Override public String name() { return "minecraft:player"; }
    @Override public String boneName() { return boneName; }
    @Override public ModelLoader.AnimationType getType() { return type; }
    @Override public Point getTransformAtTime(int time) { return transform; }
    @Override public boolean isPlaying() { return true; }
    @Override public Point getTransform() { return transform; }
    @Override public void setDirection(AnimationHandler.AnimationDirection direction) {}
    @Override public void stop() {}
    @Override public void play() {}
    @Override public void tick() {}
    @Override public void resume(short tick) {}
    @Override public short getTick() { return 0; }
}

package net.worldseed.multipart.animations;

import java.util.HashSet;
import java.util.Set;

public class ModelAnimationClassic implements ModelAnimation {
    private final String name;
    private final int priority;
    int animationTime;
    AnimationHandler.AnimationDirection direction;
    Set<BoneAnimation> boneAnimations;

    public ModelAnimationClassic(String name, int animationTime, int priority, HashSet<BoneAnimation> animationSet) {
        this.direction = AnimationHandler.AnimationDirection.PAUSE;
        this.animationTime = animationTime;
        this.boneAnimations = animationSet;
        this.name = name;
        this.priority = priority;
    }

    @Override
    public int priority() {
        return priority;
    }

    @Override
    public int animationTime() {
        return animationTime;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public AnimationHandler.AnimationDirection direction() {
        return direction;
    }

    @Override
    public void setDirection(AnimationHandler.AnimationDirection direction) {
        this.direction = direction;
        boneAnimations.forEach(a -> a.setDirection(direction));
    }

    @Override
    public void stop() {
        boneAnimations.forEach(BoneAnimation::stop);
    }

    @Override
    public void play() {
        boneAnimations.forEach(BoneAnimation::play);
    }

    @Override
    public void tick() {
        boneAnimations.forEach(BoneAnimation::tick);
    }
}

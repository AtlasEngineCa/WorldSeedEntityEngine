package net.worldseed.multipart.animations;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class ModelAnimationClassic implements ModelAnimation {
    private final String name;
    private final int priority;
    private final int animationTime;
    private AnimationHandler.AnimationDirection direction;
    private final Set<BoneAnimation> boneAnimations;
    private final Set<String> animatedBones;

    public ModelAnimationClassic(String name, int animationTime, int priority, HashSet<BoneAnimation> animationSet, HashSet<String> animatedBones) {
        this.direction = AnimationHandler.AnimationDirection.PAUSE;
        this.animationTime = animationTime;
        this.boneAnimations = animationSet;
        this.animatedBones = animatedBones;
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
    public void stop(Set<String> animatedBones) { //Only stop bones that are in the new playOnce animation
        boneAnimations.forEach(boneAnimation -> {
            if (animatedBones.contains(boneAnimation.boneName())) {
                boneAnimation.stop();
            }
        });
    }

    @Override
    public void play(boolean resume) {
        if (resume) {
            Optional<Short> tick = boneAnimations.stream().filter(BoneAnimation::isPlaying).findFirst().map(BoneAnimation::getTick);
            if (tick.isPresent()) {
                boneAnimations.forEach(boneAnimation -> boneAnimation.resume(tick.get()));
                return;
            }
        }
        boneAnimations.forEach(BoneAnimation::play);
    }

    @Override
    public void tick() {
        boneAnimations.forEach(BoneAnimation::tick);
    }

    public Set<String> getAnimatedBones() {
        return animatedBones;
    }
}

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
    private final boolean looping;

    public ModelAnimationClassic(String name, int animationTime, int priority, HashSet<BoneAnimation> animationSet, HashSet<String> animatedBones, boolean looping) {
        this.direction = AnimationHandler.AnimationDirection.PAUSE;
        this.animationTime = animationTime;
        this.boneAnimations = animationSet;
        this.animatedBones = animatedBones;
        this.name = name;
        this.priority = priority;
        this.looping = looping;
    }

    @Override
    public boolean loops() {
        return looping;
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

    private int effectTick = -1; // standalone playback clock so effects-only animations (no bones) still fire
    private double weight = 1.0;
    private double targetWeight = 1.0;
    private double weightStep = 0.0;

    @Override
    public double weight() {
        return weight;
    }

    @Override
    public void setWeight(double w) {
        this.weight = w;
        this.targetWeight = w;
        this.weightStep = 0;
        boneAnimations.forEach(b -> b.setWeight(w));
    }

    @Override
    public void blendTo(double target, int ticks) {
        this.targetWeight = target;
        this.weightStep = ticks <= 0 ? (target - weight) : (target - weight) / ticks;
        if (ticks <= 0) setWeight(target);
    }

    @Override
    public boolean fadedOut() {
        return weight <= 0.001 && targetWeight <= 0.001;
    }

    @Override
    public void stop() {
        boneAnimations.forEach(BoneAnimation::stop);
        this.effectTick = -1;
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
        this.effectTick = 0;
    }

    @Override
    public void tick() {
        boneAnimations.forEach(BoneAnimation::tick);
        if (weight != targetWeight) {
            weight += weightStep;
            if ((weightStep >= 0 && weight >= targetWeight) || (weightStep < 0 && weight <= targetWeight)) weight = targetWeight;
            boneAnimations.forEach(b -> b.setWeight(weight));
        }
        if (effectTick >= 0 && direction != AnimationHandler.AnimationDirection.PAUSE) {
            if (direction == AnimationHandler.AnimationDirection.FORWARD) {
                effectTick++;
                if (effectTick > animationTime && animationTime != 0) effectTick = looping ? 0 : animationTime;
            } else if (direction == AnimationHandler.AnimationDirection.BACKWARD) {
                effectTick--;
                if (effectTick < 0 && animationTime != 0) effectTick = looping ? animationTime : 0;
            }
        }
    }

    @Override
    public int currentTick() {
        for (BoneAnimation boneAnimation : boneAnimations) {
            if (boneAnimation.isPlaying()) return boneAnimation.getTick();
        }
        return effectTick; // -1 when stopped; the standalone clock for effects-only animations
    }

    public Set<String> getAnimatedBones() {
        return animatedBones;
    }
}

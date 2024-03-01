package net.worldseed.multipart.animations;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minestom.server.MinecraftServer;
import net.minestom.server.timer.ExecutionType;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import net.worldseed.multipart.GenericModel;
import net.worldseed.multipart.ModelLoader;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class AnimationHandlerImpl implements AnimationHandler {
    private final GenericModel model;
    private final Task task;

    Map<String, ModelAnimation> animations = new ConcurrentHashMap<>();
    TreeMap<Integer, ModelAnimation> repeating = new TreeMap<>();
    String playingOnce = null;

    Map<String, Consumer<Void>> callbacks = new ConcurrentHashMap<>();
    Map<String, Integer> callbackTimers = new ConcurrentHashMap<>();

    public AnimationHandlerImpl(GenericModel model) {
        this.model = model;
        loadDefaultAnimations();
        this.task = MinecraftServer.getSchedulerManager().scheduleTask(this::tick, TaskSchedule.immediate(), TaskSchedule.tick(1), ExecutionType.ASYNC);
    }

    protected void loadDefaultAnimations() {
        JsonObject loadedAnimations = ModelLoader.loadAnimations(model.getId());
        // Init animation
        int i = 0;
        for (Map.Entry<String, JsonElement> animation : loadedAnimations.get("animations").getAsJsonObject().entrySet()) {
            registerAnimation(animation.getKey(), animation.getValue(), i);
            i--;
        }
    }

    @Override
    public void registerAnimation(String name, JsonElement animation, int priority) {
        final JsonElement animationLength = animation.getAsJsonObject().get("animation_length");
        final double length = animationLength == null ? 0 : animationLength.getAsDouble();

        HashSet<BoneAnimation> animationSet = new HashSet<>();
        for (Map.Entry<String, JsonElement> boneEntry : animation.getAsJsonObject().get("bones").getAsJsonObject().entrySet()) {
            String boneName = boneEntry.getKey();
            var bone = model.getPart(boneName);
            if (bone == null) continue;

            JsonElement animationRotation = boneEntry.getValue().getAsJsonObject().get("rotation");
            JsonElement animationPosition = boneEntry.getValue().getAsJsonObject().get("position");

            if (animationRotation != null) {
                BoneAnimationImpl boneAnimation = new BoneAnimationImpl(model.getId(), name, boneName, bone, animationRotation, ModelLoader.AnimationType.ROTATION, length);
                animationSet.add(boneAnimation);
            }
            if (animationPosition != null) {
                BoneAnimationImpl boneAnimation = new BoneAnimationImpl(model.getId(), name, boneName, bone, animationPosition, ModelLoader.AnimationType.TRANSLATION, length);
                animationSet.add(boneAnimation);
            }
        }

        animations.put(name, new ModelAnimationClassic(name, (int) (length * 20), priority, animationSet));
    }

    @Override
    public void registerAnimation(ModelAnimation animator) {
        animations.put(animator.name(), animator);
    }

    public void playRepeat(String animation) throws IllegalArgumentException {
        playRepeat(animation, AnimationDirection.FORWARD);
    }

    @Override
    public void playRepeat(String animation, AnimationDirection direction) throws IllegalArgumentException {
        if (this.animationPriorities().get(animation) == null) throw new IllegalArgumentException("Animation " + animation + " does not exist");

        var modelAnimation = this.animations.get(animation);

        if (this.repeating.containsKey(this.animationPriorities().get(animation))
                && modelAnimation.direction() == direction) return;

        modelAnimation.setDirection(direction);

        this.repeating.put(this.animationPriorities().get(animation), modelAnimation);
        var top = this.repeating.firstEntry();

        if (top != null && animation.equals(top.getValue().name())) {
            this.repeating.values().forEach(v -> {
                if (!v.name().equals(animation)) {
                    this.animations.forEach((k, a) -> a.stop());
                }
            });
            if (playingOnce == null) {
                modelAnimation.play();
            }
        }
    }

    public void stopRepeat(String animation) throws IllegalArgumentException {
        if (this.animationPriorities().get(animation) == null) throw new IllegalArgumentException("Animation " + animation + " does not exist");

        var modelAnimation = this.animations.get(animation);

        modelAnimation.stop();
        int priority = this.animationPriorities().get(animation);

        Map.Entry<Integer, ModelAnimation> currentTop = this.repeating.firstEntry();

        this.repeating.remove(priority);

        Map.Entry<Integer, ModelAnimation> firstEntry = this.repeating.firstEntry();

        if (this.playingOnce == null && firstEntry != null && currentTop != null && !firstEntry.getKey().equals(currentTop.getKey())) {
            firstEntry.getValue().play();
        }
    }

    public void playOnce(String animation, Consumer<Void> cb) throws IllegalArgumentException {
        this.playOnce(animation, AnimationDirection.FORWARD, cb);
    }

    @Override
    public void playOnce(String animation, AnimationDirection direction, Consumer<Void> cb) throws IllegalArgumentException {
        if (this.animationPriorities().get(animation) == null) throw new IllegalArgumentException("Animation " + animation + " does not exist");

        var modelAnimation = this.animations.get(animation);

        AnimationDirection currentDirection = modelAnimation.direction();
        modelAnimation.setDirection(direction);

        if (this.callbacks.containsKey(animation)) {
            this.callbacks.get(animation).accept(null);
        }

        int callbackTimer = this.callbackTimers.getOrDefault(animation, 0);

        if (animation.equals(this.playingOnce) && direction == AnimationDirection.PAUSE && callbackTimer > 0) {
            // Pause. Only call if we're not stopped
            playingOnce = animation;
            this.callbacks.put(animation, cb);
        } else if (animation.equals(this.playingOnce) && currentDirection != direction) {
            playingOnce = animation;
            this.callbacks.put(animation, cb);
            if (currentDirection != AnimationDirection.PAUSE)
                this.callbackTimers.put(animation, modelAnimation.animationTime() - callbackTimer + 1);
        } else if (direction != AnimationDirection.PAUSE) {
            if (playingOnce != null) modelAnimation.stop();
            playingOnce = animation;

            this.callbacks.put(animation, cb);
            this.callbackTimers.put(animation, modelAnimation.animationTime());
            modelAnimation.play();

            this.repeating.values().forEach(v -> {
                if (!v.name().equals(animation)) {
                    v.stop();
                }
            });
        }
    }

    private void tick() {
        try {
            for (Map.Entry<String, Integer> entry : callbackTimers.entrySet()) {
                var modelAnimation = animations.get(entry.getKey());

                if (entry.getValue() <= 0) {
                    if (this.playingOnce != null && this.playingOnce.equals(entry.getKey())) {
                        Map.Entry<Integer, ModelAnimation> firstEntry = this.repeating.firstEntry();
                        if (firstEntry != null) {
                            firstEntry.getValue().play();
                        }

                        this.playingOnce = null;
                    }

                    this.model.triggerAnimationEnd(entry.getKey(), modelAnimation.direction());

                    modelAnimation.stop();
                    callbackTimers.remove(entry.getKey());

                    var cb = callbacks.remove(entry.getKey());
                    if (cb != null) cb.accept(null);
                } else {
                    if (modelAnimation.direction() != AnimationDirection.PAUSE) {
                        callbackTimers.put(entry.getKey(), entry.getValue() - 1);
                    }
                }
            }

            if (callbacks.size() + repeating.size() == 0) return;
            this.model.draw();

            this.animations.forEach((animation, animations) -> {
                animations.tick();
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void destroy() {
        this.task.cancel();
    }

    public String getPlaying() {
        if (this.playingOnce != null) return this.playingOnce;
        var playing = this.repeating.firstEntry();
        return playing != null ? playing.getValue().name() : null;
    }

    @Override
    public Map<String, Integer> animationPriorities() {
        return new HashMap<>() {{
            for (Map.Entry<String, ModelAnimation> entry : animations.entrySet()) {
                put(entry.getKey(), entry.getValue().priority());
            }
        }};
    }
}
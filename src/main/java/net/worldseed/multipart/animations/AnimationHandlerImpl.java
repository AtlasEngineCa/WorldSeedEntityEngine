package net.worldseed.multipart.animations;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minestom.server.MinecraftServer;
import net.minestom.server.timer.ExecutionType;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import net.worldseed.multipart.GenericModel;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public abstract class AnimationHandlerImpl implements AnimationHandler {
    private final Map<String, Set<ModelAnimation>> animations;
    public final Map<String, Integer> animationTimes = new HashMap<>();

    private final GenericModel model;
    private final Task task;

    TreeMap<Integer, String> repeating = new TreeMap<>();
    String playingOnce = null;

    Map<String, Consumer<Void>> callbacks = new ConcurrentHashMap<>();
    Map<String, Integer> callbackTimers = new ConcurrentHashMap<>();

    public AnimationHandlerImpl(GenericModel model) {
        this.model = model;
        JsonObject loadedAnimations = AnimationLoader.loadAnimations(model.getId());
        // Init animation
        {
            Map<String, HashSet<ModelAnimation>> animations = new HashMap<>();
            Map<String, Double> animationTimes = new HashMap<>();
            for (Map.Entry<String, JsonElement> animation : loadedAnimations.get("animations").getAsJsonObject().entrySet()) {
                final String animationName = animation.getKey();
                final JsonElement animationLength = animation.getValue().getAsJsonObject().get("animation_length");
                final double length = animationLength == null ? 0 : animationLength.getAsDouble();

                HashSet<ModelAnimation> animationSet = new HashSet<>();
                for (Map.Entry<String, JsonElement> boneEntry : animation.getValue().getAsJsonObject().get("bones").getAsJsonObject().entrySet()) {
                    String boneName = boneEntry.getKey();
                    JsonElement animationRotation = boneEntry.getValue().getAsJsonObject().get("rotation");
                    JsonElement animationPosition = boneEntry.getValue().getAsJsonObject().get("position");

                    if (animationRotation != null) {
                        ModelAnimation boneAnimation = new ModelAnimation(model.getId(), animationName, boneName, model.getPart(boneName), animationRotation, AnimationLoader.AnimationType.ROTATION, length);
                        animationSet.add(boneAnimation);
                    }
                    if (animationPosition != null) {
                        ModelAnimation boneAnimation = new ModelAnimation(model.getId(), animationName, boneName, model.getPart(boneName), animationPosition, AnimationLoader.AnimationType.TRANSLATION, length);
                        animationSet.add(boneAnimation);
                    }
                }
                animationTimes.put(animationName, length);
                animations.put(animationName, animationSet);
            }
            this.animations = Map.copyOf(animations);
            // this.animationTimes = Map.copyOf(animationTimes);

            animationTimes.forEach((name, time) -> {
                this.animationTimes.put(name, (int) (time * 20));
            });
        }

        this.task = MinecraftServer.getSchedulerManager().scheduleTask(this::tick, TaskSchedule.immediate(), TaskSchedule.tick(1), ExecutionType.ASYNC);
    }

    public void playRepeat(String animation) {
        this.repeating.put(this.animationPriorities().get(animation), animation);
        var top = this.repeating.firstEntry();

        if (playingOnce != null) {
            this.callbacks.get(playingOnce).accept(null);
            this.animations.get(playingOnce).forEach(ModelAnimation::stop);
            this.callbacks.remove(playingOnce);
            this.callbackTimers.remove(playingOnce);
        }

        if (top != null && animation.equals(top.getValue())) {
            this.repeating.values().forEach(v -> {
                if (!v.equals(animation)) {
                    this.animations.get(v).forEach(ModelAnimation::stop);
                }
            });
            this.animations.get(animation).forEach(ModelAnimation::play);
        }
    }

    public void stopRepeat(String animation) {
        this.animations.get(animation).forEach(ModelAnimation::stop);
        int priority = this.animationPriorities().get(animation);

        Map.Entry<Integer, String> currentTop = this.repeating.firstEntry();

        this.animations.get(animation).forEach(ModelAnimation::stop);
        this.repeating.remove(priority);

        Map.Entry<Integer, String> firstEntry = this.repeating.firstEntry();
        if (firstEntry != null && currentTop != null && !firstEntry.getKey().equals(currentTop.getKey())) {
            this.animations.get(firstEntry.getValue()).forEach(ModelAnimation::play);
        }
    }

    public void playOnce(String animation, Consumer<Void> cb) {
        if (this.callbacks.containsKey(animation)) {
            this.callbacks.get(animation).accept(null);
        }

        if (playingOnce != null) {
            this.animations.get(playingOnce).forEach(ModelAnimation::stop);
        }

        playingOnce = animation;

        this.callbacks.put(animation, cb);
        this.callbackTimers.put(animation, animationTimes.get(animation));
        this.animations.get(animation).forEach(ModelAnimation::play);

        this.repeating.values().forEach(v -> {
            if (!v.equals(animation)) {
                this.animations.get(v).forEach(ModelAnimation::stop);
            }
        });
    }

    public void playOnceConcurrent(String animation, Consumer<Void> cb) {
        if (this.callbacks.containsKey(animation)) {
            this.callbacks.get(animation).accept(null);
        }

        this.callbacks.put(animation, cb);
        this.callbackTimers.put(animation, animationTimes.get(animation));
        this.animations.get(animation).forEach(ModelAnimation::play);
    }

    private void tick() {
        try {
            for (Map.Entry<String, Integer> entry : callbackTimers.entrySet()) {
                if (entry.getValue() <= 0) {
                    if (this.playingOnce != null && this.playingOnce.equals(entry.getKey())) {
                        Map.Entry<Integer, String> firstEntry = this.repeating.firstEntry();
                        if (firstEntry != null) {
                            this.animations.get(firstEntry.getValue()).forEach(ModelAnimation::play);
                        }

                        this.playingOnce = null;
                    }

                    animations.get(entry.getKey()).forEach(ModelAnimation::stop);
                    callbackTimers.remove(entry.getKey());
                    callbacks.get(entry.getKey()).accept(null);
                    callbacks.remove(entry.getKey());
                } else {
                    callbackTimers.put(entry.getKey(), entry.getValue() - 1);
                }
            }

            if (callbacks.size() + repeating.size() == 0) return;
            this.model.drawBones();

            this.animations.forEach((animation, animations) -> {
                animations.forEach(ModelAnimation::tick);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void destroy() {
        this.task.cancel();
    }

    public String getPlaying() {
        var playing = this.repeating.firstEntry();
        return playing != null ? playing.getValue() : null;
    }
}

package net.worldseed.multipart.animations;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minestom.server.MinecraftServer;
import net.minestom.server.timer.ExecutionType;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import net.worldseed.multipart.GenericModel;

import java.util.*;
import java.util.function.Consumer;

public abstract class AnimationHandlerImpl implements AnimationHandler {
    private static final short REFRESH_RATE_TICKS = 1;
    public abstract Map<String, Integer> animationPriorities();

    private final Map<String, Set<ModelAnimation>> animations;
    public final Map<String, Double> animationTimes;
    public final JsonObject loadedAnimations;

    private final GenericModel model;
    private final TreeMap<Integer, String> toPlay = new TreeMap<>();

    Map.Entry<Integer, String> playing;

    private Task drawBonesTask;
    private boolean updates = false;

    // When true, force the entire animation to play out. No animations can interrupt it.
    private short tick;
    private short animationLength;
    private final Map<String, Consumer<Void>> removeAfterPlaying = new HashMap<>();

    public AnimationHandlerImpl(GenericModel model) {
        this.model = model;
        this.loadedAnimations = AnimationLoader.loadAnimations(model.getId());
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
                        ModelAnimation boneAnimation = new ModelAnimation(model.getId(), animationName, model.getPart(boneName), animationRotation, AnimationLoader.AnimationType.ROTATION, length);
                        animationSet.add(boneAnimation);
                    }
                    if (animationPosition != null) {
                        ModelAnimation boneAnimation = new ModelAnimation(model.getId(), animationName, model.getPart(boneName), animationPosition, AnimationLoader.AnimationType.TRANSLATION, length);
                        animationSet.add(boneAnimation);
                    }
                }
                animationTimes.put(animationName, length);
                animations.put(animationName, animationSet);
            }
            this.animations = Map.copyOf(animations);
            this.animationTimes = Map.copyOf(animationTimes);
        }
    }

    private short getTick() {
        return (short) (animationLength - tick);
    }

    public void playRepeat(String animation) {
        this.toPlay.put(animationPriorities().get(animation), animation);
        setUpdates(true);
        playNext();
    }

    public void stopRepeat(String animation) {
        this.toPlay.remove(animationPriorities().get(animation));
        playNext();
    }

    @Override
    public void playOnce(String animation, Consumer<Void> cb) {
        this.toPlay.put(animationPriorities().get(animation), animation);
        setUpdates(true);
        this.removeAfterPlaying.put(animation, cb);
        playNext();
    }

    @Override
    public void destroy() {
        if (drawBonesTask != null) {
            this.drawBonesTask.cancel();
            this.drawBonesTask = null;
        }
    }

    private void playNext() {
        final Map.Entry<Integer, String> oldPlaying = this.playing;
        final Map.Entry<Integer, String> playing = toPlay.firstEntry();
        if (playing == null) return;
        if (oldPlaying != null && oldPlaying.getValue().equals(playing.getValue())) return;
        playNextSchedule();
    }

    private void playNextSchedule() {
        if (!this.updates) return;

        final Map.Entry<Integer, String> oldPlaying = this.playing;
        final Map.Entry<Integer, String> playing = toPlay.firstEntry();

        this.playing = playing;
        if (this.playing == null) return;

        this.tick = (short) (animationTimes.get(playing.getValue()) * 1000 / 50);
        this.animationLength = (short) (animationTimes.get(playing.getValue()) * 1000 / 50);

        if (oldPlaying != null && !oldPlaying.getValue().equals(playing.getValue())) {
            this.animations.get(oldPlaying.getValue()).forEach(ModelAnimation::cancel);
        }
        this.animations.get(playing.getValue()).forEach(ModelAnimation::play);
    }

    public String getPlaying() {
        var playing = this.playing;
        return playing != null ? playing.getValue() : null;
    }

    public void setUpdates(boolean updates) {
        if (this.updates && this.drawBonesTask != null && !updates) {
            this.drawBonesTask.cancel();
            this.drawBonesTask = null;
            this.updates = false;

            for (Map.Entry<String, Consumer<Void>> toRemove : removeAfterPlaying.entrySet()) {
                toRemove.getValue().accept(null);
                this.toPlay.remove(animationPriorities().get(toRemove.getKey()));
            }
            this.removeAfterPlaying.clear();
        } else if (updates && !this.updates && this.drawBonesTask == null) {
            this.updates = true;

            this.drawBonesTask = MinecraftServer.getSchedulerManager()
                    .submitTask(() -> {
                        if (tick < 0) {
                            if (playing != null) {
                                Integer playingKey = playing.getKey();
                                String playingValue = playing.getValue();

                                if (removeAfterPlaying.containsKey(playingValue)) {
                                    toPlay.remove(playingKey);
                                    Consumer<Void> found = removeAfterPlaying.get(playingValue);
                                    removeAfterPlaying.remove(playingValue);
                                    found.accept(null);
                                }
                            }
                            playNextSchedule();
                        }
                        if (this.updates) {
                            model.drawBones(getTick());
                            tick--;
                            return TaskSchedule.tick(REFRESH_RATE_TICKS);
                        }
                        return TaskSchedule.stop();
                    }, ExecutionType.ASYNC);
        }
    }
}

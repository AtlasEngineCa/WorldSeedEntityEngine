package net.worldseed.multipart.animations;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minestom.server.MinecraftServer;
import net.minestom.server.timer.ExecutionType;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import net.worldseed.multipart.GenericModel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

public abstract class AnimationHandlerImpl implements AnimationHandler {
    private static final short refreshRateTicks = 1;

    public final Map<String, Double> animationTimes = new HashMap<>();
    public final JsonObject loadedAnimations;

    private final GenericModel model;
    private final TreeMap<Integer, String> toPlay = new TreeMap<>();

    private final HashMap<String, HashSet<ModelAnimation>> animations = new HashMap<>();

    private final float timeScalar = 1f;

    protected Map<String, Integer> animationPriorities = new HashMap<>();

    Map.Entry<Integer, String> playing;

    private Task drawBonesTask;
    private boolean updates = false;

    // When true, force the entire animation to play out. No animations can interrupt it.
    private short tick;
    private short animationLength;
    private HashMap<String, Consumer<Void>> removeAfterPlaying = new HashMap<>();

    public AnimationHandlerImpl(GenericModel model) {
        this.model = model;
        this.loadedAnimations = AnimationLoader.loadAnimations(model.getId());
    }

    protected void initAnimations() {
        for (Map.Entry<String, JsonElement> animation : loadedAnimations.get("animations").getAsJsonObject().entrySet()) {
            String animationName = animation.getKey();

            var animationLength = animation.getValue().getAsJsonObject().get("animation_length");
            double length = animationLength == null ? 0 : animationLength.getAsDouble();

            HashSet<ModelAnimation> animationSet = new HashSet<>();

            for (Map.Entry<String, JsonElement> boneEntry : animation.getValue().getAsJsonObject().get("bones").getAsJsonObject().entrySet()) {
                String boneName = boneEntry.getKey();

                JsonElement animationRotation = boneEntry.getValue().getAsJsonObject().get("rotation");
                JsonElement animationPosition = boneEntry.getValue().getAsJsonObject().get("position");

                if (animationRotation != null) {
                    ModelAnimation boneAnimation = new ModelAnimation(model.getId(), animationName, model.getPart(boneName), animationRotation, AnimationLoader.AnimationType.ROTATION, length, timeScalar);
                    animationSet.add(boneAnimation);
                }

                if (animationPosition != null) {
                    ModelAnimation boneAnimation = new ModelAnimation(model.getId(), animationName, model.getPart(boneName), animationPosition, AnimationLoader.AnimationType.TRANSLATION, length, timeScalar);
                    animationSet.add(boneAnimation);
                }
            }

            animationTimes.put(animationName, length * timeScalar);
            animations.put(animationName, animationSet);
        }
    }

    private short getTick() {
        return (short) (animationLength - tick);
    }

    public void playRepeat(String animation) {
        toPlay.put(animationPriorities.get(animation), animation);
        setUpdates(true);
        playNext();
    }

    public void stopRepeat(String animation) {
        toPlay.remove(animationPriorities.get(animation));
        playNext();
    }

    @Override
    public void playOnce(String animation, Consumer<Void> cb) {
        toPlay.put(animationPriorities.get(animation), animation);
        setUpdates(true);
        playNext();

        removeAfterPlaying.put(animation, cb);
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
        
        tick = (short) (animationTimes.get(playing.getValue()).shortValue() * 1000 / 50);
        animationLength = (short) (animationTimes.get(playing.getValue()).shortValue() * 1000 / 50);

        if (oldPlaying != null && !oldPlaying.getValue().equals(playing.getValue())) {
            animations.get(oldPlaying.getValue()).forEach(ModelAnimation::cancel);
        }

        animations.get(playing.getValue()).forEach(ModelAnimation::play);
    }

    public String getPlaying() {
        if (this.playing == null) return null;
        return this.playing.getValue();
    }

    public void setUpdates(boolean updates) {
        if (this.updates && this.drawBonesTask != null && !updates) {
            this.drawBonesTask.cancel();
            this.drawBonesTask = null;
            this.updates = false;
        } else if (updates && !this.updates && this.drawBonesTask == null) {
            this.updates = true;

            this.drawBonesTask = MinecraftServer.getSchedulerManager()
                .submitTask(() -> {
                    if (tick < 0) {
                        if (playing != null && removeAfterPlaying.containsKey(playing.getValue())) {
                            toPlay.remove(playing.getKey());
                            removeAfterPlaying.get(playing.getValue()).accept(null);
                            removeAfterPlaying.remove(playing.getValue());
                        }

                        playNextSchedule();
                    }

                    if (this.updates) {
                        model.drawBones(getTick());
                        tick--;

                        return TaskSchedule.tick(refreshRateTicks);
                    }
                    
                    return TaskSchedule.stop();

                }, ExecutionType.ASYNC);
        }
    }
}

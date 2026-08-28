package net.worldseed.multipart.animations;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.timer.ExecutionType;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import net.worldseed.multipart.GenericModel;
import net.worldseed.multipart.ModelLoader;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AnimationHandlerImpl implements AnimationHandler {
    private final GenericModel model;
    private final Task task;

    private final Map<String, ModelAnimation> animations = new ConcurrentHashMap<>();
    private final TreeMap<Integer, ModelAnimation> repeating = new TreeMap<>();
    /** The loop requested most recently by the caller; map priority must never pick locomotion state. */
    private ModelAnimation activeRepeating;
    private String playingOnce = null;
    private int playingOnceBlendTicks = 0;

    private final Map<String, Runnable> callbacks = new ConcurrentHashMap<>();
    private final Map<String, Integer> callbackTimers = new ConcurrentHashMap<>();

    private final Map<String, List<AnimationEffect>> effectsByAnimation = new ConcurrentHashMap<>();
    private final Map<String, Integer> lastEffectTick = new ConcurrentHashMap<>();
    private volatile AnimationEffectHandler effectHandler = AnimationHandler.DEFAULT_EFFECT_HANDLER;

    public AnimationHandlerImpl(GenericModel model) {
        this(model, true);
    }

    AnimationHandlerImpl(GenericModel model, boolean scheduleTask) {
        this.model = model;
        if (model.getParts().isEmpty()) {
            throw new IllegalStateException(
                    "The model must be initialized before creating its AnimationHandler; " +
                            "otherwise animation channels cannot bind to bones.");
        }
        loadDefaultAnimations();
        this.task = scheduleTask
                ? MinecraftServer.getSchedulerManager().scheduleTask(this::tick, TaskSchedule.immediate(), TaskSchedule.tick(1), ExecutionType.TICK_START)
                : null;
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
        final JsonElement loopElement = animation.getAsJsonObject().get("loop");
        final boolean looping = loopElement != null && loopElement.getAsBoolean();

        HashSet<BoneAnimation> animationSet = new HashSet<>();
        HashSet<String> animatedBones = new HashSet<>();

        for (Map.Entry<String, JsonElement> boneEntry : animation.getAsJsonObject().get("bones").getAsJsonObject().entrySet()) {
            String boneName = boneEntry.getKey();
            var bone = model.getPart(boneName);
            if (bone == null) continue;

            JsonElement animationRotation = boneEntry.getValue().getAsJsonObject().get("rotation");
            JsonElement animationPosition = boneEntry.getValue().getAsJsonObject().get("position");
            JsonElement animationScale = boneEntry.getValue().getAsJsonObject().get("scale");

            boolean animated = false;

            if (hasKeyframes(animationRotation)) {
                animated = true;
                BoneAnimationImpl boneAnimation = new BoneAnimationImpl(model.getId(), name, boneName, bone, animationRotation, ModelLoader.AnimationType.ROTATION, length, looping);
                animationSet.add(boneAnimation);
            }
            if (hasKeyframes(animationPosition)) {
                animated = true;
                BoneAnimationImpl boneAnimation = new BoneAnimationImpl(model.getId(), name, boneName, bone, animationPosition, ModelLoader.AnimationType.TRANSLATION, length, looping);
                animationSet.add(boneAnimation);
            }
            if (hasKeyframes(animationScale)) {
                animated = true;
                BoneAnimationImpl boneAnimation = new BoneAnimationImpl(model.getId(), name, boneName, bone, animationScale, ModelLoader.AnimationType.SCALE, length, looping);
                animationSet.add(boneAnimation);
            }

            if (animated) {
                animatedBones.add(boneName);
            }
        }

        animations.put(name, new ModelAnimationClassic(name, (int) (length * 20), priority, animationSet, animatedBones, looping));

        // parse Blockbench sound/particle/timeline effects for this animation
        List<AnimationEffect> effects = new ArrayList<>();
        JsonElement effectsJson = animation.getAsJsonObject().get("effects");
        if (effectsJson != null && effectsJson.isJsonArray()) {
            for (JsonElement el : effectsJson.getAsJsonArray()) {
                JsonObject o = el.getAsJsonObject();
                String channel = o.has("channel") ? o.get("channel").getAsString() : "";
                AnimationEffect.Type type = switch (channel) {
                    case "sound" -> AnimationEffect.Type.SOUND;
                    case "particle" -> AnimationEffect.Type.PARTICLE;
                    default -> AnimationEffect.Type.TIMELINE;
                };
                int tick = (int) Math.round(o.get("time").getAsDouble() * 20);
                effects.add(new AnimationEffect(name, type, tick,
                        o.has("effect") ? o.get("effect").getAsString() : null,
                        o.has("locator") ? o.get("locator").getAsString() : null,
                        o.has("script") ? o.get("script").getAsString() : null));
            }
            effects.sort(Comparator.comparingInt(AnimationEffect::tick));
        }
        effectsByAnimation.put(name, effects);
    }

    private static boolean hasKeyframes(JsonElement channel) {
        return channel != null && channel.isJsonObject() && !channel.getAsJsonObject().isEmpty();
    }

    @Override
    public void setEffectHandler(AnimationEffectHandler handler) {
        this.effectHandler = handler;
    }

    @Override
    public void registerAnimation(ModelAnimation animator) {
        animations.put(animator.name(), animator);
    }

    public void playRepeat(String animation) throws IllegalArgumentException {
        playRepeat(animation, AnimationDirection.FORWARD);
    }

    @Override
    public void playRepeat(String animation, int blendTicks) throws IllegalArgumentException {
        playRepeat(animation, AnimationDirection.FORWARD, blendTicks);
    }

    @Override
    public void playRepeat(String animation, AnimationDirection direction, int blendTicks) throws IllegalArgumentException {
        if (blendTicks <= 0) {
            playRepeat(animation, direction);
            return;
        }
        Integer priority = this.animationPriorities().get(animation);
        if (priority == null) throw new IllegalArgumentException("Animation " + animation + " does not exist");
        ModelAnimation modelAnimation = this.animations.get(animation);
        ModelAnimation existing = this.activeRepeating;
        if (existing == modelAnimation && modelAnimation.weight() >= 0.999) return;

        // fade out whatever is currently repeating (except the target)
        for (ModelAnimation other : this.repeating.values()) {
            if (!other.name().equals(animation)) other.blendTo(0, blendTicks);
        }

        modelAnimation.setDirection(direction);
        this.repeating.put(priority, modelAnimation);
        this.activeRepeating = modelAnimation;
        if (playingOnce == null) {
            modelAnimation.setWeight(0);
            modelAnimation.play(false);
            modelAnimation.blendTo(1, blendTicks); // fade in
        }
    }

    @Override
    public void playRepeat(String animation, AnimationDirection direction) throws IllegalArgumentException {
        if (this.animationPriorities().get(animation) == null)
            throw new IllegalArgumentException("Animation " + animation + " does not exist");
        var modelAnimation = this.animations.get(animation);

        if (this.activeRepeating == modelAnimation && modelAnimation.direction() == direction) return;

        modelAnimation.setDirection(direction);

        this.repeating.put(this.animationPriorities().get(animation), modelAnimation);
        this.activeRepeating = modelAnimation;
        this.repeating.values().forEach(v -> {
            if (v != modelAnimation) v.stop();
        });
        if (playingOnce == null) {
            modelAnimation.play(false);
        }
    }

    public void stopRepeat(String animation) throws IllegalArgumentException {
        if (this.animationPriorities().get(animation) == null)
            throw new IllegalArgumentException("Animation " + animation + " does not exist");

        var modelAnimation = this.animations.get(animation);

        modelAnimation.stop(); //Stop the highest priority repeating animation
        int priority = this.animationPriorities().get(animation);
        this.repeating.remove(priority);
        if (activeRepeating == modelAnimation) {
            Map.Entry<Integer, ModelAnimation> fallback = this.repeating.firstEntry();
            activeRepeating = fallback == null ? null : fallback.getValue();
            if (activeRepeating != null && playingOnce == null) {
                activeRepeating.setDirection(AnimationDirection.FORWARD);
                activeRepeating.play(true);
            }
        }
    }


    public void playOnce(String animation, Runnable cb) throws IllegalArgumentException {
        this.playOnce(animation, true, cb);
    }

    @Override
    public void playOnce(String animation, int blendTicks, Runnable cb) throws IllegalArgumentException {
        if (blendTicks <= 0) {
            playOnce(animation, cb);
            return;
        }
        playOnceInternal(animation, AnimationDirection.FORWARD, true, blendTicks, cb);
    }

    public void playOnce(String animation, boolean override, Runnable cb) throws IllegalArgumentException {
        this.playOnce(animation, AnimationDirection.FORWARD, override, cb);
    }

    @Override
    public void playOnce(String animation, AnimationDirection direction, boolean override, Runnable cb) throws IllegalArgumentException {
        playOnceInternal(animation, direction, override, 0, cb);
    }

    private void playOnceInternal(String animation, AnimationDirection direction, boolean override,
                                  int blendTicks, Runnable cb) throws IllegalArgumentException {
        if (this.animationPriorities().get(animation) == null)
            throw new IllegalArgumentException("Animation " + animation + " does not exist");

        var modelAnimation = this.animations.get(animation);

        AnimationDirection currentDirection = modelAnimation.direction();
        modelAnimation.setDirection(direction);

        if (this.callbacks.containsKey(animation)) { //This animation had a pending runnable
            this.callbacks.get(animation).run(); //Run callback runnable
        }

        int callbackTimer = this.callbackTimers.getOrDefault(animation, 0);

        if (animation.equals(this.playingOnce) && direction == AnimationDirection.PAUSE && callbackTimer > 0) { //This animation was already playing, paused and not finished
            // Pause. Only call if we're not stopped
            playingOnce = animation;
            this.callbacks.put(animation, cb);
        } else if (animation.equals(this.playingOnce) && currentDirection != direction) { //This animation was already playing, but in a different direction
            playingOnce = animation;
            this.callbacks.put(animation, cb);
            if (currentDirection != AnimationDirection.PAUSE)
                this.callbackTimers.put(animation, modelAnimation.animationTime() - callbackTimer + 1);
        } else if (direction != AnimationDirection.PAUSE) { //This animation was not playing, or it was in the same direction
            if (playingOnce != null) { //Stop current animation
                this.animations.get(playingOnce).stop();
                modelAnimation.stop();
            }
            playingOnce = animation;
            playingOnceBlendTicks = Math.min(blendTicks, Math.max(0, modelAnimation.animationTime() / 3));

            this.callbacks.put(animation, cb);
            this.callbackTimers.put(animation, modelAnimation.animationTime());
            if (playingOnceBlendTicks > 0) {
                modelAnimation.setWeight(0);
                modelAnimation.play(false);
                modelAnimation.blendTo(1, playingOnceBlendTicks);
            } else {
                modelAnimation.setWeight(1);
                modelAnimation.play(false);
            }

            Set<String> animatedBones = modelAnimation.getAnimatedBones();
            this.repeating.values().forEach(v -> {
                if (!v.name().equals(animation)) {
                    if (playingOnceBlendTicks > 0 && override) {
                        v.blendTo(0, playingOnceBlendTicks);
                    } else if (override) {
                        v.stop(); //Stop all repeating animations
                    } else {
                        v.stop(animatedBones); //Stop all 'animatedBones' for all repeating animations
                    }
                }
            });
        }
    }

    private void tick() {
        try {
            for (Map.Entry<String, Integer> entry : callbackTimers.entrySet()) {
                var modelAnimation = animations.get(entry.getKey()); //Get playOnce animation from string

                if (entry.getValue() <= 0) { //All ticks were removed so playOnce should end
                    if (this.playingOnce != null && this.playingOnce.equals(entry.getKey())) {
                        ModelAnimation repeat = this.activeRepeating;
                        if (repeat != null) {
                            if (playingOnceBlendTicks <= 0) {
                                repeat.play(true);
                            }
                        }
                        this.playingOnce = null;
                    }

                    this.model.triggerAnimationEnd(entry.getKey(), modelAnimation.direction()); //Call AnimationCompleteEvent

                    modelAnimation.stop();
                    callbackTimers.remove(entry.getKey()); //Remove playOnce animation from map

                    var cb = callbacks.remove(entry.getKey());
                    if (cb != null) cb.run(); //Run 'callback' runnable
                } else {
                    if (modelAnimation.direction() != AnimationDirection.PAUSE) {
                        if (playingOnceBlendTicks > 0 && entry.getValue() == playingOnceBlendTicks) {
                            modelAnimation.blendTo(0, playingOnceBlendTicks);
                            ModelAnimation repeat = this.activeRepeating;
                            if (repeat != null) {
                                repeat.setWeight(0);
                                repeat.play(true);
                                repeat.blendTo(1, playingOnceBlendTicks);
                            }
                        }
                        callbackTimers.put(entry.getKey(), entry.getValue() - 1); //Countdown 1 tick until it reaches 0 during playOnce animation
                    }
                }
            }

            if (callbacks.size() + repeating.size() == 0) return; //Return if no playOnce or repeating animation is playing
            this.model.draw(); 

            this.animations.forEach((_, animations) -> {
                animations.tick(); //Play every tick (besides the first one) of the animation
            });

            fireEffects();

            // drop animations that have finished blending out
            this.repeating.entrySet().removeIf(entry -> {
                if (playingOnce != null && playingOnceBlendTicks > 0) return false;
                if (entry.getValue().fadedOut()) {
                    entry.getValue().stop();
                    return true;
                }
                return false;
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Fire any sound/particle/timeline effects the currently-playing animations crossed this tick. */
    private void fireEffects() {
        AnimationEffectHandler handler = this.effectHandler;
        if (handler == null) return;

        Set<ModelAnimation> playing = new HashSet<>(repeating.values());
        if (playingOnce != null) {
            ModelAnimation once = animations.get(playingOnce);
            if (once != null) playing.add(once);
        }

        for (ModelAnimation anim : playing) {
            List<AnimationEffect> effects = effectsByAnimation.get(anim.name());
            if (effects == null || effects.isEmpty()) continue;

            int cur = anim.currentTick();
            if (cur < 0) { // not actually playing this tick
                lastEffectTick.remove(anim.name());
                continue;
            }
            int last = lastEffectTick.getOrDefault(anim.name(), -1);
            if (cur != last) {
                for (AnimationEffect effect : effects) {
                    int t = effect.tick();
                    // fire effects whose tick falls in (last, cur], wrapping when the animation looped
                    boolean crossed = (last < cur) ? (t > last && t <= cur) : (t > last || t <= cur);
                    if (crossed) {
                        try {
                            handler.onEffect(model, effect);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                lastEffectTick.put(anim.name(), cur);
            }
        }
    }

    /** Built-in effect handler (see {@link AnimationHandler#DEFAULT_EFFECT_HANDLER}). */
    public static void playDefaultEffect(GenericModel model, AnimationEffect effect) {
        switch (effect.type()) {
            case SOUND -> {
                if (effect.effect() == null || effect.effect().isBlank()) return;
                Key key;
                try {
                    key = Key.key(effect.effect());
                } catch (Exception invalidKey) {
                    return; // effect id isn't a Minecraft sound key — a custom handler should map it
                }
                Point pos = effectPosition(model, effect);
                Sound sound = Sound.sound(key, Sound.Source.NEUTRAL, 1f, 1f);
                model.getViewers().forEach(viewer -> viewer.playSound(sound, pos.x(), pos.y(), pos.z()));
            }
            case PARTICLE -> {
                if (effect.effect() == null || effect.effect().isBlank()) return;
                Particle particle;
                try {
                    particle = Particle.fromKey(effect.effect());
                } catch (Exception invalidKey) {
                    return;
                }
                if (particle == null) return; // not a vanilla particle — a custom handler should map it
                Point pos = effectPosition(model, effect);
                ParticlePacket packet = new ParticlePacket(particle, pos, Vec.ZERO, 0f, 1);
                model.getViewers().forEach(viewer -> viewer.sendPacket(packet));
            }
            case TIMELINE -> { /* script instruction — no built-in behaviour; set a custom handler */ }
        }
    }

    private static Point effectPosition(GenericModel model, AnimationEffect effect) {
        if (effect.locator() != null && !effect.locator().isBlank()) {
            try {
                Point p = model.getVFX(effect.locator());
                if (p != null) return p;
            } catch (Exception ignored) {
                // no such locator/VFX bone — fall back to the model origin
            }
        }
        return model.getPosition();
    }

    public void destroy() {
        if (this.task != null) this.task.cancel();
    }

    @Override
    public @Nullable String getPlaying() {
        if (this.playingOnce != null) return this.playingOnce;
        return getRepeating();
    }

    @Override
    public @Nullable String getRepeating() {
        var playing = this.repeating.firstEntry();
        return playing != null ? playing.getValue().name() : null;
    }

    @Override
    public @Nullable ModelAnimation getAnimation(String animation) {
        return this.animations.get(animation);
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

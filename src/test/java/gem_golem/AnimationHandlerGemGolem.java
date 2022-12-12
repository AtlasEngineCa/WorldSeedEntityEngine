package gem_golem;

import net.worldseed.multipart.GenericModel;
import net.worldseed.multipart.animations.AnimationHandlerImpl;

import java.util.Map;

import static java.util.Map.entry;

public class AnimationHandlerGemGolem extends AnimationHandlerImpl {
    private static final Map<String, Integer> ANIMATION_PRIORITIES = Map.ofEntries(
            entry("idle_extended", 5),
            entry("idle_retracted", 4),
            entry("walk", 3),
            entry("attack", 2),
            entry("attack_near", 1),
            entry("death", 0),
            entry("retract", -1),
            entry("extend", -2));

    public AnimationHandlerGemGolem(GenericModel model) {
        super(model);
    }

    @Override
    public Map<String, Integer> animationPriorities() {
        return ANIMATION_PRIORITIES;
    }
}

package tuff_golem;

import net.worldseed.multipart.GenericModel;
import net.worldseed.multipart.animations.AnimationHandlerImpl;

import java.util.Map;

import static java.util.Map.entry;

public class TuffGolemAnimationHandler extends AnimationHandlerImpl {
    private static final Map<String, Integer> ANIMATION_PRIORITIES = Map.ofEntries(
            entry("overhead_swipe", 1),
            entry("death", 2),
            entry("wither_pool", 3),
            entry("idle", 4),
            entry("cont_smash", 5),
            entry("roundhouse", 6)
    );

    public TuffGolemAnimationHandler(GenericModel model) {
        super(model);
    }

    @Override
    public Map<String, Integer> animationPriorities() {
        return ANIMATION_PRIORITIES;
    }
}

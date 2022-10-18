package TuffGolem;

import net.worldseed.multipart.GenericModel;
import net.worldseed.multipart.animations.AnimationHandlerImpl;

import java.util.Map;

import static java.util.Map.entry;

public class TuffGolemAnimationHandler extends AnimationHandlerImpl {
    private static final Map<String, Integer> ANIMATION_PRIORITIES = Map.ofEntries(
            entry("walk", 0),
            entry("walk_holding", 1),
            entry("inactive", 2),
            entry("inactive_holding", 3),
            entry("get_up", 4),
            entry("get_up_holding", 5),
            entry("grab", 6),
            entry("down_holding", 7),
            entry("down", 8)
    );

    public TuffGolemAnimationHandler(GenericModel model) {
        super(model);
    }

    @Override
    public Map<String, Integer> animationPriorities() {
        return ANIMATION_PRIORITIES;
    }
}

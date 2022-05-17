package GemGolem;

import net.worldseed.multipart.GenericModel;
import net.worldseed.multipart.animations.AnimationHandlerImpl;

public class AnimationHandlerGemGolem extends AnimationHandlerImpl {
    public AnimationHandlerGemGolem(GenericModel model) {
        super(model);

        animationPriorities.put("idle_extended", 5);
        animationPriorities.put("idle_retracted", 4);
        animationPriorities.put("walk", 3);
        animationPriorities.put("attack", 2);
        animationPriorities.put("attack_near", 1);
        animationPriorities.put("death", 0);
        animationPriorities.put("retract", -1);
        animationPriorities.put("extend", -2);

        initAnimations();
    }
}

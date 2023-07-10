package net.worldseed.multipart.events;

import net.worldseed.multipart.GenericModel;
import net.worldseed.multipart.animations.AnimationHandlerImpl;
import org.jetbrains.annotations.NotNull;

public class AnimationCompleteEvent implements ModelEvent {
    private final String animation;
    private final AnimationHandlerImpl.AnimationDirection direction;
    private final GenericModel model;

    public AnimationCompleteEvent(@NotNull GenericModel model, String animation, AnimationHandlerImpl.AnimationDirection direction) {
        this.animation = animation;
        this.direction = direction;
        this.model = model;
    }

    public String animation() {
        return animation;
    }

    public AnimationHandlerImpl.AnimationDirection direction() {
        return direction;
    }

    @Override
    public GenericModel getModel() {
        return model;
    }
}

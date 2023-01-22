package net.worldseed.multipart.events;

import net.minestom.server.entity.Entity;
import net.minestom.server.event.trait.EntityEvent;
import net.worldseed.multipart.animations.AnimationHandlerImpl;
import org.jetbrains.annotations.NotNull;

public class AnimationCompleteEvent implements EntityEvent {
    private final Entity entity;
    private final String animation;
    private final AnimationHandlerImpl.AnimationDirection direction;

    public AnimationCompleteEvent(@NotNull Entity entity, String animation, AnimationHandlerImpl.AnimationDirection direction) {
        this.entity = entity;
        this.animation = animation;
        this.direction = direction;
    }

    @Override
    public @NotNull Entity getEntity() {
        return entity;
    }

    public String animation() {
        return animation;
    }

    public AnimationHandlerImpl.AnimationDirection direction() {
        return direction;
    }
}

package net.worldseed.multipart.events;

import net.minestom.server.entity.Entity;
import net.minestom.server.event.trait.EntityEvent;
import org.jetbrains.annotations.NotNull;

public class EntityControlEvent implements EntityEvent {
    private final Entity entity;
    private final float movement;

    public EntityControlEvent(@NotNull Entity entity, float movement) {
        this.entity = entity;
        this.movement = movement;
    }

    @Override
    public @NotNull Entity getEntity() {
        return entity;
    }

    public float getMovement() {
        return movement;
    }
}


package net.worldseed.multipart.events;

import net.minestom.server.entity.Entity;
import net.minestom.server.event.trait.EntityEvent;
import org.jetbrains.annotations.NotNull;

public class EntityControlEvent implements EntityEvent {
    private final Entity entity;
    private final float forward;
    private final float sideways;
    private final boolean jump;

    public EntityControlEvent(@NotNull Entity entity, float forward, float sideways, boolean jump) {
        this.entity = entity;
        this.forward = forward;
        this.sideways = sideways;
        this.jump = jump;
    }

    @Override
    public @NotNull Entity getEntity() {
        return entity;
    }

    public float getForward() {
        return forward;
    }

    public float getSideways() {
        return sideways;
    }

    public boolean getJump() {
        return jump;
    }
}


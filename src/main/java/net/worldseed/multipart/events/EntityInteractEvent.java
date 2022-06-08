package net.worldseed.multipart.events;

import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.EntityEvent;
import org.jetbrains.annotations.NotNull;

public class EntityInteractEvent implements EntityEvent {
    private final Entity entity;
    private final Entity interactor;

    public EntityInteractEvent(@NotNull Entity entity, Entity interactor) {
        this.entity = entity;
        this.interactor = interactor;
    }

    @Override
    public @NotNull Entity getEntity() {
        return entity;
    }

    public @NotNull Entity getInteracted() {
        return interactor;
    }
}


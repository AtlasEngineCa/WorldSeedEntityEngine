package net.worldseed.multipart.events;

import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.EntityEvent;
import org.jetbrains.annotations.NotNull;

public class EntityDismountEvent implements EntityEvent {
    private final Entity entity;
    private final Entity rider;

    public EntityDismountEvent(@NotNull Entity entity, Entity rider) {
        this.entity = entity;
        this.rider = rider;
    }

    @Override
    public @NotNull Entity getEntity() {
        return entity;
    }

    public @NotNull Entity getRider() {
        return rider;
    }
}


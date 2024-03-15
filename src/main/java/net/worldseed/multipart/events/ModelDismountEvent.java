package net.worldseed.multipart.events;

import net.minestom.server.entity.Entity;
import net.worldseed.multipart.GenericModel;
import org.jetbrains.annotations.NotNull;

public record ModelDismountEvent(GenericModel model, Entity rider) implements ModelEvent {
    public ModelDismountEvent(@NotNull GenericModel model, Entity rider) {
        this.rider = rider;
        this.model = model;
    }

    @Override
    public @NotNull GenericModel model() {
        return model;
    }

    @Override
    public @NotNull Entity rider() {
        return rider;
    }
}


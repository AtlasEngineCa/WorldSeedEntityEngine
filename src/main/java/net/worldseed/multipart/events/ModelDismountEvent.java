package net.worldseed.multipart.events;

import net.minestom.server.entity.Entity;
import net.worldseed.multipart.GenericModel;
import org.jetbrains.annotations.NotNull;

public class ModelDismountEvent implements ModelEvent {
    private final GenericModel model;
    private final Entity rider;

    public ModelDismountEvent(@NotNull GenericModel model, Entity rider) {
        this.rider = rider;
        this.model = model;
    }

    @Override
    public @NotNull GenericModel getModel() {
        return model;
    }

    public @NotNull Entity getRider() {
        return rider;
    }
}


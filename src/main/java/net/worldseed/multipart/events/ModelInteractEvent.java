package net.worldseed.multipart.events;

import net.minestom.server.entity.Entity;
import net.worldseed.multipart.GenericModel;
import org.jetbrains.annotations.NotNull;

public class ModelInteractEvent implements ModelEvent {
    private final GenericModel model;
    private final Entity interactor;

    public ModelInteractEvent(@NotNull GenericModel model, Entity interactor) {
        this.model = model;
        this.interactor = interactor;
    }

    @Override
    public @NotNull GenericModel getModel() {
        return model;
    }

    public @NotNull Entity getInteracted() {
        return interactor;
    }
}


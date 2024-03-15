package net.worldseed.multipart.events;

import net.worldseed.multipart.GenericModel;
import org.jetbrains.annotations.NotNull;

public record ModelControlEvent(GenericModel model, float forward, float sideways, boolean jump) implements ModelEvent {
    public ModelControlEvent(@NotNull GenericModel model, float forward, float sideways, boolean jump) {
        this.model = model;
        this.forward = forward;
        this.sideways = sideways;
        this.jump = jump;
    }

    @Override
    public @NotNull GenericModel model() {
        return model;
    }
}


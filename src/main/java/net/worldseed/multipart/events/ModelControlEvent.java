package net.worldseed.multipart.events;

import net.worldseed.multipart.GenericModel;
import org.jetbrains.annotations.NotNull;

public class ModelControlEvent implements ModelEvent {
    private final GenericModel model;
    private final float forward;
    private final float sideways;
    private final boolean jump;

    public ModelControlEvent(@NotNull GenericModel model, float forward, float sideways, boolean jump) {
        this.model = model;
        this.forward = forward;
        this.sideways = sideways;
        this.jump = jump;
    }

    @Override
    public @NotNull GenericModel getModel() {
        return model;
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


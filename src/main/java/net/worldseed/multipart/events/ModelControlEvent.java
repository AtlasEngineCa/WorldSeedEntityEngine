package net.worldseed.multipart.events;

import net.minestom.server.network.packet.client.play.ClientSteerVehiclePacket;
import net.worldseed.multipart.GenericModel;
import org.jetbrains.annotations.NotNull;

public record ModelControlEvent(GenericModel model, ClientSteerVehiclePacket packet) implements ModelEvent {
    public ModelControlEvent(@NotNull GenericModel model, ClientSteerVehiclePacket packet) {
        this.model = model;
        this.packet = packet;
    }

    @Override
    public @NotNull GenericModel model() {
        return model;
    }

    public @NotNull ClientSteerVehiclePacket packet() {
        return packet;
    }
}


package net.worldseed.multipart.events;

import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.network.packet.client.play.ClientInputPacket;
import net.worldseed.multipart.GenericModel;
import org.jetbrains.annotations.NotNull;

public record ModelControlEvent(GenericModel model, ClientInputPacket packet) implements ModelEvent {
    public ModelControlEvent(@NotNull GenericModel model, ClientInputPacket packet) {
        this.model = model;
        this.packet = packet;
    }

    @Override
    public @NotNull GenericModel model() {
        return model;
    }

    public @NotNull ClientInputPacket packet() {
        return packet;
    }
}


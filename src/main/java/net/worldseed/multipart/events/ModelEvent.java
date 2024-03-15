package net.worldseed.multipart.events;

import net.minestom.server.event.Event;
import net.worldseed.multipart.GenericModel;

public interface ModelEvent extends Event {
    GenericModel model();
}

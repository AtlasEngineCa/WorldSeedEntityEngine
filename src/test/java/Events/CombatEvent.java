package Events;

import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.entity.EntityDamageEvent;

public class CombatEvent {
    public static void hook(GlobalEventHandler handler) {
        handler.addListener(EntityDamageEvent.class, event -> {
            System.out.println("Entity Hit");
        });
    }
}

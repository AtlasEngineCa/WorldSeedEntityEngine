package events;

import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.damage.EntityDamage;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.entity.EntityAttackEvent;

public class CombatEvent {
    public static void hook(GlobalEventHandler handler) {
        handler.addListener(EntityAttackEvent.class, event -> {
            if (event.getTarget() instanceof LivingEntity target) {
                int damage = 1;
                target.damage(EntityDamage.fromEntity(event.getEntity(), damage));
            }
        });
    }
}

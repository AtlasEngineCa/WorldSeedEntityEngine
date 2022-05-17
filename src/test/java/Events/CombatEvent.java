package Events;

import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.entity.EntityAttackEvent;

public class CombatEvent {
    public static void hook(GlobalEventHandler handler) {
        handler.addListener(EntityAttackEvent.class, event -> {
            if (event.getTarget() instanceof LivingEntity target) {
                int damage = 1;

                target.damage(DamageType.fromEntity(event.getEntity()), damage);
                target.takeKnockback(
                        0.5f,
                        Math.sin(event.getEntity().getPosition().yaw() * (Math.PI / 180)),
                        -Math.cos(event.getEntity().getPosition().yaw() * (Math.PI / 180))
                );
            }
        });
    }
}

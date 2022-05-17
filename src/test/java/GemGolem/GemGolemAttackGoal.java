package GemGolem;

import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.ai.GoalSelector;
import net.worldseed.multipart.animations.AnimationHandler;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

public class GemGolemAttackGoal extends GoalSelector {
    private final AnimationHandler animationHandler;
    private final Duration attackDuration = Duration.ofSeconds(3);
    private long lastUpdateTime;

    public GemGolemAttackGoal(@NotNull EntityCreature entityCreature, AnimationHandler handler) {
        super(entityCreature);
        this.animationHandler = handler;
    }

    @Override
    public boolean shouldStart() {
        if (entityCreature.getTarget() == null) {
            entityCreature.setTarget(findTarget());
            if (entityCreature.getTarget() == null) return false;
        }
        return (entityCreature.getDistance(entityCreature.getTarget()) <= 5);
    }

    @Override
    public void start() {
    }

    @Override
    public void tick(long time) {
        if (!entityCreature.isDead() && entityCreature.getTarget() != null) {
            double dist = entityCreature.getDistance(entityCreature.getTarget());

            if (attackDuration.toMillis() + lastUpdateTime < time && dist <= 5) {
                this.lastUpdateTime = time;
                attackNear();
            }
        }
    }

    private void attackNear() {
        animationHandler.playOnce("attack_near", (a) -> {});
    }

    @Override
    public boolean shouldEnd() {
        final Entity target = entityCreature.getTarget();
        return target == null ||
                target.isRemoved() ||
                entityCreature.isDead() ||
                entityCreature.isRemoved() ||
                target.getPosition().distance(entityCreature.getPosition()) <= 5;
    }

    @Override
    public void end() {
    }
}

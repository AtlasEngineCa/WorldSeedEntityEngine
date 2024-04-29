package demo_models.bulbasaur;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.ai.GoalSelector;
import net.minestom.server.entity.pathfinding.Navigator;
import net.worldseed.multipart.animations.AnimationHandler;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

public class BulbasaurMoveGoal extends GoalSelector {
    private final AnimationHandler animationHandler;
    private final Duration pathDuration = Duration.ofSeconds(1);
    private final int minDistance = 3;
    private final int maxDistance = 40;
    private Entity target = null;
    private Pos lastTargetPos = Pos.ZERO;
    private boolean forceEnd;
    private long lastUpdateTime;

    public BulbasaurMoveGoal(@NotNull BulbasaurMob entityCreature, AnimationHandler handler) {
        super(entityCreature);
        this.animationHandler = handler;
    }

    @Override
    public boolean shouldStart() {
        target = findTarget();
        if (target == null) return false;
        if (entityCreature.getPassengers().contains(target)) return false;
        if (!this.animationHandler.getPlaying().equals("animation.bulbasaur.ground_idle")) return false;

        if (entityCreature.getNavigator().getPathPosition() != null)
            if (entityCreature.getNavigator().getPathPosition().samePoint(target.getPosition())) return false;

        return entityCreature.getDistance(target) < maxDistance && entityCreature.getDistance(target) > minDistance;
    }

    @Override
    public void start() {
        this.animationHandler.playRepeat("animation.bulbasaur.ground_walk");

        this.entityCreature.setTarget(target);
        Navigator navigator = entityCreature.getNavigator();

        this.lastTargetPos = target.getPosition();

        if (navigator.getPathPosition() == null || !navigator.getPathPosition().samePoint(lastTargetPos)) {
            navigator.setPathTo(lastTargetPos);
        } else {
            forceEnd = true;
        }
    }

    @Override
    public void tick(long time) {
        if (forceEnd || pathDuration.isZero() || pathDuration.toMillis() + lastUpdateTime > time) {
            return;
        }
        final Pos targetPos = entityCreature.getTarget() != null ? entityCreature.getTarget().getPosition() : null;
        if (targetPos != null && !targetPos.samePoint(lastTargetPos)) {
            this.lastUpdateTime = time;
            this.lastTargetPos = targetPos;
            this.entityCreature.getNavigator().setPathTo(targetPos);
        }
    }

    @Override
    public boolean shouldEnd() {
        final Entity target = entityCreature.getTarget();

        return forceEnd
                || target == null
                || target.isRemoved()
                || entityCreature.getDistance(target) >= maxDistance
                || entityCreature.getDistance(target) <= minDistance
                || entityCreature.getPassengers().contains(target)
                || !this.animationHandler.getPlaying().equals("animation.bulbasaur.ground_walk");
    }

    @Override
    public void end() {
        this.entityCreature.getNavigator().setPathTo(null);
        this.animationHandler.stopRepeat("animation.bulbasaur.ground_walk");
        this.forceEnd = false;
    }
}

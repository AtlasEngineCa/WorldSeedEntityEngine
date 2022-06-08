package GemGolem;


import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.ai.GoalSelector;
import net.worldseed.multipart.animations.AnimationHandler;
import net.worldseed.multipart.mount.ControlGoal;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

public class GemGolemControlGoal extends GoalSelector implements ControlGoal {
    private final AnimationHandler animationHandler;
    private Pos lastTargetPos;
    private boolean forceEnd;
    private final Duration pathDuration = Duration.ofMillis(100);
    private long lastUpdateTime;

    private float forward = 0f;
    private float sideways = 0f;

    public GemGolemControlGoal(@NotNull GemGolemMob entityCreature, AnimationHandler handler) {
        super(entityCreature);
        this.animationHandler = handler;
    }

    @Override
    public boolean shouldStart() {
        return !entityCreature.getPassengers().isEmpty();
    }

    @Override
    public void start() {
        lastTargetPos = Pos.ZERO;
        this.animationHandler.playRepeat("walk");
    }

    @Override
    public void tick(long time) {
        Entity passenger = entityCreature.getPassengers().stream().findAny().orElse(null);
        if (passenger == null) {
            forceEnd = true;
            return;
        }

        if (forceEnd || pathDuration.isZero() || pathDuration.toMillis() + lastUpdateTime > time) {
            return;
        }

        final Pos targetPos = entityCreature.getPosition().add(passenger.getPosition().withPitch(0.3f).direction().normalize().mul(forward).mul(10));

        if (!targetPos.samePoint(lastTargetPos)) {
            this.lastUpdateTime = time;
            this.lastTargetPos = targetPos;
            this.entityCreature.getNavigator().setPathTo(targetPos);
            ((GemGolemMob)entityCreature).facePoint(targetPos);
        }
    }

    @Override
    public boolean shouldEnd() {
        return forceEnd;
    }

    public void setForward(float forward) {
        this.forward = forward;
    }

    @Override
    public void end() {
        this.entityCreature.getNavigator().setPathTo(null);
        this.animationHandler.stopRepeat("walk");
        this.forceEnd = false;
    }
}

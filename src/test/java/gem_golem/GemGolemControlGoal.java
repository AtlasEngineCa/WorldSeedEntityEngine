package gem_golem;


import net.minestom.server.MinecraftServer;
import net.minestom.server.attribute.Attribute;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.ai.GoalSelector;
import net.minestom.server.timer.TaskSchedule;
import net.worldseed.multipart.animations.AnimationHandler;
import net.worldseed.multipart.mount.ControlGoal;
import org.jetbrains.annotations.NotNull;

public class GemGolemControlGoal extends GoalSelector implements ControlGoal {
    private final AnimationHandler animationHandler;
    private boolean forceEnd;
    private long lastUpdateTime;
    private boolean jumpCooldown;
    private long lastRotationTime;

    private float forward = 0f;
    private float sideways = 0f;

    public GemGolemControlGoal(@NotNull GemGolemMob entityCreature, AnimationHandler handler) {
        super(entityCreature);
        this.animationHandler = handler;
    }

    @Override
    public boolean shouldStart() {
        return !entityCreature.getPassengers().isEmpty() && forward != 0;
    }

    @Override
    public void start() {
        this.animationHandler.playRepeat("walk");
        this.entityCreature.getNavigator().setPathTo(null);
    }

    @Override
    public void tick(long time) {
        Entity passenger = entityCreature.getPassengers().stream().findAny().orElse(null);
        if (passenger == null) {
            forceEnd = true;
            return;
        }

        if (lastRotationTime + 100 > time) return;
        lastRotationTime = time;

        if (forceEnd) {
            return;
        }

        final Vec movement = passenger.getPosition().withPitch(0.3f).direction().normalize().mul(20).mul(forward);
        ((GemGolemMob)entityCreature).facePoint(entityCreature.getPosition().add(movement));
        entityCreature.setVelocity(movement.mul(entityCreature.getAttribute(Attribute.MOVEMENT_SPEED).getValue()));
    }

    @Override
    public boolean shouldEnd() {
        return forceEnd || (Math.abs(this.forward) - 0.001f) < 0f;
    }

    public void setForward(float forward) {
        this.forward = forward;
    }

    @Override
    public void setSideways(float sideways) {
    }

    @Override
    public void setJump(boolean jump) {
        if (jump && !jumpCooldown) {
            jumpCooldown = true;
            animationHandler.playOnce("attack", (i) -> {});

            MinecraftServer.getSchedulerManager().scheduleTask(() -> {
                jumpCooldown = false;
            }, TaskSchedule.tick(30), TaskSchedule.stop());
        }
    }

    @Override
    public void end() {
        this.animationHandler.stopRepeat("walk");
        this.forceEnd = false;
    }
}

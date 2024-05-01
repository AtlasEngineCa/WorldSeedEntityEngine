package demo_models.gem_golem;

import net.minestom.server.entity.Entity;
import net.minestom.server.entity.ai.GoalSelector;
import net.minestom.server.utils.time.Cooldown;
import net.minestom.server.utils.time.TimeUnit;
import net.worldseed.multipart.animations.AnimationHandler;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

public class GemGolemActivateGoal extends GoalSelector {
    private final AnimationHandler animationHandler;
    private final Cooldown startCooldown = new Cooldown(Duration.of(20, TimeUnit.SERVER_TICK));
    private Entity target = null;
    private boolean playing = false;

    public GemGolemActivateGoal(@NotNull GemGolemMob entityCreature, AnimationHandler handler) {
        super(entityCreature);
        this.animationHandler = handler;
        deactivate();
    }

    private void activate() {
        playing = true;

        animationHandler.playOnce("extend", () -> {
            ((GemGolemMob) entityCreature).setSleeping(false);
            animationHandler.playRepeat("idle_extended");
            playing = false;
        });
    }

    private void deactivate() {
        playing = true;
        ((GemGolemMob) entityCreature).setSleeping(true);

        animationHandler.stopRepeat("idle_extended");

        animationHandler.playOnce("retract", () -> {
            animationHandler.playOnce("idle_retracted", () -> {
                playing = false;
            });
        });
    }

    @Override
    public boolean shouldStart() {
        if (startCooldown.isReady(System.currentTimeMillis()) && !playing) {
            startCooldown.refreshLastUpdate(System.currentTimeMillis());

            if (target == null || target.isRemoved()) target = findTarget();
            if (target == null) return false;

            if (((GemGolemMob) entityCreature).isSleeping()) {
                return entityCreature.getDistance(target) <= 10;
            } else {
                return entityCreature.getDistance(target) >= 15;
            }
        }

        return false;
    }

    @Override
    public void start() {
        if (((GemGolemMob) entityCreature).isSleeping()) {
            activate();
        } else {
            deactivate();
        }
    }

    @Override
    public void tick(long time) {
    }

    @Override
    public boolean shouldEnd() {
        return true;
    }

    @Override
    public void end() {
    }
}

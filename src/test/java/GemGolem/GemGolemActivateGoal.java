package GemGolem;

import net.minestom.server.entity.Entity;
import net.minestom.server.entity.ai.GoalSelector;
import net.worldseed.multipart.animations.AnimationHandler;
import org.jetbrains.annotations.NotNull;

public class GemGolemActivateGoal extends GoalSelector {
    private final AnimationHandler animationHandler;
    private Entity target = null;
    private boolean playing = false;

    public GemGolemActivateGoal(@NotNull GemGolemMob entityCreature, AnimationHandler handler) {
        super(entityCreature);
        this.animationHandler = handler;
        deactivate();
    }

    private void activate() {
        playing = true;

        animationHandler.playOnce("extend", (v) -> {
            ((GemGolemMob)entityCreature).setSleeping(false);
            animationHandler.playRepeat("idle_extended");
            playing = false;
        });
        animationHandler.setUpdates(true);
    }

    private void deactivate() {
        playing = true;
        ((GemGolemMob)entityCreature).setSleeping(true);

        animationHandler.stopRepeat("idle_extended");

        animationHandler.playOnce("retract", (v) -> {
            animationHandler.playOnce("idle_retracted", (v2) -> {
                playing = false;
                animationHandler.setUpdates(false);
            });
        });
    }

    @Override
    public boolean shouldStart() {
        if (!playing) {
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
        if (((GemGolemMob)entityCreature).isSleeping()) {
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

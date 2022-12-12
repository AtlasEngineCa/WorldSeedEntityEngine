package gem_golem;

import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.ai.GoalSelector;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.particle.ParticleCreator;
import net.minestom.server.timer.ExecutionType;
import net.minestom.server.timer.TaskSchedule;
import net.worldseed.multipart.GenericModel;
import net.worldseed.multipart.animations.AnimationHandler;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

public class GemGolemAttackGoal extends GoalSelector {
    private final AnimationHandler animationHandler;
    private final Duration attackDuration = Duration.ofSeconds(6);
    private final GenericModel model;
    private long lastUpdateTime;
    private int attackIndex = 0;

    public GemGolemAttackGoal(@NotNull EntityCreature entityCreature, AnimationHandler handler, GenericModel model) {
        super(entityCreature);
        this.animationHandler = handler;
        this.model = model;
    }

    @Override
    public boolean shouldStart() {
        if (entityCreature.getTarget() == null) {
            entityCreature.setTarget(findTarget());
            if (entityCreature.getTarget() == null) return false;
        }

        if (!model.getPassengers().isEmpty()) return false;

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
        if (attackIndex == 0) {
            animationHandler.playOnce("attack", (a) -> {});
            attackIndex = 1;
        } else {
            animationHandler.playOnce("attack_near", (a) -> {});

            MinecraftServer.getSchedulerManager().scheduleTask(() -> {
                int intervals = 5;
                int totalLength = 500;
                int sections = totalLength / intervals;

                AtomicInteger counter = new AtomicInteger();

                MinecraftServer.getSchedulerManager().submitTask(() -> {
                    Point p = model.getVFX("hit_vfx");

                    if (p != null) {
                        ParticlePacket packet = ParticleCreator.createParticlePacket(Particle.FLAME, p.x(), p.y(), p.z(), 0, 0, 0, 1);
                        entityCreature.sendPacketToViewers(packet);
                    }

                    if (counter.addAndGet(1) > sections) {
                        if (p != null) {
                            ParticlePacket packet = ParticleCreator.createParticlePacket(Particle.EXPLOSION, p.x(), p.y(), p.z(), 0, 0, 0, 1);
                            entityCreature.sendPacketToViewers(packet);
                        }

                        return TaskSchedule.stop();
                    } else {
                        return TaskSchedule.millis(intervals);
                    }
                }, ExecutionType.ASYNC);
            }, TaskSchedule.millis(1000), TaskSchedule.stop(), ExecutionType.ASYNC);

            attackIndex = 0;
        }
    }

    @Override
    public boolean shouldEnd() {
        final Entity target = entityCreature.getTarget();
        return target == null ||
                target.isRemoved() ||
                entityCreature.isDead() ||
                entityCreature.isRemoved() ||
                !model.getPassengers().isEmpty() ||
                target.getPosition().distance(entityCreature.getPosition()) <= 5;
    }

    @Override
    public void end() {
    }
}

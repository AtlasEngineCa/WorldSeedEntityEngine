package demo_models.bulbasaur;

import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.entity.damage.EntityDamage;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.registry.DynamicRegistry;
import net.minestom.server.timer.Task;
import net.minestom.server.utils.position.PositionUtils;
import net.minestom.server.utils.time.TimeUnit;
import net.worldseed.multipart.animations.AnimationHandler;
import net.worldseed.multipart.animations.AnimationHandlerImpl;
import net.worldseed.multipart.events.ModelDamageEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public class BulbasaurMob extends EntityCreature {
    private final BulbasaurModel model;
    private final AnimationHandler animationHandler;
    boolean dying = false;
    private Task stateTask;

    public BulbasaurMob(Instance instance, Pos pos) {
        super(EntityType.ZOMBIE);
        this.setInvisible(true);
        this.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.1f);

        this.model = new BulbasaurModel();
        model.init(instance, pos, 1f);

        model.eventNode().addListener(ModelDamageEvent.class, (event) -> {
            damage(event.getDamage().getType(), event.getDamage().getAmount());
        });

        this.animationHandler = new AnimationHandlerImpl(model);
        this.animationHandler.playRepeat("animation.bulbasaur.ground_idle");

        addAIGroup(
                List.of(
                        new BulbasaurMoveGoal(this, animationHandler)
                ),
                List.of(
                        new BulbasaurTarget(this)
                )
        );

        this.setInstance(instance, pos).join();
    }

    @Override
    public void updateNewViewer(@NotNull Player player) {
        super.updateNewViewer(player);
        this.model.addViewer(player);
    }

    @Override
    public void updateOldViewer(@NotNull Player player) {
        super.updateOldViewer(player);
        this.model.removeViewer(player);
    }

    public void facePoint(Point point) {
        Point e = this.position.sub(point);
        model.setGlobalRotation(180 + PositionUtils.getLookYaw(e.x(), e.z()));
    }

    private void facePlayer() {
        Entity target = this.getTarget();
        if (target == null) return;
        if (getPassengers().contains(target)) return;

        Point e = this.position.sub(target.getPosition());
        model.setGlobalRotation(180 + PositionUtils.getLookYaw(e.x(), e.z()));
    }

    @Override
    public void tick(long time) {
        super.tick(time);
        if (!this.isDead) this.model.setPosition(this.position);
        facePlayer();
    }

    @Override
    public boolean damage(@NotNull DynamicRegistry.Key<DamageType> type, float amount) {
        if (this.dying) return false;
        this.animationHandler.playOnce("animation.bulbasaur.cry", () -> {
        });
        this.model.setState("hit");

        if (stateTask != null && stateTask.isAlive()) stateTask.cancel();
        this.stateTask = MinecraftServer.getSchedulerManager()
                .buildTask(() -> this.model.setState("normal")).delay(7, TimeUnit.CLIENT_TICK)
                .schedule();

        return super.damage(type, amount);
    }

    @Override
    public void remove() {
        var viewers = Set.copyOf(this.getViewers());
        this.dying = true;
        this.animationHandler.playOnce("animation.bulbasaur.faint", () -> {
            this.model.destroy();
            this.animationHandler.destroy();

            ParticlePacket packet = new ParticlePacket(
                    Particle.POOF,
                    false,
                    this.position.x(),
                    this.position.y() + 1,
                    this.position.z(),
                    1,
                    1,
                    1,
                    0,
                    50
            );
            viewers.forEach(v -> v.sendPacket(packet));

            super.remove();
        });
    }

}

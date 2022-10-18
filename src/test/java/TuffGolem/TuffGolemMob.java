package TuffGolem;

import GemGolem.GemGolemTarget;
import net.minestom.server.MinecraftServer;
import net.minestom.server.attribute.Attribute;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.*;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.entity.damage.EntityDamage;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.particle.ParticleCreator;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.ExecutionType;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.utils.time.TimeUnit;
import net.worldseed.multipart.animations.AnimationHandler;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public class TuffGolemMob extends EntityCreature {
    private final TuffGolem model;
    private final AnimationHandler animationHandler;
    private final Player player;
    private Task stateTask;

    public TuffGolemMob(Instance instance, Pos pos, Player player) {
        super(EntityType.ZOMBIE);
        this.entityMeta.setInvisible(true);
        this.player = player;

        this.model = new TuffGolem();
        model.init(instance, pos, this);

        this.animationHandler = new TuffGolemAnimationHandler(model);
        animationHandler.playRepeat("walk");

        addAIGroup(
                List.of(
                        new TuffGolemMoveGoal(this, animationHandler)
                ),
                List.of(
                        new GemGolemTarget(this)
                )
        );

        setBoundingBox(1, 4, 1);
        this.setInstance(instance, pos);

        this.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.12f);

        // No way to set size without modifying minestom
        // PufferfishMeta meta = ((PufferfishMeta)this.getLivingEntityMeta());
        // meta.setSize(20);
    }

    @Override
    public void tick(long time) {
        super.tick(time);
        if (this.isDead) {
            return;
        }

        this.model.setPosition(this.getPosition());
        this.model.setGlobalRotation(-player.getPosition().yaw());
    }

    @Override
    public boolean damage(@NotNull DamageType type, float value) {
        if (type instanceof EntityDamage source) {
            if (model.getPassengers().contains(source.getSource())) {
                animationHandler.playOnce("grab", (i) -> {});
                MinecraftServer.getSchedulerManager().scheduleTask(() -> {
                    this.getInstance().getNearbyEntities(this.position, 8)
                            .stream()
                            .filter(e -> !model.getPassengers().contains(e)
                                    && !e.hasTag(Tag.String("WSEE"))
                                    && e != this)
                            .forEach(e -> {
                                if (e instanceof LivingEntity) {
                                    ((LivingEntity) e).damage(type, value);
                                    e.takeKnockback(0.9f, Math.sin(this.getPosition().yaw() * 0.017453292), -Math.cos(this.getPosition().yaw() * 0.017453292));
                                }
                            });
                }, TaskSchedule.millis(1400), TaskSchedule.stop(), ExecutionType.ASYNC);

                return false;
            }
        }

        // this.model.setState("hit");
        this.model.setState("animated_head");

        if (stateTask != null && stateTask.isAlive()) stateTask.cancel();
        this.stateTask = MinecraftServer.getSchedulerManager()
                .buildTask(() -> this.model.setState("normal")).delay(7, TimeUnit.CLIENT_TICK)
                .schedule();

        return super.damage(type, value);
    }

    @Override
    public void remove() {
        var viewers = Set.copyOf(this.getViewers());
        this.animationHandler.playOnce("grab", (cb) -> {
            this.model.destroy();
            this.animationHandler.destroy();
            ParticlePacket packet = ParticleCreator.createParticlePacket(Particle.POOF, position.x(), position.y() + 1, position.z(), 1, 1, 1, 50);
            viewers.forEach(v -> v.sendPacket(packet));
        });

        super.remove();
    }

    @Override
    public @NotNull Set<Entity> getPassengers() {
        return model.getPassengers();
    }
}

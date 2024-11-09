package demo_models.tuff_golem;

import demo_models.gem_golem.GemGolemTarget;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.registry.DynamicRegistry;
import net.minestom.server.timer.Task;
import net.minestom.server.utils.time.TimeUnit;
import net.worldseed.multipart.animations.AnimationHandler;
import net.worldseed.multipart.animations.AnimationHandlerImpl;
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
        model.init(instance, pos);

        this.animationHandler = new AnimationHandlerImpl(model);
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
    public boolean damage(@NotNull DynamicRegistry.Key<DamageType> type, float value) {
        this.model.setState("animated_head");

        if (stateTask != null && stateTask.isAlive()) stateTask.cancel();
        this.stateTask = MinecraftServer.getSchedulerManager()
                .buildTask(() -> this.model.setState("normal")).delay(7, TimeUnit.CLIENT_TICK)
                .schedule();

        return super.damage(type, value);
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

    @Override
    public void remove() {
        var viewers = Set.copyOf(this.getViewers());
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
    }
}

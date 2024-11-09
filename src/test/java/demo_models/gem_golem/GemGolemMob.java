package demo_models.gem_golem;

import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.*;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.entity.damage.EntityDamage;
import net.minestom.server.entity.metadata.other.ArmorStandMeta;
import net.minestom.server.entity.metadata.water.fish.PufferfishMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.registry.DynamicRegistry;
import net.minestom.server.timer.Task;
import net.minestom.server.utils.position.PositionUtils;
import net.minestom.server.utils.time.TimeUnit;
import net.worldseed.multipart.animations.AnimationHandler;
import net.worldseed.multipart.animations.AnimationHandlerImpl;
import net.worldseed.multipart.events.ModelControlEvent;
import net.worldseed.multipart.events.ModelDamageEvent;
import net.worldseed.multipart.events.ModelDismountEvent;
import net.worldseed.multipart.events.ModelInteractEvent;
import net.worldseed.multipart.model_bones.BoneEntity;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public class GemGolemMob extends EntityCreature {
    static final String SEAT = "seat";

    private final GemGolemModel model;
    private final AnimationHandler animationHandler;
    private final GemGolemControlGoal controlGoal;
    private Task stateTask;
    private boolean sleeping = false;

    public GemGolemMob(Instance instance, Pos pos) {
        super(EntityType.PUFFERFISH);

        this.model = new GemGolemModel();

        BoneEntity nametag = new BoneEntity(EntityType.ARMOR_STAND, model, "nametag");
        nametag.setCustomNameVisible(true);
        nametag.setCustomName(Component.text("Gem Golem"));
        nametag.setNoGravity(true);
        nametag.setInvisible(true);
        nametag.setInstance(instance, pos);

        ArmorStandMeta meta = (ArmorStandMeta) nametag.getEntityMeta();
        meta.setMarker(true);

        model.init(instance, pos, nametag);

        this.animationHandler = new AnimationHandlerImpl(model);
        animationHandler.playRepeat("idle_extended");

        this.controlGoal = new GemGolemControlGoal(this, animationHandler);

        model.eventNode()
                .addListener(ModelDamageEvent.class, event -> {
                    if (event.getDamage() instanceof EntityDamage entityDamage) {
                        if (model.getPassengers(SEAT).contains(entityDamage.getSource())) return;
                    }

                    damage(event.getDamage().getType(), event.getDamage().getAmount());
                })
                .addListener(ModelInteractEvent.class, event -> model.mountEntity(SEAT, event.getInteracted()))
                .addListener(ModelDismountEvent.class, event -> model.dismountEntity(SEAT, event.rider()))
                .addListener(ModelControlEvent.class, event -> {
                    var forward = 0;

                    if (event.packet().forward()) forward = 1;
                    else if (event.packet().backward()) forward = -1;

                    controlGoal.setForward(forward);
                    controlGoal.setJump(event.packet().jump());
                });

        addAIGroup(
                List.of(
                        controlGoal,
                        new GemGolemActivateGoal(this, animationHandler),
                        new GemGolemMoveGoal(this, animationHandler),
                        new GemGolemAttackGoal(this, animationHandler, model)
                ),
                List.of(
                        new GemGolemTarget(this)
                )
        );

        setBoundingBox(3, 3, 3);
        this.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.3f);

        // Add the shadow for the entity
        int size = 15;
        var pfMeta = new PufferfishMeta(this, metadata) {
            @Override
            public void setState(State state) {
                super.metadata.setIndex(OFFSET, Metadata.VarInt(size));
            }
        };

        pfMeta.setState(PufferfishMeta.State.UNPUFFED);
        this.entityMeta = pfMeta;

        this.setInstance(instance, pos);
    }

    public void facePoint(Point point) {
        Point e = this.position.sub(point);
        model.setGlobalRotation(PositionUtils.getLookYaw(e.x(), e.z()));
    }

    private void facePlayer() {
        Entity target = this.getTarget();
        if (target == null) return;
        if (getPassengers().contains(target)) return;

        Point e = this.position.sub(target.getPosition());
        model.setGlobalRotation(PositionUtils.getLookYaw(e.x(), e.z()));
    }

    @Override
    public void tick(long time) {
        super.tick(time);
        if (!this.isDead) this.model.setPosition(this.position);
        facePlayer();
    }

    @Override
    public boolean damage(@NotNull DynamicRegistry.Key<DamageType> type, float value) {
        this.model.setState("hit");

        if (stateTask != null && stateTask.isAlive()) stateTask.cancel();
        this.stateTask = MinecraftServer.getSchedulerManager()
                .buildTask(() -> this.model.setState("normal")).delay(7, TimeUnit.CLIENT_TICK)
                .schedule();

        return super.damage(type, value);
    }

    @Override
    public void remove() {
        var viewers = Set.copyOf(this.getViewers());
        this.animationHandler.playOnce("death", () -> {
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

    public boolean isSleeping() {
        return sleeping;
    }

    public void setSleeping(boolean sleeping) {
        this.sleeping = sleeping;
    }

    @Override
    public @NotNull Set<Entity> getPassengers() {
        return model.getPassengers(SEAT);
    }
}

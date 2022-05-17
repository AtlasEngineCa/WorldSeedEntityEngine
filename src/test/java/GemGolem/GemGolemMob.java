package GemGolem;

import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.attribute.Attribute;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.entity.metadata.other.ArmorStandMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.timer.Task;
import net.minestom.server.utils.position.PositionUtils;
import net.minestom.server.utils.time.TimeUnit;
import net.worldseed.multipart.animations.AnimationHandler;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GemGolemMob extends EntityCreature {
    private final GemGolemModel model;
    private final AnimationHandler animationHandler;
    private Task stateTask;
    private boolean sleeping = false;

    public GemGolemMob(Instance instance, Pos pos) {
        super(EntityType.PUFFERFISH);

        LivingEntity nametag = new LivingEntity(EntityType.ARMOR_STAND);
        nametag.setCustomNameVisible(true);
        nametag.setCustomName(Component.text("Gem Golem"));
        nametag.setNoGravity(true);
        nametag.setInvisible(true);
        nametag.setInstance(instance, pos);

        ArmorStandMeta meta = (ArmorStandMeta) nametag.getEntityMeta();
        meta.setMarker(true);

        this.model = new GemGolemModel();
        model.init(instance, pos, this, nametag);

        this.animationHandler = new AnimationHandlerGemGolem(model);
        animationHandler.playRepeat("idle_extended");

        addAIGroup(
                List.of(
                        new GemGolemActivateGoal(this, animationHandler),
                        new GemGolemMoveGoal(this, animationHandler),
                        new GemGolemAttackGoal(this, animationHandler, model)
                ),
                List.of(
                        new GemGolemTarget(this)
                )
        );

        setBoundingBox(3, 3, 3);
        this.setInstance(instance, pos);
        this.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.15f);

        // No way to set size without modifying minestom
        // PufferfishMeta meta = ((PufferfishMeta)this.getLivingEntityMeta());
        // meta.setSize(20);
    }

    private void facePlayer() {
        Entity target = this.getTarget();
        if (target == null) return;

        Point e = this.position.sub(target.getPosition());
        model.setGlobalRotation(-PositionUtils.getLookYaw(e.x(), e.z()) + 180);
    }

    @Override
    public void tick(long time) {
        super.tick(time);
        if (!this.isDead) this.model.setPosition(this.position);
        facePlayer();
    }

    @Override
    public boolean damage(@NotNull DamageType type, float value) {
        this.model.setState("hit");

        if (stateTask != null && stateTask.isAlive()) stateTask.cancel();
        this.stateTask = MinecraftServer.getSchedulerManager()
                .buildTask(() -> this.model.setState("normal")).delay(20, TimeUnit.CLIENT_TICK)
                .schedule();
        
        return super.damage(type, value);
    }

    @Override
    public void remove() {
        super.remove();

        this.animationHandler.playOnce("death", (cb) -> {
            this.model.destroy();
            this.animationHandler.destroy();
        });
    }

    public void setSleeping(boolean b) {
        this.sleeping = b;
    }

    public boolean isSleeping() {
        return sleeping;
    }
}

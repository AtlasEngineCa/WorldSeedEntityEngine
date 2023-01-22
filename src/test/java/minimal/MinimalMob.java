package minimal;

import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.EntityType;
import net.minestom.server.instance.Instance;
import net.minestom.server.timer.TaskSchedule;
import net.worldseed.multipart.animations.AnimationHandler;
import net.worldseed.multipart.animations.AnimationHandlerImpl;
import net.worldseed.multipart.events.AnimationCompleteEvent;

public class MinimalMob extends EntityCreature {
    private final Minimal model;
    private final AnimationHandler animationHandler;

    public MinimalMob(Instance instance, Pos pos) {
        super(EntityType.ZOMBIE);
        this.setInvisible(true);

        this.model = new Minimal();
        model.init(instance, pos, this);

        this.animationHandler = new AnimationHandlerImpl(model);

        MinecraftServer.getSchedulerManager().scheduleTask(() -> {
            this.animationHandler.playOnce("opening", AnimationHandlerImpl.AnimationDirection.FORWARD, (a) -> {});
        }, TaskSchedule.immediate(), TaskSchedule.tick(200));

        MinecraftServer.getSchedulerManager().scheduleTask(() -> {
            this.animationHandler.playOnce("opening", AnimationHandlerImpl.AnimationDirection.PAUSE, (a) -> {});
        }, TaskSchedule.tick(50), TaskSchedule.tick(200));

        MinecraftServer.getSchedulerManager().scheduleTask(() -> {
            this.animationHandler.playOnce("opening", AnimationHandlerImpl.AnimationDirection.FORWARD, (a) -> {});
        }, TaskSchedule.tick(100), TaskSchedule.tick(200));

        MinecraftServer.getSchedulerManager().scheduleTask(() -> {
            this.animationHandler.playOnce("opening", AnimationHandlerImpl.AnimationDirection.PAUSE, (a) -> {});
        }, TaskSchedule.tick(150), TaskSchedule.tick(200));

        this.eventNode().addListener(AnimationCompleteEvent.class, event -> {
            System.out.println("Animation complete: " + event.animation() + " " + event.direction());
        });

        this.setInstance(instance, pos);
    }

    @Override
    public void remove() {
        super.remove();
        this.model.destroy();
        this.animationHandler.destroy();
    }
}

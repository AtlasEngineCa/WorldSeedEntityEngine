package demo_models.minimal;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.worldseed.multipart.animations.AnimationHandler;
import net.worldseed.multipart.animations.AnimationHandlerImpl;
import org.jetbrains.annotations.NotNull;

public class MinimalMob extends EntityCreature {
    private final Minimal model;
    private final AnimationHandler animationHandler;

    public MinimalMob(Instance instance, Pos pos) {
        super(EntityType.ZOMBIE);
        this.setInvisible(true);

        this.model = new Minimal();
        model.init(instance, pos);

        this.animationHandler = new AnimationHandlerImpl(model);
        this.animationHandler.playRepeat("dab");

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

    @Override
    public void remove() {
        super.remove();
        this.model.destroy();
        this.animationHandler.destroy();
    }
}

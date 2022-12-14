package minimal;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.EntityType;
import net.minestom.server.instance.Instance;
import net.worldseed.multipart.animations.AnimationHandler;
import net.worldseed.multipart.animations.AnimationHandlerImpl;

public class MinimalMob extends EntityCreature {
    private final Minimal model;
    private final AnimationHandler animationHandler;

    public MinimalMob(Instance instance, Pos pos) {
        super(EntityType.ZOMBIE);
        this.setInvisible(true);

        this.model = new Minimal();
        model.init(instance, pos, this);

        this.animationHandler = new AnimationHandlerImpl(model);
        this.animationHandler.playRepeat("wave");

        this.setInstance(instance, pos);
    }

    @Override
    public void remove() {
        super.remove();
        this.model.destroy();
        this.animationHandler.destroy();
    }
}

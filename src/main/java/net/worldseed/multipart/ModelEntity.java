package net.worldseed.multipart;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

/**
 * A real Minestom entity that owns and drives a {@link GenericModel}. Extend this instead of hand-wiring
 * an invisible "carrier" entity to a model.
 *
 * <p>It removes the boilerplate that every WSEE mob used to repeat:
 * <ul>
 *   <li><b>viewers</b> — the model is shown to exactly the players who can see this entity (auto-synced),</li>
 *   <li><b>position</b> — the model follows this entity every tick,</li>
 *   <li><b>lifecycle</b> — the model is destroyed with this entity,</li>
 *   <li><b>hits</b> — a hit on any of the model's hitboxes arrives as a <i>normal</i> Minestom
 *       {@link net.minestom.server.event.entity.EntityDamageEvent} /
 *       {@link net.minestom.server.event.player.PlayerEntityInteractEvent} on <b>this</b> entity
 *       (WSEE routes it via {@link GenericModel#getOwner()}); no custom events to hook.</li>
 * </ul>
 *
 * <p>The carrier entity is made invisible by default so only the model is seen. Typical usage:
 * <pre>{@code
 * public class GemGolem extends ModelEntity {
 *     public GemGolem(Instance instance, Pos pos) {
 *         super(EntityType.PUFFERFISH, new GemGolemModel(), instance, pos);
 *     }
 * }
 * // then just: node.addListener(EntityDamageEvent.class, e -> { if (e.getEntity() instanceof GemGolem g) ... });
 * }</pre>
 */
public class ModelEntity extends EntityCreature {
    private final GenericModel model;

    public ModelEntity(@NotNull EntityType entityType, @NotNull GenericModel model,
                       @NotNull Instance instance, @NotNull Pos position) {
        super(entityType);
        this.model = model;
        setInvisible(true);
        model.setOwner(this);
        model.init(instance, position);
        setInstance(instance, position);
    }

    /** The model this entity drives. */
    public @NotNull GenericModel getModel() {
        return model;
    }

    @Override
    public void updateNewViewer(@NotNull Player player) {
        super.updateNewViewer(player);
        model.addViewer(player);
    }

    @Override
    public void updateOldViewer(@NotNull Player player) {
        super.updateOldViewer(player);
        model.removeViewer(player);
    }

    @Override
    public void tick(long time) {
        super.tick(time);
        if (isRemoved()) return;
        Pos pos = getPosition();
        model.setPosition(pos);
        model.setGlobalRotation(pos.yaw(), pos.pitch());
    }

    @Override
    public void remove() {
        model.destroy();
        super.remove();
    }
}

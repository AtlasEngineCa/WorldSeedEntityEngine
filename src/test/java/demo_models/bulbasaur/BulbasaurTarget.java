package demo_models.bulbasaur;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.ai.TargetSelector;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;

public class BulbasaurTarget extends TargetSelector {
    private final int distance;

    public BulbasaurTarget(@NotNull EntityCreature entityCreature) {
        super(entityCreature);
        this.distance = 50;
    }

    @Override
    public @Nullable Entity findTarget() {
        final Instance instance = getEntityCreature().getInstance();
        final Pos entityCreaturePosition = entityCreature.getPosition();

        if (entityCreature.isDead() || instance == null) return null;
        final Chunk currentChunk = instance.getChunkAt(entityCreaturePosition);
        if (currentChunk == null) {
            return null;
        }

        Collection<Entity> nearbyPlayers = instance.getNearbyEntities(entityCreaturePosition, this.distance);

        Optional<Player> closestPlayer = nearbyPlayers
                .stream()
                .filter(entity -> entity instanceof Player)
                .map(entity -> (Player) entity)
                .min((p1, p2) -> (int) (p1.getDistance(entityCreaturePosition) - p2.getDistance(entityCreaturePosition)))
                .filter(p -> p.getPosition().distance(entityCreaturePosition) < this.distance);

        return closestPlayer.orElse(null);
    }
}

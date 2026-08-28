package net.worldseed.multipart;

import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.event.player.PlayerEntityInteractEvent;
import net.minestom.server.network.player.GameProfile;
import net.worldseed.multipart.model_bones.BoneEntity;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ModelEngineHitboxTest {
    @Test
    void redirectsHitboxAttackToModelOwner() {
        var model = new TestModel();
        var owner = new LivingEntity(EntityType.ZOMBIE);
        var attacker = new LivingEntity(EntityType.ZOMBIE);
        var hitbox = new BoneEntity(EntityType.INTERACTION, model, "hitbox");
        model.setOwner(owner);

        EntityAttackEvent forwarded = ModelEngine.ownerAttack(new EntityAttackEvent(attacker, hitbox));

        assertSame(attacker, forwarded.getEntity());
        assertSame(owner, forwarded.getTarget());
    }

    @Test
    void ignoresOrdinaryEntitiesAndUnownedHitboxes() {
        var attacker = new LivingEntity(EntityType.ZOMBIE);
        var ordinaryTarget = new LivingEntity(EntityType.ZOMBIE);
        assertNull(ModelEngine.ownerAttack(new EntityAttackEvent(attacker, ordinaryTarget)));

        var model = new TestModel();
        var hitbox = new BoneEntity(EntityType.INTERACTION, model, "hitbox");
        assertNull(ModelEngine.ownerAttack(new EntityAttackEvent(attacker, hitbox)));
    }

    @Test
    void redirectsHitboxInteractionToModelOwner() {
        MinecraftServer.init();
        try {
            var model = new TestModel();
            var owner = new LivingEntity(EntityType.ZOMBIE);
            var hitbox = new BoneEntity(EntityType.INTERACTION, model, "hitbox");
            var player = new Player(null, new GameProfile(UUID.randomUUID(), "test"));
            var interactionPoint = new Vec(0.25, 0.5, -0.25);
            model.setOwner(owner);
            ModelEngine.loadMappings(new StringReader("{}"), Path.of("."));

            var forwardedInteraction = new AtomicReference<PlayerEntityInteractEvent>();
            MinecraftServer.getGlobalEventHandler().addListener(PlayerEntityInteractEvent.class, event -> {
                if (event.getTarget() == owner) forwardedInteraction.set(event);
            });

            EventDispatcher.call(new PlayerEntityInteractEvent(player, hitbox, PlayerHand.OFF, interactionPoint));

            PlayerEntityInteractEvent forwarded = forwardedInteraction.get();
            assertNotNull(forwarded);
            assertSame(player, forwarded.getPlayer());
            assertSame(owner, forwarded.getTarget());
            assertSame(PlayerHand.OFF, forwarded.getHand());
            assertSame(interactionPoint, forwarded.getInteractPosition());
        } finally {
            MinecraftServer.stopCleanly();
        }
    }

    private static final class TestModel extends GenericModelImpl {
        @Override
        public String getId() {
            return "test";
        }
    }
}

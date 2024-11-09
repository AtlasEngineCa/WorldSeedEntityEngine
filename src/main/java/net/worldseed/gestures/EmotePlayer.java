package net.worldseed.gestures;

import com.google.gson.JsonObject;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.*;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.entity.EntityDamageEvent;
import net.minestom.server.event.player.PlayerEntityInteractEvent;
import net.minestom.server.instance.Instance;
import net.worldseed.multipart.animations.AnimationHandler;
import net.worldseed.multipart.animations.AnimationHandlerImpl;
import net.worldseed.multipart.events.ModelDamageEvent;
import net.worldseed.multipart.events.ModelInteractEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public abstract class EmotePlayer extends EntityCreature {

    private final EmoteModel model;
    private final AnimationHandler animationHandler;
    private int emoteIndex = 0;

    public EmotePlayer(Instance instance, Pos pos, PlayerSkin skin, EntityType entityType) {
        super(entityType);

        Entity self = this;
        this.model = new EmoteModel(skin) {
            @Override
            public void setPosition(Pos pos) {
                super.setPosition(pos);
                if (self.getInstance() != null) self.teleport(pos);
            }
        };

        model.init(instance, pos);

        setBoundingBox(0.8, 1.8, 0.8);
        this.setInvisible(true);
        this.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.001f);
        this.setInstance(instance, pos).join();

        this.animationHandler = new AnimationHandlerImpl(model) {
            @Override
            protected void loadDefaultAnimations() {
            }
        };

        this.eventNode().addListener(EntityDamageEvent.class, (event) -> {
            event.setCancelled(true);
            ModelDamageEvent modelDamageEvent = new ModelDamageEvent(model, event);
            EventDispatcher.call(modelDamageEvent);
        }).addListener(PlayerEntityInteractEvent.class, (event) -> {
            ModelInteractEvent modelInteractEvent = new ModelInteractEvent(model, event);
            EventDispatcher.call(modelInteractEvent);
        });

        this.model.draw();
        this.model.draw();
    }

    public EmotePlayer(Instance instance, Pos pos, PlayerSkin skin) {
        this(instance, pos, skin, EntityType.ZOMBIE);
    }

    /**
     * Loads the emotes into the animation handler
     *
     * @param emotes Map containing the emote name, and emote data
     */
    public void loadEmotes(Map<String, JsonObject> emotes) {
        for (Map.Entry<String, JsonObject> entry : emotes.entrySet()) {
            this.animationHandler.registerAnimation(entry.getKey(), entry.getValue(), emoteIndex);
            emoteIndex++;
        }
    }

    @Override
    public void remove() {
        this.model.destroy();
        this.animationHandler.destroy();
        super.remove();
    }

    @Override
    public void tick(long time) {
        var position = this.getPosition();
        super.tick(time);
        if (position.equals(this.getPosition())) return;
        this.model.setPosition(this.getPosition());
    }

    public void setRotation(float yaw) {
        this.model.setGlobalRotation(yaw);
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

    protected AnimationHandler getAnimationHandler() {
        return animationHandler;
    }
}

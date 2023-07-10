package net.worldseed.gestures;

import com.google.gson.JsonObject;
import net.minestom.server.attribute.Attribute;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.instance.Instance;
import net.worldseed.multipart.ModelConfig;
import net.worldseed.multipart.animations.AnimationHandler;
import net.worldseed.multipart.animations.AnimationHandlerImpl;

import java.util.Map;

public abstract class EmotePlayer extends EntityCreature {
    private static final ModelConfig modelConfig = new ModelConfig(
            ModelConfig.ModelType.ARMOUR_STAND,
            ModelConfig.InterpolationType.POSITION_INTERPOLATION,
            ModelConfig.Size.NORMAL,
            ModelConfig.ItemSlot.HAND
    );

    private final EmoteModel model;
    private final AnimationHandler animationHandler;
    private int emoteIndex = 0;

    public EmotePlayer(Instance instance, Pos pos, PlayerSkin skin, EntityType entityType) {
        super(entityType);

        this.model = new EmoteModel(skin);
        model.init(instance, pos, modelConfig);

        this.animationHandler = new AnimationHandlerImpl(model) {
            @Override
            protected void loadDefaultAnimations() {}
        };

        setBoundingBox(0.8, 1.8, 0.8);
        this.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.001f);
    }

    public EmotePlayer(Instance instance, Pos pos, PlayerSkin skin) {
        this(instance, pos, skin, EntityType.ZOMBIE);
    }

    /**
     * Loads the emotes into the animation handler
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

    protected AnimationHandler getAnimationHandler() {
        return animationHandler;
    }
}

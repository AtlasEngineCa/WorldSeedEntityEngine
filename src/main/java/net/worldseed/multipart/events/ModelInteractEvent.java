package net.worldseed.multipart.events;

import net.minestom.server.entity.Player;
import net.worldseed.multipart.model_bones.BoneEntity;
import net.minestom.server.event.player.PlayerEntityInteractEvent;
import net.worldseed.gestures.EmoteModel;
import net.worldseed.multipart.GenericModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ModelInteractEvent implements ModelEvent {
    private final GenericModel model;
    private final Player interactor;
    private final BoneEntity interactedBone;
    private final Player.Hand hand;

    public ModelInteractEvent(@NotNull EmoteModel model, PlayerEntityInteractEvent event) {
        this(model, event, null);
    }
    
    public ModelInteractEvent(@NotNull GenericModel model, PlayerEntityInteractEvent event, @Nullable BoneEntity interactedBone) {
        this.model = model;
        this.hand = event.getHand();
        this.interactor = event.getPlayer();
        this.interactedBone = interactedBone;
    }

    @Override
    public @NotNull GenericModel model() {
        return model;
    }

    public @NotNull Player.Hand getHand() {
        return hand;
    }

    public @NotNull Player getInteracted() { // This should probably be getInteractor() or getPlayer() but I left this untouched so code doesn't break
        return interactor;
    }
    
    public @Nullable BoneEntity getBone() {
        return interactedBone;
    }
}


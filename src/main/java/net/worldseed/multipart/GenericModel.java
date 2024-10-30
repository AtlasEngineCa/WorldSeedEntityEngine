package net.worldseed.multipart;

import net.kyori.adventure.util.RGBLike;
import net.minestom.server.Viewable;
import net.minestom.server.collision.Shape;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventHandler;
import net.minestom.server.instance.Instance;
import net.worldseed.multipart.animations.AnimationHandlerImpl;
import net.worldseed.multipart.events.ModelEvent;
import net.worldseed.multipart.model_bones.BoneEntity;
import net.worldseed.multipart.model_bones.ModelBone;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface GenericModel extends Viewable, EventHandler<ModelEvent>, Shape {
    /**
     * Get the ID of the model
     *
     * @return the model ID
     */
    String getId();

    /**
     * Get the pivot point of the model. Used for global rotation
     *
     * @return the global rotation pivot point
     */
    Point getPivot();

    /**
     * Get the rotation of the model on the X axis
     *
     * @return the pitch
     */
    double getPitch();

    /**
     * Get the rotation of the model on the Y axis
     *
     * @return the global rotation
     */
    double getGlobalRotation();

    /**
     * Set the rotation of the model on the Y axis
     *
     * @param rotation new global rotation
     */
    void setGlobalRotation(double rotation);

    /**
     * Set the rotation of the model on the Y and X axis
     *
     * @param yaw   new global rotation
     * @param pitch new pitch
     */
    void setGlobalRotation(double yaw, double pitch);

    /**
     * Get the postion offset for drawing the model
     *
     * @return the position
     */
    Point getGlobalOffset();

    /**
     * Get the position the model is being drawn at
     *
     * @return the model position
     */
    Pos getPosition();

    /**
     * Set the position of the model
     *
     * @param pos new model position
     */
    void setPosition(Pos pos);

    /**
     * Set the state of the model. By default, `normal` and `hit` are supported
     *
     * @param state the new state
     */
    void setState(String state);

    /**
     * Destroy the model
     */
    void destroy();

    void mountEntity(String name, Entity entity);

    void dismountEntity(String name, Entity entity);

    Collection<Entity> getPassengers(String name);

    /**
     * Get a VFX bone location
     *
     * @param name the name of the bone
     * @return the bone location
     */
    Point getVFX(String name);

    @ApiStatus.Internal
    ModelBone getPart(String boneName);

    @ApiStatus.Internal
    void draw();

    /**
     * Set the model's head rotation
     *
     * @param name     name of the bone
     * @param rotation rotation of head
     */
    void setHeadRotation(String name, double rotation);

    @NotNull List<ModelBone> getParts();

    Instance getInstance();

    Point getOffset(String bone);

    Point getDiff(String bone);

    void triggerAnimationEnd(String animation, AnimationHandlerImpl.AnimationDirection direction);

    void setGlobalScale(float scale);

    void removeGlowing();

    void setGlowing(RGBLike color);

    void removeGlowing(Player player);

    void setGlowing(Player player, RGBLike color);

    void attachModel(GenericModel model, String boneName);

    Map<String, List<GenericModel>> getAttachedModels();

    void detachModel(GenericModel model, String boneName);

    @Nullable BoneEntity generateRoot();

    void bindNametag(String name, Entity nametag);

    void unbindNametag(String name);

    @Nullable Entity getNametag(String name);
}

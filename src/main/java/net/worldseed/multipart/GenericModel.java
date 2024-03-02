package net.worldseed.multipart;

import net.minestom.server.Viewable;
import net.minestom.server.collision.Shape;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.event.EventHandler;
import net.minestom.server.instance.Instance;
import net.worldseed.multipart.animations.AnimationHandlerImpl;
import net.worldseed.multipart.events.ModelEvent;
import net.worldseed.multipart.model_bones.BoneEntity;
import net.worldseed.multipart.model_bones.ModelBone;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;
import java.util.Set;

public interface GenericModel extends Viewable, EventHandler<ModelEvent>, Shape {
    /**
     * Get the ID of the model
     * @return the model ID
     */
    String getId();

    /**
     * Get the pivot point of the model. Used for global rotation
     * @return the global rotation pivot point
     */
    Point getPivot();

    /**
     * Get the rotation of the model on the Y axis
     * @return the global rotation
     */
    double getGlobalRotation();

    /**
     * Set the rotation of the model on the Y axis
     * @param rotation new global rotation
     */
    void setGlobalRotation(double rotation);

    /**
     * Get the postion offset for drawing the model
     * @return the position
     */
    Point getGlobalOffset();

    /**
     * Get the position the model is being drawn at
     * @return the model position
     */
    Pos getPosition();

    /**
     * Set the position of the model
     * @param pos new model position
     */
    void setPosition(Pos pos);

    /**
     * Set the state of the model. By default, `normal` and `hit` are supported
     * @param state the new state
     */
    void setState(String state);

    /**
     * Destroy the model
     */
    void destroy();

    /**
     * Remove all hitboxes from the model
     */
    void removeHitboxes();

    void mountEntity(Entity entity);
    void dismountEntity(Entity entity);
    Set<Entity> getPassengers();

    /**
     * Get a VFX bone location
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
     * @param rotation rotation of head
     */
    void setHeadRotation(double rotation);

    List<ModelBone> getParts();

    ModelBone getSeat();

    /**
     * Check where a bone will be at a specified time during a specified animation
     * @param animation animation
     * @param bone bone name
     * @param time time in ticks
     * @return position of bone
     */
    Point getBoneAtTime(String animation, String bone, int time);

    /**
     * Set the entity used for the nametag
     * Takes over control of entity movement.
     * @param entity the entity
     */
    void setNametagEntity(BoneEntity entity);
    Entity getNametagEntity();

    Instance getInstance();

    Point getOffset(String bone);
    Point getDiff(String bone);

    void triggerAnimationEnd(String animation, AnimationHandlerImpl.AnimationDirection direction);

    void setScale(float scale);
}

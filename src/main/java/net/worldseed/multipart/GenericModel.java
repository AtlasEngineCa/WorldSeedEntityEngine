package net.worldseed.multipart;

import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.ApiStatus;

import java.util.Set;

public interface GenericModel {
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
    Point getPosition();

    /**
     * Set the position of the model
     * @param pos new model position
     */
    void setPosition(Point pos);

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
    Set<Entity> getPassenger();

    /**
     * Get a VFX bone location
     * @param name the name of the bone
     * @return the bone location
     */
    Point getVFX(String name);

    @ApiStatus.Internal
    ModelBone getPart(String boneName);

    @ApiStatus.Internal
    void drawBones(short tick);
}

package net.worldseed.multipart.model_bones.bone_types;

import net.minestom.server.entity.Entity;
import net.worldseed.multipart.model_bones.ModelBone;

import java.util.List;

public interface RideableBone extends ModelBone {
    /**
     * Allows to set the seat rotation independent of the model rotation.
     * @param followModelRotation if the seat should rotate with the model
     */
    void setFollowModelRotation(boolean followModelRotation);

    /**
     * @return if the seat is rotating independently of the model.
     */
    boolean isFollowModelRotation();

    /**
     * Delegator of {@link Entity#addPassenger(Entity)}
     */
    void addPassenger(Entity entity);

    /**
     * Delegator of {@link Entity#addPassenger(Entity)}
     */
    void removePassenger(Entity entity);

    /**
     * Delegator of {@link Entity#addPassenger(Entity)}
     */
    List<Entity> getPassengers();
}

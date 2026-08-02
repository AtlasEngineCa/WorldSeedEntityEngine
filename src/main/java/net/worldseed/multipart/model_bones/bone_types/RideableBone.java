package net.worldseed.multipart.model_bones.bone_types;

import net.minestom.server.entity.Entity;
import net.worldseed.multipart.model_bones.ModelBone;

import java.util.List;

public interface RideableBone extends ModelBone {
    void addPassenger(Entity entity);
    void removePassenger(Entity entity);
    List<Entity> getPassengers();
}

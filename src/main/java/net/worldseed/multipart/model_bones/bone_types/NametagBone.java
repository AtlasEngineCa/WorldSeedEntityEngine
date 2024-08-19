package net.worldseed.multipart.model_bones.bone_types;

import net.minestom.server.entity.Entity;
import net.worldseed.multipart.model_bones.ModelBone;

public interface NametagBone extends ModelBone {
    void bind(Entity nametag);
    void unbind();
    Entity getNametag();
}
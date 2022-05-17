package net.worldseed.multipart;

import net.minestom.server.coordinate.Vec;
import net.worldseed.multipart.animations.AnimationLoader;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public abstract class GenericModelImpl implements GenericModel {
    private final HashMap<String, ModelBone> parts = new HashMap<>();
    private final Set<ModelBonePart> viewableBones = new HashSet<>();
    private final Set<ModelBoneHitbox> hittableBones = new HashSet<>();
    private final HashMap<String, ModelBoneVFX> VFXBones = new HashMap<>();

    private Point position;
    private double globalRotation;

    @Override
    public double getGlobalRotation() {
        return globalRotation;
    }

    @Override
    public Point getPosition() {
        return position;
    }

    public void init(@Nullable Instance instance, @NotNull Pos position, ModelEngine.RenderType renderType, Entity masterEntity) {
        init(instance, position, renderType, masterEntity, null);
    }

    public void init(@Nullable Instance instance, @NotNull Pos position, ModelEngine.RenderType renderType, Entity masterEntity, LivingEntity nametagEntity) {
        JsonObject loadedModel = AnimationLoader.loadModel(getId());
        this.position = new Vec(position.x(), position.y(), position.z());

        // Build bones
        for (JsonElement bone : loadedModel.get("minecraft:geometry").getAsJsonArray().get(0).getAsJsonObject().get("bones").getAsJsonArray()) {
            JsonElement pivot = bone.getAsJsonObject().get("pivot");
            String name = bone.getAsJsonObject().get("name").getAsString();

            Point boneRotation = ModelEngine.getPos(bone.getAsJsonObject().get("rotation")).orElse(Pos.ZERO).mul(-1, -1, 1);
            Point pivotPos = ModelEngine.getPos(pivot).orElse(Pos.ZERO).mul(-1,1,1);

            ModelBone modelBonePart;

            if (name.equals("nametag") && nametagEntity != null) {
                modelBonePart = new ModelBoneNametag(pivotPos, name, boneRotation, this, nametagEntity);
            } else if (name.contains("hitbox")) {
                modelBonePart = new ModelBoneHitbox(pivotPos, name, boneRotation, this, masterEntity);
            } else if (name.contains("vfx")) {
                modelBonePart = new ModelBoneVFX(pivotPos, name, boneRotation, this);
            } else {
                modelBonePart = new ModelBonePart(pivotPos, name, boneRotation, this, renderType, masterEntity);
            }

            this.parts.put(name, modelBonePart);
        }

        // Link parents
        for (JsonElement bone : loadedModel.get("minecraft:geometry").getAsJsonArray().get(0).getAsJsonObject().get("bones").getAsJsonArray()) {
            String name = bone.getAsJsonObject().get("name").getAsString();
            JsonElement parent = bone.getAsJsonObject().get("parent");
            String parentString = parent == null ? null : parent.getAsString();

            if (parentString != null) {
                ModelBone child = this.parts.get(name);
                ModelBone parentBone = this.parts.get(parentString);
                child.setParent(parentBone);
                parentBone.addChild(child);
            }
        }

        for (ModelBone modelBonePart : this.parts.values()) {
            modelBonePart.spawn(instance, this.position);

            if (modelBonePart instanceof ModelBonePart bonePart)
                viewableBones.add(bonePart);
            else if (modelBonePart instanceof ModelBoneHitbox hitbox)
                hittableBones.add(hitbox);
            else if (modelBonePart instanceof ModelBoneVFX vfx)
                VFXBones.put(vfx.getName(), vfx);
        }

        drawBones((short) 0);
    }

    public void setPosition(Point pos) {
        this.position = pos;
    }

    public void setGlobalRotation(double rotation) {
        this.globalRotation = rotation;
    }

    public void setState(String state) {
        for (ModelBonePart part : viewableBones) {
            part.setState(state);
        }
    }

    public ModelBone getPart(String boneName) {
        return this.parts.get(boneName);
    }

    public void drawBones(short tick) {
        for (ModelBone modelBonePart : this.parts.values()) {
            if (modelBonePart.getParent() == null)
                modelBonePart.draw(tick);
        }
    }

    public void destroy() {
        for (ModelBone modelBonePart : this.parts.values()) {
            modelBonePart.destroy();
        }

        this.viewableBones.clear();
        this.hittableBones.clear();
        this.VFXBones.clear();
        this.parts.clear();
    }

    public void removeHitboxes() {
        hittableBones.forEach(ModelBoneGeneric::destroy);
        hittableBones.clear();
    }

    @Override
    public Point getVFX(String name) {
        var found = VFXBones.get(name);
        if (found == null) return null;
        return found.getPosition();
    }
}

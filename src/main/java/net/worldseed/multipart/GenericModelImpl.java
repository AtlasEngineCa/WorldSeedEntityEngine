package net.worldseed.multipart;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.instance.Instance;
import net.worldseed.multipart.animations.AnimationLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class GenericModelImpl implements GenericModel {
    private final HashMap<String, ModelBone> parts = new HashMap<>();
    private final Set<ModelBoneGeneric> viewableBones = new HashSet<>();
    private final Set<ModelBoneHitbox> hittableBones = new HashSet<>();
    private final HashMap<String, ModelBoneVFX> VFXBones = new HashMap<>();

    private ModelEngine.RenderType renderType;

    private ModelBoneSeat seat;
    private ModelBoneHead head;
    private ModelBoneNametag nametag;

    private Point position;
    private double globalRotation;
    private Instance instance;

    @Override
    public double getGlobalRotation() {
        return globalRotation;
    }

    @Override
    public ModelEngine.RenderType getRenderType() {
        return renderType;
    }

    @Override
    public Point getPosition() {
        return position;
    }

    public void init(@Nullable Instance instance, @NotNull Pos position, ModelEngine.RenderType renderType) {
        init(instance, position, renderType, null);
    }

    public void init(@Nullable Instance instance, @NotNull Pos position, ModelEngine.RenderType renderType, LivingEntity masterEntity) {
        this.renderType = renderType;
        this.instance = instance;

        JsonObject loadedModel = AnimationLoader.loadModel(getId());
        this.position = new Vec(position.x(), position.y(), position.z());
        this.setGlobalRotation(position.yaw());

        // Build bones
        for (JsonElement bone : loadedModel.get("minecraft:geometry").getAsJsonArray().get(0).getAsJsonObject().get("bones").getAsJsonArray()) {
            JsonElement pivot = bone.getAsJsonObject().get("pivot");
            String name = bone.getAsJsonObject().get("name").getAsString();

            Point boneRotation = ModelEngine.getPos(bone.getAsJsonObject().get("rotation")).orElse(Pos.ZERO).mul(-1, -1, 1);
            Point pivotPos = ModelEngine.getPos(pivot).orElse(Pos.ZERO).mul(-1,1,1);

            ModelBone modelBonePart;

            if (name.equals("nametag")) {
                this.nametag = new ModelBoneNametag(pivotPos, name, boneRotation, this, null);
                modelBonePart = nametag;
            } else if (name.contains("hitbox")) {
                modelBonePart = new ModelBoneHitbox(pivotPos, name, boneRotation, this, masterEntity);
            } else if (name.contains("vfx")) {
                modelBonePart = new ModelBoneVFX(pivotPos, name, boneRotation, this);
            } else if (name.contains("seat")) {
                modelBonePart = new ModelBoneSeat(pivotPos, name, boneRotation, this, masterEntity);
                this.seat = (ModelBoneSeat) modelBonePart;
            } else if (name.equals("head")) {
                if (renderType == ModelEngine.RenderType.ARMOUR_STAND || renderType == ModelEngine.RenderType.SMALL_ARMOUR_STAND) {
                    modelBonePart = new ModelBoneHeadArmourStand(pivotPos, name, boneRotation, this, renderType, masterEntity);
                } else {
                    modelBonePart = new ModelBoneHeadZombie(pivotPos, name, boneRotation, this, renderType, masterEntity);
                }
                this.head = (ModelBoneHead) modelBonePart;
            } else {
                if (renderType == ModelEngine.RenderType.ARMOUR_STAND || renderType == ModelEngine.RenderType.SMALL_ARMOUR_STAND) {
                    modelBonePart = new ModelBonePartArmourStand(pivotPos, name, boneRotation, this, renderType, masterEntity);
                } else {
                    modelBonePart = new ModelBonePartZombie(pivotPos, name, boneRotation, this, renderType, masterEntity);
                }
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

            if (modelBonePart instanceof ModelBonePartArmourStand bonePart)
                viewableBones.add(bonePart);
            else if (modelBonePart instanceof ModelBonePartZombie bonePart)
                viewableBones.add(bonePart);
            else if (modelBonePart instanceof ModelBoneHitbox hitbox)
                hittableBones.add(hitbox);
            else if (modelBonePart instanceof ModelBoneVFX vfx)
                VFXBones.put(vfx.getName(), vfx);
        }

        drawBones();
        setState("normal");
    }

    public void setNametagEntity(LivingEntity entity) {
        if (this.nametag != null) this.nametag.linkEntity(entity);
    }

    public LivingEntity getNametagEntity() {
        if (this.nametag != null) return this.nametag.stand;
        return null;
    }

    public void setPosition(Point pos) {
        this.position = pos;
    }

    public void setGlobalRotation(double rotation) {
        this.globalRotation = rotation;
    }

    public Instance getInstance() {
        return instance;
    }

    public void mountEntity(Entity entity) {
        if (this.seat != null) {
            this.seat.getEntity().addPassenger(entity);
        }
    }

    public void dismountEntity(Entity e) {
        if (this.seat != null)
            this.seat.getEntity().removePassenger(e);
    }

    public Set<Entity> getPassengers() {
        if (this.seat == null || this.seat.getEntity() == null) return Set.of();
        return this.seat.getEntity().getPassengers();
    }

    public void setState(String state) {
        for (ModelBoneGeneric part : viewableBones) {
            part.setState(state);
        }
    }

    public ModelBone getPart(String boneName) {
        return this.parts.get(boneName);
    }

    public ModelBone getSeat() {
        return this.seat;
    }

    public void drawBones() {
        for (ModelBone modelBonePart : this.parts.values()) {
            if (modelBonePart.getParent() == null)
                modelBonePart.draw();
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
        ModelBoneVFX found = VFXBones.get(name);
        if (found == null) return null;
        return found.getPosition();
    }

    @Override
    public void setHeadRotation(double rotation) {
        if (this.head != null) this.head.setRotation(rotation);
    }

    public List<Entity> getParts() {
        return this.parts.values().stream().map(ModelBone::getEntity).filter(Objects::nonNull).toList();
    }

    @Override
    public Point getBoneAtTime(String animation, String boneName, int time) {
        var bone = this.parts.get(boneName);

        Point p = bone.getOffset();
        p = bone.simulateTransform(p, animation, time);
        p = bone.calculateRotation(p, new Vec(0, getGlobalRotation(), 0), getPivot());

        if (this.renderType == ModelEngine.RenderType.ARMOUR_STAND || this.renderType == ModelEngine.RenderType.ZOMBIE) {
            return p.div(6.4, 6.4, 6.4)
                    .add(getPosition())
                    .add(getGlobalOffset());
        } else {
            return p.div(6.4, 6.4, 6.4)
                    .div(1.426)
                    .add(getPosition())
                    .add(getGlobalOffset());
        }
    }
}

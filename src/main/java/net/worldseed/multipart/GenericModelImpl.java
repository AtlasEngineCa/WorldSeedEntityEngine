package net.worldseed.multipart;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.instance.Instance;
import net.worldseed.multipart.model_bones.ModelBone;
import net.worldseed.multipart.model_bones.ModelBoneImpl;
import net.worldseed.multipart.model_bones.ModelBoneHead;
import net.worldseed.multipart.model_bones.ModelBoneViewable;
import net.worldseed.multipart.model_bones.armour_stand.ModelBoneHeadArmourStand;
import net.worldseed.multipart.model_bones.armour_stand.ModelBoneHeadArmourStandHand;
import net.worldseed.multipart.model_bones.armour_stand.ModelBonePartArmourStand;
import net.worldseed.multipart.model_bones.armour_stand.ModelBonePartArmourStandHand;
import net.worldseed.multipart.model_bones.misc.*;
import net.worldseed.multipart.model_bones.zombie.ModelBoneHeadZombie;
import net.worldseed.multipart.model_bones.zombie.ModelBonePartZombie;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class GenericModelImpl implements GenericModel {
    protected final LinkedHashMap<String, ModelBone> parts = new LinkedHashMap<>();
    protected final Set<ModelBoneImpl> viewableBones = new LinkedHashSet<>();
    protected final Set<ModelBoneHitbox> hittableBones = new LinkedHashSet<>();
    protected final Map<String, ModelBoneVFX> VFXBones = new LinkedHashMap<>();

    protected ModelConfig config;

    private ModelBoneSeat seat;
    private ModelBoneHead head;
    private ModelBoneNametag nametag;

    private Point position;
    private double globalRotation;
    protected Instance instance;

    @Override
    public double getGlobalRotation() {
        return globalRotation;
    }

    @Override
    public ModelConfig config() {
        return config;
    }

    @Override
    public Point getPosition() {
        return position;
    }

    public void init(@Nullable Instance instance, @NotNull Pos position, ModelConfig config) {
        init(instance, position, config, null);
    }

    public void init(@Nullable Instance instance, @NotNull Pos position, ModelConfig config, LivingEntity masterEntity) {
        this.config = config;
        this.instance = instance;
        this.position = position;

        JsonObject loadedModel = ModelLoader.loadModel(getId());
        this.setGlobalRotation(position.yaw());

        loadBones(loadedModel, masterEntity);

        for (ModelBone modelBonePart : this.parts.values()) {
            if (modelBonePart instanceof ModelBoneViewable)
                viewableBones.add((ModelBoneImpl) modelBonePart);
            else if (modelBonePart instanceof ModelBoneHitbox hitbox)
                hittableBones.add(hitbox);
            else if (modelBonePart instanceof ModelBoneVFX vfx)
                VFXBones.put(vfx.getName(), vfx);

            modelBonePart.spawn(instance, modelBonePart.calculatePosition()).join();
        }

        draw();
        draw();
        draw();

        this.setState("normal");
    }

    protected void loadBones(JsonObject loadedModel, LivingEntity masterEntity) {
        Map<ModelBoneLoader, String> boneLoaders = new LinkedHashMap<>();
        ModelBoneLoaderGlobal globalLoader = null;

        // Build bones
        for (JsonElement bone : loadedModel.get("minecraft:geometry").getAsJsonArray().get(0).getAsJsonObject().get("bones").getAsJsonArray()) {
            JsonElement pivot = bone.getAsJsonObject().get("pivot");
            String name = bone.getAsJsonObject().get("name").getAsString();

            Point boneRotation = ModelEngine.getPos(bone.getAsJsonObject().get("rotation")).orElse(Pos.ZERO).mul(-1, -1, 1);
            Point pivotPos = ModelEngine.getPos(pivot).orElse(Pos.ZERO).mul(-1,1,1);

            ModelBone modelBonePart = null;

            if (name.equals("nametag")) {
                this.nametag = new ModelBoneNametag(pivotPos, name, boneRotation, this, null);
                modelBonePart = nametag;
            } else if (name.contains("hitbox")) {
                modelBonePart = new ModelBoneHitbox(pivotPos, name, boneRotation, this, masterEntity);
            } else if (name.contains("globalloader")) {
                if (config.modelType() == ModelConfig.ModelType.ZOMBIE) {
                    globalLoader = new ModelBoneLoaderGlobal(pivotPos, name, boneRotation, this, masterEntity);
                    modelBonePart = globalLoader;
                }
            } else if (name.contains("loader")) {
                if (config.modelType() == ModelConfig.ModelType.ZOMBIE) {
                    String[] splitName = name.split("_");

                    if (splitName.length > 2) {
                        modelBonePart = new ModelBoneLoader(pivotPos, name, boneRotation, this, masterEntity);
                        boneLoaders.put((ModelBoneLoader) modelBonePart, String.join("_", List.of(splitName).subList(2, splitName.length)));
                    }
                }
            } else if (name.contains("vfx")) {
                modelBonePart = new ModelBoneVFX(pivotPos, name, boneRotation, this);
            } else if (name.contains("seat")) {
                modelBonePart = new ModelBoneSeat(pivotPos, name, boneRotation, this, masterEntity);
                this.seat = (ModelBoneSeat) modelBonePart;
            } else if (name.equals("head")) {
                if (config.modelType() == ModelConfig.ModelType.ARMOUR_STAND) {
                    if (config.itemSlot() == ModelConfig.ItemSlot.HEAD) {
                        modelBonePart = new ModelBoneHeadArmourStand(pivotPos, name, boneRotation, this, config, masterEntity);
                    } else {
                        modelBonePart = new ModelBoneHeadArmourStandHand(pivotPos, name, boneRotation, this, config, masterEntity);
                    }
                } else {
                    modelBonePart = new ModelBoneHeadZombie(pivotPos, name, boneRotation, this, config, masterEntity);
                }
                this.head = (ModelBoneHead) modelBonePart;
            } else {
                if (config.modelType() == ModelConfig.ModelType.ARMOUR_STAND) {
                    if (config.itemSlot() == ModelConfig.ItemSlot.HEAD) {
                        modelBonePart = new ModelBonePartArmourStand(pivotPos, name, boneRotation, this, config, masterEntity);
                    } else {
                        modelBonePart = new ModelBonePartArmourStandHand(pivotPos, name, boneRotation, this, config, masterEntity);
                    }
                } else {
                    modelBonePart = new ModelBonePartZombie(pivotPos, name, boneRotation, this, config, masterEntity);
                }
            }

            if (modelBonePart != null) this.parts.put(name, modelBonePart);
        }

        // Link parents
        for (JsonElement bone : loadedModel.get("minecraft:geometry").getAsJsonArray().get(0).getAsJsonObject().get("bones").getAsJsonArray()) {
            String name = bone.getAsJsonObject().get("name").getAsString();
            JsonElement parent = bone.getAsJsonObject().get("parent");
            String parentString = parent == null ? null : parent.getAsString();

            if (parentString != null) {
                ModelBone child = this.parts.get(name);

                if (child == null) continue;
                ModelBone parentBone = this.parts.get(parentString);
                child.setParent(parentBone);
                parentBone.addChild(child);
            }
        }

        for (var loader : boneLoaders.entrySet()) {
            loader.getKey().setLoading(this.parts.get(loader.getValue()).getEntity());
        }

        if (globalLoader != null) {
            for (var bone : this.parts.values()) {
                if (bone.getEntity() != null) {
                    if (bone.getEntity().getEntityType().equals(EntityType.ZOMBIE)) {
                        globalLoader.addLoading(bone.getEntity());
                    }
                }
            }
        }
    }

    public void setNametagEntity(LivingEntity entity) {
        if (this.nametag != null) this.nametag.linkEntity(entity);
    }

    public LivingEntity getNametagEntity() {
        if (this.nametag != null) return this.nametag.getStand();
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
        for (ModelBoneImpl part : viewableBones) {
            part.setState(state);
        }
    }

    public ModelBone getPart(String boneName) {
        return this.parts.get(boneName);
    }

    public ModelBone getSeat() {
        return this.seat;
    }

    public void draw() {
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
        hittableBones.forEach(ModelBoneImpl::destroy);
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

        if (config.size() == ModelConfig.Size.NORMAL) {
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

    public Pos getPivot() {
        return Pos.ZERO;
    }
    public Pos getGlobalOffset() {
        return Pos.ZERO;
    }

    public Point getDiff(String boneName) {
        return ModelEngine.diffMappings.get(getId() + "/" + boneName);
    }

    public Point getOffset(String boneName) {
        return ModelEngine.offsetMappings.get(getId() + "/" + boneName);
    }
}
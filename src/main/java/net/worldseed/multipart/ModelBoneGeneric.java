package net.worldseed.multipart;

import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.worldseed.multipart.animations.AnimationLoader.AnimationType;
import net.worldseed.multipart.animations.ModelAnimation;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

abstract sealed class ModelBoneGeneric implements ModelBone permits ModelBoneHitbox, ModelBoneNametag, ModelBonePartArmourStand, ModelBonePartZombie, ModelBoneSeat, ModelBoneVFX {
    protected final HashMap<String, ItemStack> items;
    final Point diff;
    final Point pivot;
    private final String name;

    final List<ModelAnimation> allAnimations = new ArrayList<>();

    Point offset;
    Point rotation;
    ModelBone parent;
    LivingEntity stand;

    final ArrayList<ModelBone> children = new ArrayList<>();
    final GenericModel model;

    @Override
    public Entity getEntity() {
        return stand;
    }

    @Override
    public ModelBone getParent() {
        return parent;
    }

    @Override
    public void setParent(ModelBone parent) {
        this.parent = parent;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public ModelBoneGeneric(Point pivot, String name, Point rotation, GenericModel model) {
        this.name = name;
        this.rotation = rotation;
        this.model = model;

        this.diff = ModelEngine.diffMappings.get(model.getId() + "/" + name);
        this.offset = ModelEngine.offsetMappings.get(model.getId() + "/" + name);

        if (this.diff != null)
            this.pivot = pivot.add(this.diff);
        else
            this.pivot = pivot;

        this.items = ModelEngine.getItems(model.getId(), name);
    }

    public Point calculateGlobalRotation(Point endPos) {
        return calculateRotation(endPos, new Vec(0, model.getGlobalRotation(), 0), this.model.getPivot());
    }

    public Point calculateRotation(Point p, Point rotation, Point pivot) {
        Point position = p.sub(pivot);
        return ModelMath.rotate(position, rotation).add(pivot);
    }

    @Override
    public Point simulateTransform(Point p, String animation, int time) {
        Point endPos = p;

        if (this.diff != null)
            endPos = calculateRotation(endPos, this.simulateRotation(animation, time), this.pivot.sub(this.diff));
        else
            endPos = calculateRotation(endPos, this.simulateRotation(animation, time), this.pivot);

        for (ModelAnimation currentAnimation : this.allAnimations) {
            if (currentAnimation == null || !currentAnimation.name().equals(animation)) continue;

            if (currentAnimation.getType() == AnimationType.TRANSLATION) {
                var calculatedTransform = currentAnimation.getTransformAtTime(time);
                endPos = endPos.add(calculatedTransform);
            }
        }

        if (this.parent != null) {
            endPos = parent.simulateTransform(endPos, animation, time);
        }

        return endPos;
    }

    public Point applyTransform(Point p) {
        Point endPos = p;

        if (this.diff != null)
            endPos = calculateRotation(endPos, this.getRotation(), this.pivot.sub(this.diff));
        else
            endPos = calculateRotation(endPos, this.getRotation(), this.pivot);

        for (ModelAnimation currentAnimation : this.allAnimations) {
            if (currentAnimation != null && currentAnimation.isPlaying()) {
                if (currentAnimation.getType() == AnimationType.TRANSLATION) {
                    var calculatedTransform = currentAnimation.getTransform();
                    endPos = endPos.add(calculatedTransform);
                }
            }
        }

        if (this.parent != null) {
            endPos = parent.applyTransform(endPos);
        }

        return endPos;
    }

    public Point getRotation() {
        Point netTransform = Vec.ZERO;

        for (ModelAnimation currentAnimation : this.allAnimations) {
            if (currentAnimation != null && currentAnimation.isPlaying()) {
                if (currentAnimation.getType() == AnimationType.ROTATION) {
                    Point calculatedTransform = currentAnimation.getTransform();
                    netTransform = netTransform.add(calculatedTransform);
                }
            }
        }

        return this.rotation.add(netTransform);
    }

    public Point simulateRotation(String animation, int time) {
        Point netTransform = Vec.ZERO;

        for (ModelAnimation currentAnimation : this.allAnimations) {
            if (currentAnimation == null || !currentAnimation.name().equals(animation)) continue;

            if (currentAnimation.getType() == AnimationType.ROTATION) {
                Point calculatedTransform = currentAnimation.getTransformAtTime(time);
                netTransform = netTransform.add(calculatedTransform);
            }
        }

        return this.rotation.add(netTransform);
    }

    public Quaternion calculateFinalAngle(Quaternion q) {
        if (this.parent != null) {
            Quaternion pq = parent.calculateFinalAngle(new Quaternion(parent.getRotation()));
            q = pq.multiply(q);
        }

        return q;
    }

    public void addAnimation(ModelAnimation animation) {
        this.allAnimations.add(animation);
    }
    public void addChild(ModelBone child) {
        this.children.add(child);
    }

    @Override
    public void destroy() {
        if (this.stand != null) {
            this.stand.setInvisible(true);
            this.stand.remove();
        }

        this.children.clear();
    }

    @Override
    public Point getOffset() {
        return this.offset;
    }
}

package net.worldseed.multipart.model_bones;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.worldseed.multipart.GenericModel;
import net.worldseed.multipart.ModelEngine;
import net.worldseed.multipart.ModelLoader.AnimationType;
import net.worldseed.multipart.ModelMath;
import net.worldseed.multipart.Quaternion;
import net.worldseed.multipart.animations.BoneAnimation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public abstract class ModelBoneImpl implements ModelBone {
    protected final HashMap<String, ItemStack> items;
    protected final Point pivot;
    protected final String name;
    protected final List<BoneAnimation> allAnimations = new ArrayList<>();
    protected final ArrayList<ModelBone> children = new ArrayList<>();
    protected final GenericModel model;
    protected Point diff;
    protected float scale;
    protected Point offset;
    protected Point rotation;
    protected BoneEntity stand;
    private ModelBone parent;

    public ModelBoneImpl(Point pivot, String name, Point rotation, GenericModel model, float scale) {
        this.name = name;
        this.rotation = rotation;
        this.model = model;

        this.diff = model.getDiff(name);
        this.offset = model.getOffset(name);

        if (this.diff != null) this.pivot = pivot.add(this.diff);
        else this.pivot = pivot;

        this.items = ModelEngine.getItems(model.getId(), name);
        this.scale = scale;
    }

    @Override
    public BoneEntity getEntity() {
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

    @Override
    public void setGlobalScale(float scale) {
        this.scale = scale;
    }

    public Point calculateGlobalRotation(Point endPos) {
        return calculateRotation(endPos, new Vec(0, 180 - model.getGlobalRotation(), 0), this.model.getPivot());
    }

    public Point calculateRotation(Point p, Point rotation, Point pivot) {
        Point position = p.sub(pivot);
        return ModelMath.rotate(position, rotation).add(pivot);
    }

    @Override
    public Point calculateScale(Point p, Point scale, Point pivot) {
        Point position = p.sub(pivot);
        return position.mul(scale).add(pivot);
    }

    public Point applyTransform(Point p) {
        Point endPos = p;

        if (this.diff != null) {
            endPos = calculateScale(endPos, this.getPropogatedScale(), this.pivot.sub(this.diff));
            endPos = calculateRotation(endPos, this.getPropogatedRotation(), this.pivot.sub(this.diff));
        } else {
            endPos = calculateScale(endPos, this.getPropogatedScale(), this.pivot);
            endPos = calculateRotation(endPos, this.getPropogatedRotation(), this.pivot);
        }

        for (BoneAnimation currentAnimation : this.allAnimations) {
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

    public Point getPropogatedRotation() {
        Point netTransform = Vec.ZERO;

        for (BoneAnimation currentAnimation : this.allAnimations) {
            if (currentAnimation != null && currentAnimation.isPlaying()) {
                if (currentAnimation.getType() == AnimationType.ROTATION) {
                    Point calculatedTransform = currentAnimation.getTransform();
                    netTransform = netTransform.add(calculatedTransform);
                }
            }
        }

        return this.rotation.add(netTransform);
    }

    @Override
    public Point getPropogatedScale() {
        Point netTransform = Vec.ONE;

        for (BoneAnimation currentAnimation : this.allAnimations) {
            if (currentAnimation != null && currentAnimation.isPlaying()) {
                if (currentAnimation.getType() == AnimationType.SCALE) {
                    Point calculatedTransform = currentAnimation.getTransform();
                    netTransform = netTransform.mul(calculatedTransform);
                }
            }
        }

        return netTransform;
    }

    @Override
    public Point calculateFinalScale(Point q) {
        if (this.parent != null) {
            Point pq = parent.calculateFinalScale(parent.getPropogatedScale());
            q = pq.mul(q);
        }

        return q;
    }

    public Quaternion calculateFinalAngle(Quaternion q) {
        if (this.parent != null) {
            Quaternion pq = parent.calculateFinalAngle(new Quaternion(parent.getPropogatedRotation()));
            q = pq.multiply(q);
        }

        return q;
    }

    public void addAnimation(BoneAnimation animation) {
        this.allAnimations.add(animation);
    }

    public void addChild(ModelBone child) {
        this.children.add(child);
    }

    @Override
    public void destroy() {
        this.children.forEach(ModelBone::destroy);
        this.children.clear();

        if (this.stand != null) {
            this.stand.remove();
        }
    }

    public CompletableFuture<Void> spawn(Instance instance, Pos position) {
        if (this.offset != null && this.stand != null) {
            this.stand.setNoGravity(true);
            this.stand.setSilent(true);
            return this.stand.setInstance(instance, position);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public Point getOffset() {
        return this.offset;
    }

    public abstract Pos calculatePosition();

    public abstract Point calculateRotation();

    public abstract Point calculateScale();
}

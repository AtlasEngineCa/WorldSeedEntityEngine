package net.worldseed.multipart;

import net.worldseed.multipart.animations.AnimationLoader.AnimationType;
import net.worldseed.multipart.animations.ModelAnimation;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

abstract sealed class ModelBoneGeneric implements ModelBone permits ModelBoneHitbox, ModelBoneNametag, ModelBonePart, ModelBoneVFX {
    protected final HashMap<String, ItemStack> items;
    private final Pos diff;
    private final Point pivot;
    private final String name;

    final List<ModelAnimation> allAnimations = new ArrayList<>();

    Point offset;
    Point rotation;
    ModelBone parent;
    LivingEntity stand;

    final ArrayList<ModelBone> children = new ArrayList<>();
    final GenericModel model;

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

    public Point applyGlobalRotation(Point endPos) {
        return applyRotation(endPos, new Pos(0, model.getGlobalRotation(), 0), this.model.getPivot());
    }

    public Point applyRotation(Point p, Point rotation, Point pivot) {
        Point position = p.sub(pivot);
        return ModelMath.rotate(position, rotation).add(pivot);
    }

    public Point applyTransform(Point p, short tick) {
        Point endPos = p;

        if (this.diff != null)
            endPos = applyRotation(endPos, this.getRotation(tick), this.pivot.sub(this.diff));
        else
            endPos = applyRotation(endPos, this.getRotation(tick), this.pivot);

        for (ModelAnimation currentAnimation : this.allAnimations) {
            if (currentAnimation != null && currentAnimation.isPlaying()) {
                if (currentAnimation.getType() == AnimationType.TRANSLATION) {
                    var calculatedTransform = currentAnimation.getTransform(tick);
                    endPos = endPos.add(calculatedTransform);
                }
            }
        }

        if (this.parent != null) {
            endPos = parent.applyTransform(endPos, tick);
        }

        return endPos;
    }

    public Point getRotation(short tick) {
        Pos netTransform = new Pos(0, 0, 0);

        for (ModelAnimation currentAnimation : this.allAnimations) {
            if (currentAnimation != null && currentAnimation.isPlaying()) {
                if (currentAnimation.getType() == AnimationType.ROTATION) {
                    Point calculatedTransform = currentAnimation.getTransform(tick);
                    netTransform = netTransform.add(calculatedTransform);
                }
            }
        }

        return this.rotation.add(netTransform);
    }

    public Quaternion calculateFinalAngle(Quaternion q, short tick) {
        if (this.parent != null) {
            Quaternion pq = parent.calculateFinalAngle(new Quaternion(parent.getRotation(tick)), tick);
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
        allAnimations.forEach(ModelAnimation::destroy);
    }
}

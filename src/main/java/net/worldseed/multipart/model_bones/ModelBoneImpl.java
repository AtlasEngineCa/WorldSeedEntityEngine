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

    // Per-draw memoization of the propagated (local) rotation/scale. These are pure functions of the
    // current animation tick, but were recomputed once per descendant while walking parent chains
    // (O(N*depth) per tick). A monotonic draw-frame counter (bumped once per model draw) means a cache
    // entry can never be falsely reused across ticks; every transform reader runs inside a draw.
    private static volatile long globalDrawFrame = 0;
    public static void beginDrawFrame() { globalDrawFrame++; }
    private long propFrame = -1;
    private Point cachedPropagatedRotation;
    private Point cachedPropagatedScale;
    // per-draw memoized WORLD rotation/scale so draw() doesn't re-walk the parent chain (equivalent to
    // calculateFinalAngle/calculateFinalScale, computed once per bone per draw -> O(N) instead of O(N*depth)).
    private long worldFrame = -1;
    private Quaternion cachedWorldRotation;
    private Point cachedWorldScale;

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
            endPos = calculateScale(endPos, this.getPropagatedScale(), this.pivot.sub(this.diff));
            endPos = calculateRotation(endPos, this.getPropagatedRotation(), this.pivot.sub(this.diff));
        } else {
            endPos = calculateScale(endPos, this.getPropagatedScale(), this.pivot);
            endPos = calculateRotation(endPos, this.getPropagatedRotation(), this.pivot);
        }

        for (BoneAnimation currentAnimation : this.allAnimations) {
            if (currentAnimation != null && currentAnimation.isPlaying()) {
                if (currentAnimation.getType() == AnimationType.TRANSLATION) {
                    var calculatedTransform = currentAnimation.getTransform();
                    endPos = endPos.add(calculatedTransform.mul(currentAnimation.weight())); // blend weight
                }
            }
        }

        if (this.parent != null) {
            endPos = parent.applyTransform(endPos);
        }

        return endPos;
    }

    /** Compute propagated rotation AND scale once per draw frame (single pass over allAnimations). */
    private void computePropagated() {
        if (this.propFrame == globalDrawFrame) return;
        Point rot = Vec.ZERO;
        Point scale = Vec.ONE;
        for (BoneAnimation currentAnimation : this.allAnimations) {
            if (currentAnimation != null && currentAnimation.isPlaying()) {
                AnimationType type = currentAnimation.getType();
                double w = currentAnimation.weight(); // blend weight
                if (type == AnimationType.ROTATION) {
                    rot = rot.add(currentAnimation.getTransform().mul(w));
                } else if (type == AnimationType.SCALE) {
                    Point t = currentAnimation.getTransform();
                    scale = scale.mul(Vec.ONE.add(t.sub(Vec.ONE).mul(w))); // lerp(ONE, t, w)
                }
            }
        }
        this.cachedPropagatedRotation = this.rotation.add(rot);
        this.cachedPropagatedScale = scale;
        this.propFrame = globalDrawFrame;
    }

    public Point getPropagatedRotation() {
        computePropagated();
        return this.cachedPropagatedRotation;
    }

    @Override
    public Point getPropagatedScale() {
        computePropagated();
        return this.cachedPropagatedScale;
    }

    @Override
    public Point calculateFinalScale(Point q) {
        if (this.parent != null) {
            Point pq = parent.calculateFinalScale(parent.getPropagatedScale());
            q = pq.mul(q);
        }

        return q;
    }

    public Quaternion calculateFinalAngle(Quaternion q) {
        if (this.parent != null) {
            Quaternion pq = parent.calculateFinalAngle(new Quaternion(parent.getPropagatedRotation()));
            q = pq.multiply(q);
        }

        return q;
    }

    private void computeWorld() {
        Quaternion localRotation = new Quaternion(getPropagatedRotation());
        Point localScale = getPropagatedScale();
        if (this.parent instanceof ModelBoneImpl p) {
            this.cachedWorldRotation = p.worldRotation().multiply(localRotation);
            this.cachedWorldScale = p.worldScale().mul(localScale);
        } else {
            this.cachedWorldRotation = localRotation;
            this.cachedWorldScale = localScale;
        }
        this.worldFrame = globalDrawFrame;
    }

    /** Memoized world rotation — equals {@code calculateFinalAngle(new Quaternion(getPropagatedRotation()))}. */
    public Quaternion worldRotation() {
        if (this.worldFrame != globalDrawFrame) computeWorld();
        return this.cachedWorldRotation;
    }

    /** Memoized world scale — equals {@code calculateFinalScale(getPropagatedScale())}. */
    public Point worldScale() {
        if (this.worldFrame != globalDrawFrame) computeWorld();
        return this.cachedWorldScale;
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

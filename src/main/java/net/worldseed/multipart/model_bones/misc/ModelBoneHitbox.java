package net.worldseed.multipart.model_bones.misc;

import com.google.gson.JsonArray;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.other.InteractionMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.tag.Tag;
import net.worldseed.multipart.GenericModel;
import net.worldseed.multipart.animations.ModelAnimation;
import net.worldseed.multipart.model_bones.BoneEntity;
import net.worldseed.multipart.model_bones.ModelBone;
import net.worldseed.multipart.model_bones.ModelBoneImpl;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;

public class ModelBoneHitbox extends ModelBoneImpl {
    private final JsonArray cubes;

    Pos actualPosition = Pos.ZERO;
    Collection<ModelBoneHitbox> illegitimateChildren = new ConcurrentLinkedDeque<>();

    public void addViewer(Player player) {
        if (this.stand != null) this.stand.addViewer(player);
        illegitimateChildren.forEach(modelBone -> modelBone.addViewer(player));
    }

    public void removeViewer(Player player) {
        if (this.stand != null) this.stand.removeViewer(player);
        illegitimateChildren.forEach(modelBone -> modelBone.removeViewer(player));
    }

    @Override
    public void setScale(float scale) {
        super.setScale(scale);

        this.destroy();
        this.illegitimateChildren.clear();
        generateStands(this.cubes, this.pivot.mul(scale), this.name, this.rotation.mul(scale), this.model);
        this.illegitimateChildren.forEach(modelBone -> modelBone.spawn(model.getInstance(), model.getPosition()));
    }

    public ModelBoneHitbox(Point pivot, String name, Point rotation, GenericModel model, Point newOffset, double sizeX, double sizeY, JsonArray cubes, boolean parent, float scale) {
        super(pivot, name, rotation, model, scale);

        this.cubes = cubes;

        if (parent) {
            generateStands(cubes, pivot.mul(scale), name, rotation.mul(scale), model);
            this.offset = null;
        } else {
            if (this.offset != null) {
                this.stand = new BoneEntity(EntityType.INTERACTION, model) {
                    @Override
                    public void updateNewViewer(@NotNull Player player) {
                        super.updateNewViewer(player);
                        illegitimateChildren.forEach(modelBone -> modelBone.addViewer(player));
                    }

                    @Override
                    public void updateOldViewer(@NotNull Player player) {
                        super.updateOldViewer(player);
                        illegitimateChildren.forEach(modelBone -> modelBone.removeViewer(player));
                    }
                };

                this.stand.setTag(Tag.String("WSEE"), "hitbox");
                this.offset = newOffset;

                InteractionMeta meta = (InteractionMeta) this.stand.getEntityMeta();
                meta.setHeight((float) (sizeY / 4f));
                meta.setWidth((float) (sizeX / 4f));
            }
        }
    }

    public void generateStands(JsonArray cubes, Point pivotPos, String name, Point boneRotation, GenericModel genericModel) {
        for (var cube : cubes) {
            JsonArray sizeArray = cube.getAsJsonObject().get("size").getAsJsonArray();
            JsonArray p = cube.getAsJsonObject().get("pivot").getAsJsonArray();
            JsonArray origin = cube.getAsJsonObject().get("origin").getAsJsonArray();

            Point sizePoint = new Vec(sizeArray.get(0).getAsFloat(), sizeArray.get(1).getAsFloat(), sizeArray.get(2).getAsFloat()).mul(scale);
            Point pivotPoint = new Vec(p.get(0).getAsFloat(), p.get(1).getAsFloat(), p.get(2).getAsFloat()).mul(scale);
            Point originPoint = new Vec(origin.get(0).getAsFloat(), origin.get(1).getAsFloat(), origin.get(2).getAsFloat()).mul(scale);

            Point originPivotDiff = pivotPoint.sub(originPoint);

            double maxSize = Math.max(Math.min(Math.min(sizePoint.x(), sizePoint.y()), sizePoint.z()), 5);
            while (maxSize > 32) {
                maxSize /= 2;
            }

            // Convert sizePoint in to smaller squares
            for (int x = 0; x < sizePoint.x() / maxSize; ++x) {
                for (int y = 0; y < sizePoint.y() / maxSize; ++y) {
                    for (int z = 0; z < sizePoint.z() / maxSize; ++z) {
                        var relativeSize = new Vec(maxSize, maxSize, maxSize);
                        var relativePivotPoint = new Vec(x * maxSize, y * maxSize, z * maxSize);

                        if ((relativePivotPoint.x() + relativeSize.x()/2) > sizePoint.x()) relativePivotPoint = relativePivotPoint.withX((relativePivotPoint.x()) - sizePoint.x());
                        if ((relativePivotPoint.y() + relativeSize.y()) > sizePoint.y()) relativePivotPoint = relativePivotPoint.sub(0, (relativePivotPoint.y() + relativeSize.y()) - sizePoint.y(), 0);
                        if ((relativePivotPoint.z() + relativeSize.z()/2) > sizePoint.z()) relativePivotPoint = relativePivotPoint.withZ((relativePivotPoint.z()) - sizePoint.z());

                        var newOffset = pivotPoint.mul(-1, 1, 1).sub(sizePoint.x() / 2, originPivotDiff.y(), sizePoint.z() / 2);
                        newOffset = newOffset.add(relativePivotPoint).add(relativeSize.x() / 2, 0, relativeSize.z() / 2);

                        ModelBoneHitbox created = new ModelBoneHitbox(pivotPos, name, boneRotation, genericModel, newOffset, relativeSize.x(), relativeSize.y(), cubes, false, scale);
                        illegitimateChildren.add(created);
                    }
                }
            }
        }
    }

    @Override
    public void setParent(ModelBone parent) {
        super.setParent(parent);
        this.illegitimateChildren.forEach(modelBone -> modelBone.setParent(parent));
    }

    @Override
    public CompletableFuture<Void> spawn(Instance instance, Point position) {
        this.illegitimateChildren.forEach(modelBone -> modelBone.spawn(instance, position));
        return super.spawn(instance, position);
    }

    @Override
    public void addAnimation(ModelAnimation animation) {
        super.addAnimation(animation);
        this.illegitimateChildren.forEach(modelBone -> modelBone.addAnimation(animation));
    }

    @Override
    public void setState(String state) { }

    @Override
    public Pos calculatePosition() {
        if (this.offset == null) return Pos.ZERO;
        Point p = this.offset;

        p = applyTransform(p);
        p = calculateGlobalRotation(p);

        var lp = actualPosition;
        var newPoint = Pos.fromPoint(p).div(4);
        actualPosition = Pos.fromPoint(lp.asVec().lerp(Vec.fromPoint(newPoint), 0.25));

        return lp;
    }

    @Override
    public void destroy() {
        super.destroy();
        illegitimateChildren.forEach(ModelBone::destroy);
    }

    @Override
    public Point calculateRotation() {
        return Vec.ZERO;
    }

    public void draw() {
        if (this.illegitimateChildren.size() > 0) {
            this.children.forEach(ModelBone::draw);
            this.illegitimateChildren.forEach(ModelBone::draw);
        }
        if (this.offset == null) return;

        try {
            stand.teleport(calculatePosition().add(model.getPosition()));
        } catch (Exception ignored) { }
    }
}

package net.worldseed.multipart.model_bones.misc;

import net.kyori.adventure.util.RGBLike;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.worldseed.multipart.GenericModel;
import net.worldseed.multipart.model_bones.ModelBone;
import net.worldseed.multipart.model_bones.ModelBoneImpl;
import net.worldseed.multipart.model_bones.bone_types.VFXBone;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ModelBoneVFX extends ModelBoneImpl implements VFXBone {
    private final List<GenericModel> attached = new ArrayList<>();
    private Pos position = Pos.ZERO;

    public ModelBoneVFX(Point pivot, String name, Point rotation, GenericModel model, float scale) {
        super(pivot, name, rotation, model, scale);
        this.stand = null;
    }

    @Override
    public void attachModel(GenericModel model) {
        attached.add(model);
    }

    @Override
    public List<GenericModel> getAttachedModels() {
        return attached;
    }

    @Override
    public void detachModel(GenericModel model) {
        attached.remove(model);
    }

    @Override
    public void setGlobalRotation(double yaw, double pitch) {

    }

    public Point getPosition() {
        return position;
    }

    @Override
    public void setState(String state) {
    }

    public CompletableFuture<Void> spawn(Instance instance, Point position) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public Pos calculatePosition() {
        if (this.offset == null) return Pos.ZERO;

        Point p = this.offset;
        p = applyTransform(p);
        p = calculateGlobalRotation(p);

        Pos endPos = Pos.fromPoint(p);

        return endPos
                .div(4, 4, 4).mul(scale)
                .add(model.getPosition());
    }

    @Override
    public Point calculateRotation() {
        return Vec.ZERO;
    }

    @Override
    public Point calculateScale() {
        return Vec.ZERO;
    }

    public void draw() {
        this.children.forEach(ModelBone::draw);
        if (this.offset == null) return;

        this.position = calculatePosition();

        this.attached.forEach(model -> {
            model.setPosition(this.position);
            model.setGlobalRotation(this.model.getGlobalRotation());
            model.draw();
        });
    }

    @Override
    public void destroy() {
    }

    @Override
    public void addViewer(Player player) {
        this.attached.forEach(model -> model.addViewer(player));
    }

    @Override
    public void removeViewer(Player player) {
        this.attached.forEach(model -> model.removeViewer(player));
    }

    @Override
    public void removeGlowing() {
        this.attached.forEach(GenericModel::removeGlowing);
    }

    @Override
    public void setGlowing(RGBLike color) {
        this.attached.forEach(model -> model.setGlowing(color));
    }

    @Override
    public void removeGlowing(Player player) {
        this.attached.forEach(model -> model.removeGlowing(player));
    }

    @Override
    public void setGlowing(Player player, RGBLike color) {
        this.attached.forEach(model -> model.setGlowing(player, color));
    }

}

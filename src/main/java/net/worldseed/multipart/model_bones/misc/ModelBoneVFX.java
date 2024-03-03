package net.worldseed.multipart.model_bones.misc;

import net.minestom.server.color.Color;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.worldseed.multipart.GenericModel;
import net.worldseed.multipart.model_bones.ModelBone;
import net.worldseed.multipart.model_bones.ModelBoneImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ModelBoneVFX extends ModelBoneImpl {
    private final List<GenericModel> attached = new ArrayList<>();
    private Pos position = Pos.ZERO;

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

    public Point getPosition() {
        return position;
    }

    public ModelBoneVFX(Point pivot, String name, Point rotation, GenericModel model, float scale) {
        super(pivot, name, rotation, model, scale);
        this.stand = null;
    }

    @Override
    public void setState(String state) {}

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
    public void destroy() { }

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
    public void setGlowing(Color color) {
        this.attached.forEach(model -> model.setGlowing(color));
    }

}

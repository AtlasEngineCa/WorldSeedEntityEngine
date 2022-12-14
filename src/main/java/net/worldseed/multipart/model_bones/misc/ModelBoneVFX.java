package net.worldseed.multipart.model_bones.misc;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.Instance;
import net.worldseed.multipart.GenericModel;
import net.worldseed.multipart.ModelConfig;
import net.worldseed.multipart.model_bones.ModelBone;
import net.worldseed.multipart.model_bones.ModelBoneImpl;

import java.util.concurrent.CompletableFuture;

public class ModelBoneVFX extends ModelBoneImpl {
    private Point position = Pos.ZERO;

    public Point getPosition() {
        return position;
    }
    public ModelBoneVFX(Point pivot, String name, Point rotation, GenericModel model) {
        super(pivot, name, rotation, model);
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

        double divisor;
        if (model.config().itemSlot() == ModelConfig.ItemSlot.HEAD) {
            divisor = model.config().size() == ModelConfig.Size.SMALL ? 1.426 : 1;
        } else {
            divisor = model.config().size() == ModelConfig.Size.SMALL ? 1.25 : 0.624;
        }

        return endPos
                .div(6.4, 6.4, 6.4)
                .div(divisor)
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
    }

    @Override
    public void destroy() {
    }
}

package net.worldseed.multipart.model_bones.misc;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.worldseed.multipart.GenericModel;
import net.worldseed.multipart.model_bones.ModelBone;
import net.worldseed.multipart.model_bones.ModelBoneImpl;

import java.util.concurrent.CompletableFuture;

public class ModelBoneVFX extends ModelBoneImpl {

    @Override
    public void addViewer(Player player) {}

    @Override
    public void removeViewer(Player player) { }

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

        return endPos
                .div(4, 4, 4)
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

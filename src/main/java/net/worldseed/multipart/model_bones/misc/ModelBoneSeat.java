package net.worldseed.multipart.model_bones.misc;

import net.minestom.server.color.Color;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.tag.Tag;
import net.worldseed.multipart.GenericModel;
import net.worldseed.multipart.Quaternion;
import net.worldseed.multipart.model_bones.BoneEntity;
import net.worldseed.multipart.model_bones.ModelBone;
import net.worldseed.multipart.model_bones.ModelBoneImpl;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ModelBoneSeat extends ModelBoneImpl {

    @Override
    public void addViewer(Player player) {
        if (this.stand != null) this.stand.addViewer(player);
    }

    @Override
    public void removeViewer(Player player) {
        if (this.stand != null) this.stand.removeViewer(player);
    }

    @Override
    public void removeGlowing() {

    }

    @Override
    public void setGlowing(Color color) {

    }

    @Override
    public void attachModel(GenericModel model) {
        throw new UnsupportedOperationException("Cannot attach a model to a seat");
    }

    @Override
    public List<GenericModel> getAttachedModels() {
        return List.of();
    }

    @Override
    public void detachModel(GenericModel model) {
        throw new UnsupportedOperationException("Cannot detach a model from a seat");
    }

    @Override
    public void setGlobalRotation(double rotation) {

    }

    public ModelBoneSeat(Point pivot, String name, Point rotation, GenericModel model, float scale) {
        super(pivot, name, rotation, model, scale);

        if (this.offset != null) {
            this.stand = new BoneEntity(EntityType.ZOMBIE, model);
            this.stand.setTag(Tag.String("WSEE"), "seat");
            stand.setInvisible(true);
        }
    }

    @Override
    public void setState(String state) { }

    @Override
    public Point getPosition() {
        return calculatePosition();
    }

    public CompletableFuture<Void> spawn(Instance instance, Point position) {
        if (this.offset != null) {
            this.stand.setInvisible(true);
            this.stand.setNoGravity(true);
            this.stand.setSilent(true);
            return this.stand.setInstance(instance, position);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public Pos calculatePosition() {
        if (this.offset == null) return Pos.ZERO;

        var rotation = calculateRotation();

        var p = applyTransform(this.offset);
        p = calculateGlobalRotation(p);
        Pos endPos = Pos.fromPoint(p);

        return endPos
                .div(4, 4, 4).mul(scale)
                .add(model.getPosition())
                .add(model.getGlobalOffset())
                .withView((float) -rotation.y(), (float) rotation.x());
    }

    @Override
    public Point calculateRotation() {
        Quaternion q = new Quaternion(new Vec(0, 180 - this.model.getGlobalRotation(), 0));
        return q.toEulerYZX();
    }

    public void draw() {
        this.children.forEach(ModelBone::draw);
        if (this.offset == null) return;

        Pos found = calculatePosition();

        // TODO: I think this sends two packets?
        stand.setView(found.yaw(), found.pitch());
        stand.teleport(found);
    }
}

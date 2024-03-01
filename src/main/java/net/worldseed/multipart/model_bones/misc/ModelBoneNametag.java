package net.worldseed.multipart.model_bones.misc;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;
import net.worldseed.multipart.GenericModel;
import net.worldseed.multipart.model_bones.BoneEntity;
import net.worldseed.multipart.model_bones.ModelBoneImpl;

public class ModelBoneNametag extends ModelBoneImpl {

    @Override
    public void addViewer(Player player) {
        if (this.stand != null) this.stand.addViewer(player);
    }

    @Override
    public void removeViewer(Player player) {
        if (this.stand != null) this.stand.removeViewer(player);
    }

    public ModelBoneNametag(Point pivot, String name, Point rotation, GenericModel model, BoneEntity nametagEntity, float scale) {
        super(pivot, name, rotation, model, scale);

        if (this.offset != null && nametagEntity != null) {
            this.stand = nametagEntity;
            this.stand.setTag(Tag.String("WSEE"), "nametag");
        }
    }

    @Override
    public void setState(String state) { }

    @Override
    public Point getPosition() {
        return calculatePosition();
    }

    public void linkEntity(BoneEntity entity) {
        this.stand = entity;
        this.stand.setTag(Tag.String("WSEE"), "nametag");
    }

    public void draw() {
        if (this.offset == null) return;
        if (this.stand == null) return;
        stand.teleport(calculatePosition());
    }

    public Entity getStand() {
        return stand;
    }

    @Override
    public Pos calculatePosition() {
        if (this.offset == null) return Pos.ZERO;

        Point p = this.offset;
        p = applyTransform(p);
        p = calculateGlobalRotation(p);

        return Pos.fromPoint(p)
                .div(4, 4, 4).mul(scale)
                .add(model.getPosition())
                .add(model.getGlobalOffset());
    }

    @Override
    public Point calculateRotation() {
        return Vec.ZERO;
    }
}

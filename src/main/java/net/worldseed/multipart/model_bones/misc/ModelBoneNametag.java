package net.worldseed.multipart.model_bones.misc;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.tag.Tag;
import net.worldseed.multipart.GenericModel;
import net.worldseed.multipart.ModelConfig;
import net.worldseed.multipart.model_bones.ModelBoneGeneric;

public class ModelBoneNametag extends ModelBoneGeneric {
    public ModelBoneNametag(Point pivot, String name, Point rotation, GenericModel model, LivingEntity nametagEntity) {
        super(pivot, name, rotation, model);

        if (this.offset != null && nametagEntity != null) {
            this.stand = nametagEntity;
            this.stand.setTag(Tag.String("WSEE"), "nametag");
        }
    }

    @Override
    public void setState(String state) { }

    public void linkEntity(LivingEntity entity) {
        this.stand = entity;
        this.stand.setTag(Tag.String("WSEE"), "nametag");
    }

    public void draw() {
        if (this.offset == null) return;
        if (this.stand == null) return;
        stand.teleport(calculatePosition());
    }

    public LivingEntity getStand() {
        return stand;
    }

    @Override
    public Pos calculatePosition() {
        if (this.offset == null) return Pos.ZERO;

        Point p = this.offset;
        p = applyTransform(p);
        p = calculateGlobalRotation(p);

        double divisor;
        if (model.config().itemSlot() == ModelConfig.ItemSlot.HEAD) {
            divisor = model.config().size() == ModelConfig.Size.SMALL ? 1.426 : 1;
        } else {
            divisor = model.config().size() == ModelConfig.Size.SMALL ? 1.25 : 0.624;
        }

        return Pos.fromPoint(p)
                .div(6.4, 6.4, 6.4)
                .div(divisor)
                .add(model.getPosition())
                .add(model.getGlobalOffset());
    }

    @Override
    public Point calculateRotation() {
        return Vec.ZERO;
    }
}

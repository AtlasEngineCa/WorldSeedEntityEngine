package net.worldseed.multipart;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.instance.Instance;

non-sealed class ModelBoneNametag extends ModelBoneGeneric {
    public ModelBoneNametag(Point pivot, String name, Point rotation, GenericModel model, Entity forwardTo, LivingEntity nametagEntity) {
        super(pivot, name, rotation, model);

        if (this.offset != null) {
            this.stand = nametagEntity;
        }
    }

    @Override
    public void setState(String state) { }

    public void draw(short tick) {
        if (this.offset == null) return;

        Point p = this.offset;
        p = applyTransform(p, tick);
        p = applyGlobalRotation(p);

        Pos endPos = Pos.fromPoint(p)
            .div(6.4, 6.4, 6.4)
            .add(model.getPosition())
            .add(model.getGlobalOffset());

        stand.teleport(endPos);
    }

    @Override
    public void spawn(Instance instance, Point position) {
    }
}

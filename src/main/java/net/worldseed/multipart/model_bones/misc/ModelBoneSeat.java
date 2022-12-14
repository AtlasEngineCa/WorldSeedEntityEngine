package net.worldseed.multipart.model_bones.misc;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.instance.Instance;
import net.minestom.server.tag.Tag;
import net.worldseed.multipart.*;
import net.worldseed.multipart.model_bones.ModelBone;
import net.worldseed.multipart.model_bones.ModelBoneImpl;

import java.util.concurrent.CompletableFuture;

public class ModelBoneSeat extends ModelBoneImpl {
    public ModelBoneSeat(Point pivot, String name, Point rotation, GenericModel model, LivingEntity forwardTo) {
        super(pivot, name, rotation, model);

        if (this.offset != null) {
            this.stand = new LivingEntity(EntityType.ZOMBIE) {
                @Override
                public void tick(long time) {}
            };
            this.stand.setTag(Tag.String("WSEE"), "seat");

            ModelBoneImpl.hookPart(this, forwardTo);
        }
    }

    @Override
    public void setState(String state) {}

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

        double divisor;
        if (model.config().itemSlot() == ModelConfig.ItemSlot.HEAD) {
            divisor = model.config().size() == ModelConfig.Size.SMALL ? 1.426 : 1;
        } else {
            divisor = model.config().size() == ModelConfig.Size.SMALL ? 1.25 : 0.624;
        }

        return endPos
                .div(6.4, 6.4, 6.4)
                .div(divisor)
                .add(model.getPosition())
                .add(model.getGlobalOffset())
                .withView((float) -rotation.y(), (float) rotation.x());
    }

    @Override
    public Point calculateRotation() {
        Quaternion q = new Quaternion(new Vec(0, this.model.getGlobalRotation(), 0));
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

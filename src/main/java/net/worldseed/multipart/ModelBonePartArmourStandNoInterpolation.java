package net.worldseed.multipart;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.entity.damage.EntityDamage;
import net.minestom.server.entity.metadata.other.ArmorStandMeta;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.entity.EntityDamageEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.tag.Tag;
import net.worldseed.multipart.events.EntityControlEvent;
import net.worldseed.multipart.events.EntityDismountEvent;
import net.worldseed.multipart.events.EntityInteractEvent;
import net.worldseed.multipart.mount.MobRidable;

class ModelBonePartArmourStandNoInterpolation extends ModelBonePartArmourStand {
    private Point lastRotation = Vec.ZERO;
    private Point halfRotation = Vec.ZERO;
    private boolean update = true;

    public ModelBonePartArmourStandNoInterpolation(Point pivot, String name, Point rotation, GenericModel model, ModelEngine.RenderType renderType, LivingEntity forwardTo) {
        super(pivot, name, rotation, model, renderType, forwardTo);
    }

    @Override
    void setBoneRotation(Point rotation) {
        ArmorStandMeta meta = (ArmorStandMeta) this.stand.getEntityMeta();

        meta.setHeadRotation(new Vec(
            rotation.x(),
            -rotation.y(),
            -rotation.z()
        ));
    }

    @Override
    public void draw() {
        this.children.forEach(ModelBone::draw);
        if (this.offset == null) return;

        if (update) {
            Point p = this.offset.sub(0, 1.6, 0);
            p = applyTransform(p);
            p = calculateGlobalRotation(p);

            Pos endPos = Pos.fromPoint(p);

            Quaternion q = calculateFinalAngle(new Quaternion(getRotation()));
            if (model.getGlobalRotation() != 0) {
                Quaternion pq = new Quaternion(new Vec(0, this.model.getGlobalRotation(), 0));
                q = pq.multiply(q);
            }

            Pos newPos;
            if (super.model.getRenderType() == ModelEngine.RenderType.ARMOUR_STAND_NO_INTERPOLATION) {
                newPos = endPos
                        .div(6.4, 6.4, 6.4)
                        .add(model.getPosition())
                        .sub(0, 1.4, 0)
                        .add(model.getGlobalOffset());
            } else {
                newPos = endPos
                        .div(6.4, 6.4, 6.4)
                        .div(1.426)
                        .add(model.getPosition())
                        .sub(0, 0.4, 0)
                        .add(model.getGlobalOffset());
            }

            var rotation = q.toEuler();

            Point halfStep = rotation.sub(lastRotation);

            double halfStepX = halfStep.x() % 360;
            double halfStepY = halfStep.y() % 360;
            double halfStepZ = halfStep.z() % 360;

            if (halfStepX > 180) halfStepX -= 360;
            if (halfStepX < -180) halfStepX += 360;
            if (halfStepY > 180) halfStepY -= 360;
            if (halfStepY < -180) halfStepY += 360;
            if (halfStepZ > 180) halfStepZ -= 360;
            if (halfStepZ < -180) halfStepZ += 360;

            double divisor = 2;
            halfRotation = lastRotation.add(new Vec(halfStepX / divisor, halfStepY / divisor, halfStepZ / divisor));

            stand.teleport(newPos);
            setBoneRotation(lastRotation);
            lastRotation = rotation;
        } else {
            setBoneRotation(halfRotation);
        }
        update = !update;
    }
}

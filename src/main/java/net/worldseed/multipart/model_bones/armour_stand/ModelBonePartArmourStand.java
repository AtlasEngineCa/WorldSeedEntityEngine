package net.worldseed.multipart.model_bones.armour_stand;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.metadata.other.ArmorStandMeta;
import net.minestom.server.item.ItemStack;
import net.worldseed.multipart.GenericModel;
import net.worldseed.multipart.ModelConfig;
import net.worldseed.multipart.Quaternion;
import net.worldseed.multipart.model_bones.ModelBone;
import net.worldseed.multipart.model_bones.ModelBoneImpl;
import net.worldseed.multipart.model_bones.ModelBoneViewable;

public class ModelBonePartArmourStand extends ModelBoneImpl implements ModelBoneViewable {
    private Point lastRotation = Vec.ZERO;
    private Point halfRotation = Vec.ZERO;
    private Point newRotation = Vec.ZERO;
    private boolean update = true;

    private final Pos SMALL_SUB = new Pos(0, 0.72, 0);
    private final Pos NORMAL_SUB = new Pos(0, 1.426, 0);

    public ModelBonePartArmourStand(Point pivot, String name, Point rotation, GenericModel model, ModelConfig modelConfig, LivingEntity forwardTo) {
        super(pivot, name, rotation, model);

        if (this.offset != null) {
            this.stand = new LivingEntity(EntityType.ARMOR_STAND) {
                @Override
                public void tick(long time) {}
            };

            ArmorStandMeta meta = (ArmorStandMeta) this.stand.getEntityMeta();

            if (modelConfig.size() == ModelConfig.Size.SMALL)
                meta.setSmall(true);

            meta.setHasNoBasePlate(true);
            ModelBoneImpl.hookPart(this, forwardTo);
        }
    }

    void setBoneRotation(Point rotation) {
        ArmorStandMeta meta = (ArmorStandMeta) this.stand.getEntityMeta();

        if (model.config().interpolationType() == ModelConfig.InterpolationType.Y_INTERPOLATION) {
            meta.setHeadRotation(new Vec(
                    rotation.x(),
                    0,
                    -rotation.z()
            ));
        } else {
            meta.setHeadRotation(new Vec(
                    rotation.x(),
                    -rotation.y(),
                    -rotation.z()
            ));
        }
    }

    @Override
    public Pos calculatePosition() {
        if (this.offset == null) return Pos.ZERO;

        Point p = this.offset;
        p = applyTransform(p);
        p = calculateGlobalRotation(p);

        Pos endPos = Pos.fromPoint(p);
        double divisor = model.config().size() == ModelConfig.Size.SMALL ? 1.426 : 1;

        Pos sub = model.config().size() == ModelConfig.Size.SMALL
                ? SMALL_SUB : NORMAL_SUB;

        Pos newPos = endPos
                .div(6.4, 6.4, 6.4)
                .div(divisor)
                .add(model.getPosition())
                .sub(sub)
                .add(model.getGlobalOffset());

        if (model.config().interpolationType() == ModelConfig.InterpolationType.Y_INTERPOLATION) {
            return newPos.withYaw((float) -newRotation.y());
        } else {
            return newPos;
        }
    }

    @Override
    public Point calculateRotation() {
        Quaternion q = calculateFinalAngle(new Quaternion(getPropogatedRotation()));
        if (model.getGlobalRotation() != 0) {
            Quaternion pq = new Quaternion(new Vec(0, 180 - this.model.getGlobalRotation(), 0));
            q = pq.multiply(q);
        }

        return model.config().interpolationType() == ModelConfig.InterpolationType.Y_INTERPOLATION
                        ? q.toEulerYZX() : q.toEuler();
    }

    public void draw() {
        this.children.forEach(ModelBone::draw);
        if (this.offset == null) return;

        if (update) {
            var rotation = calculateRotation();
            newRotation = rotation;

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

            if (model.config().interpolationType() == ModelConfig.InterpolationType.Y_INTERPOLATION) {
                halfRotation = lastRotation.add(new Vec(halfStepX / 2, 0, halfStepZ / 2));
            } else {
                halfRotation = lastRotation.add(new Vec(halfStepX / 2, halfStepY / 2, halfStepZ / 2));
            }

            stand.teleport(calculatePosition());
            setBoneRotation(lastRotation);
            lastRotation = rotation;
        } else {
            setBoneRotation(halfRotation);
        }

        update = !update;
    }

    @Override
    public void setState(String state) {
        if (this.stand != null && this.stand instanceof LivingEntity e) {
            if (state.equals("invisible")) {
                e.setHelmet(ItemStack.AIR);
                return;
            }

            var item = this.items.get(state);

            if (item != null) {
                e.setHelmet(item);
            }
        }
    }
}

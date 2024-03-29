package net.worldseed.multipart.model_bones.armour_stand;

import net.kyori.adventure.util.RGBLike;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.other.ArmorStandMeta;
import net.minestom.server.item.ItemStack;
import net.worldseed.multipart.GenericModel;
import net.worldseed.multipart.Quaternion;
import net.worldseed.multipart.model_bones.BoneEntity;
import net.worldseed.multipart.model_bones.ModelBone;
import net.worldseed.multipart.model_bones.ModelBoneImpl;
import net.worldseed.multipart.model_bones.ModelBoneViewable;

import java.util.List;

public class ModelBonePartArmourStandHand extends ModelBoneImpl implements ModelBoneViewable {
    private static final Pos SMALL_SUB = new Pos(0, 0.66, 0);
    private static final Pos NORMAL_SUB = new Pos(0, 1.377, 0);
    private Point lastRotation = Vec.ZERO;
    private Point halfRotation = Vec.ZERO;
    private boolean update = true;

    public ModelBonePartArmourStandHand(Point pivot, String name, Point rotation, GenericModel model, float scale) {
        super(pivot, name, rotation, model, scale);

        if (this.offset != null) {
            this.stand = new BoneEntity(EntityType.ARMOR_STAND, model);

            ArmorStandMeta meta = (ArmorStandMeta) this.stand.getEntityMeta();
            stand.setInvisible(true);

            meta.setHasNoBasePlate(true);
        }
    }

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
        if (this.stand != null) this.stand.setGlowing(false);
    }

    @Override
    public void setGlowing(RGBLike color) {
        if (this.stand != null) this.stand.setGlowing(true);
    }

    @Override
    public void removeGlowing(Player player) {

    }

    @Override
    public void setGlowing(Player player, RGBLike color) {

    }

    @Override
    public void attachModel(GenericModel model) {
        throw new UnsupportedOperationException("Cannot attach a model to this bone type");
    }

    @Override
    public List<GenericModel> getAttachedModels() {
        return List.of();
    }

    @Override
    public void detachModel(GenericModel model) {
        throw new UnsupportedOperationException("Cannot detach a model from this bone type");
    }

    @Override
    public void setGlobalRotation(double rotation) {

    }

    protected void setBoneRotation(Point rotation) {
        ArmorStandMeta meta = (ArmorStandMeta) this.stand.getEntityMeta();

        meta.setRightArmRotation(new Vec(
                rotation.x(),
                -rotation.y(),
                -rotation.z()
        ));
    }

    @Override
    public Pos calculatePosition() {
        if (this.offset == null) return Pos.ZERO;

        var p = applyTransform(this.offset);
        p = calculateGlobalRotation(p);
        Pos endPos = Pos.fromPoint(p);

        double divisor = 0.624;

        return endPos
                .div(6.4, 6.4, 6.4)
                .div(divisor)
                .sub(NORMAL_SUB)
                .mul(scale)
                .add(model.getPosition())
                .add(model.getGlobalOffset());
    }

    @Override
    public Point calculateRotation() {
        Quaternion q = calculateFinalAngle(new Quaternion(getPropogatedRotation()));
        Quaternion pq = new Quaternion(new Vec(0, 180 - this.model.getGlobalRotation(), 0));
        q = pq.multiply(q);

        return q.toEuler();
    }

    @Override
    public Point calculateScale() {
        return Vec.ZERO;
    }

    public void draw() {
        this.children.forEach(ModelBone::draw);
        if (this.offset == null) return;

        if (update) {
            Point rotation = calculateRotation();
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

            halfRotation = lastRotation.add(new Vec(halfStepX / 2, halfStepY / 2, halfStepZ / 2));

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
        if (this.stand != null) {
            if (state.equals("invisible")) {
                stand.setItemInMainHand(ItemStack.AIR);
                return;
            }

            var item = this.items.get(state);

            if (item != null) {
                stand.setItemInMainHand(item);
            }
        }
    }

    @Override
    public Point getPosition() {
        return calculatePosition();
    }
}

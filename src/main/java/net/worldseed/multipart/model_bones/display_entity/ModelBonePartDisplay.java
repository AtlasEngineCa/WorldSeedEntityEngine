package net.worldseed.multipart.model_bones.display_entity;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.item.ItemStack;
import net.worldseed.multipart.GenericModel;
import net.worldseed.multipart.ModelConfig;
import net.worldseed.multipart.Quaternion;
import net.worldseed.multipart.model_bones.ModelBone;
import net.worldseed.multipart.model_bones.ModelBoneImpl;
import net.worldseed.multipart.model_bones.ModelBoneViewable;

public class ModelBonePartDisplay extends ModelBoneImpl implements ModelBoneViewable {
    int sendTick = 0;

    public ModelBonePartDisplay(Point pivot, String name, Point rotation, GenericModel model, ModelConfig config, LivingEntity forwardTo) {
        super(pivot, name, rotation, model);

        if (this.offset != null) {
            this.stand = new Entity(EntityType.ITEM_DISPLAY) {
                @Override
                public void tick(long time) {}
            };

            var meta = (ItemDisplayMeta) this.stand.getEntityMeta();

            meta.setScale(new Vec(1, 1, 1));
            meta.setDisplayContext(ItemDisplayMeta.DisplayContext.FIXED);
            meta.setInterpolationDuration(3);
            ModelBoneImpl.hookPart(this, forwardTo);
        }
    }

    @Override
    public Pos calculatePosition() {
        if (this.offset == null) return Pos.ZERO;

        Point p = this.offset;
        p = applyTransform(p);
        p = calculateGlobalRotation(p);

        Pos endPos = Pos.fromPoint(p);

        return Pos.fromPoint(endPos).div(4);
    }

    @Override
    public Point calculateRotation() {
        Quaternion q = calculateFinalAngle(new Quaternion(getPropogatedRotation()));
        if (model.getGlobalRotation() != 0) {
            Quaternion pq = new Quaternion(new Vec(0, this.model.getGlobalRotation(), 0));
            q = pq.multiply(q);
        }

        return q.toEulerYZX();
    }

    public void draw() {
        this.children.forEach(ModelBone::draw);
        if (this.offset == null) return;

        sendTick++;

        if (sendTick % 2 == 0 && this.stand != null && this.stand.getEntityMeta() instanceof ItemDisplayMeta meta) {
            var position = calculatePosition();
            Quaternion q = calculateFinalAngle(new Quaternion(getPropogatedRotation()));
            if (model.getGlobalRotation() != 0) {
                Quaternion pq = new Quaternion(new Vec(0, this.model.getGlobalRotation(), 0));
                q = pq.multiply(q);
            }

            meta.setNotifyAboutChanges(false);
            meta.setInterpolationStartDelta(0);
            meta.setRightRotation(new float[] {(float) q.x(), (float) q.y(), (float) q.z(), (float) q.w()});
            meta.setTranslation(position);
            meta.setNotifyAboutChanges(true);
        }

        stand.teleport(Pos.fromPoint(model.getPosition()));
    }

    @Override
    public void setState(String state) {
        if (this.stand != null && this.stand.getEntityMeta() instanceof ItemDisplayMeta meta) {
            if (state.equals("invisible")) {
                meta.setItemStack(ItemStack.AIR);
                meta.setViewRange(100);
                return;
            }

            var item = this.items.get(state);
            if (item != null) {
                meta.setItemStack(item);
            }
        }
    }
}
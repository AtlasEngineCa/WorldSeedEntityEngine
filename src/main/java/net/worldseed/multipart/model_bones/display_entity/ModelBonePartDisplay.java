package net.worldseed.multipart.model_bones.display_entity;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.worldseed.multipart.BundlePacket;
import net.worldseed.multipart.GenericModel;
import net.worldseed.multipart.ModelConfig;
import net.worldseed.multipart.Quaternion;
import net.worldseed.multipart.model_bones.ModelBone;
import net.worldseed.multipart.model_bones.ModelBoneImpl;
import net.worldseed.multipart.model_bones.ModelBoneViewable;

import java.util.concurrent.CompletableFuture;

public class ModelBonePartDisplay extends ModelBoneImpl implements ModelBoneViewable {
    int sendTick = 0;
    Point rootPosition = null;
    Point movingTowards = null;

    public ModelBonePartDisplay(Point pivot, String name, Point rotation, GenericModel model, ModelConfig config, LivingEntity forwardTo) {
        super(pivot, name, rotation, model);

        if (this.offset != null) {
            this.stand = new Entity(EntityType.ITEM_DISPLAY) {
                @Override
                public void tick(long time) {}
            };

            var meta = (ItemDisplayMeta) this.stand.getEntityMeta();

            meta.setScale(new Vec(1, 1, 1));
            meta.setDisplayContext(ItemDisplayMeta.DisplayContext.THIRD_PERSON_LEFT_HAND);
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
        return Pos.fromPoint(p).div(4).withView(0, 0);
    }

    @Override
    public Point calculateRotation() {
        Quaternion q = calculateFinalAngle(new Quaternion(getPropogatedRotation()));
        return q.toEuler();
    }

    private static final BundlePacket bundlePacket = new BundlePacket();

    private void updateRoot() {
        if (this.stand.getEntityMeta() instanceof ItemDisplayMeta meta) {
            var position = calculatePosition();
            rootPosition = movingTowards;

            var viewers = this.stand.getViewers();

            meta.setInterpolationDuration(-1);

            viewers.forEach(viewer -> viewer.sendPacket(bundlePacket));

            Quaternion q = calculateFinalAngle(new Quaternion(getPropogatedRotation()));
            Quaternion pq = new Quaternion(new Vec(0, 180 - this.model.getGlobalRotation(), 0));
            q = pq.multiply(q);

            meta.setNotifyAboutChanges(false);
            meta.setInterpolationStartDelta(0);
            meta.setRightRotation(new float[]{(float) q.x(), (float) q.y(), (float) q.z(), (float) q.w()});
            meta.setNotifyAboutChanges(true);

            meta.setTranslation(Pos.ZERO);
            stand.teleport(Pos.fromPoint(movingTowards));

            viewers.forEach(viewer -> viewer.sendPacket(bundlePacket));
            meta.setInterpolationDuration(3);
            
            movingTowards = position;
        }
    }

    public void draw() {
        this.children.forEach(ModelBone::draw);
        if (this.offset == null) return;

        var position = calculatePosition();
        var finalPosition = model.getPosition().add(position);

        if (sendTick % 2 == 0 && this.stand != null && this.stand.getEntityMeta() instanceof ItemDisplayMeta meta) {
            if (rootPosition.distanceSquared(finalPosition) > 00) {
                updateRoot();
                return;
            }

            Quaternion q = calculateFinalAngle(new Quaternion(getPropogatedRotation()));
            Quaternion pq = new Quaternion(new Vec(0, 180 - this.model.getGlobalRotation(), 0));
            q = pq.multiply(q);

            var diff = finalPosition.sub(rootPosition);

            meta.setNotifyAboutChanges(false);
            meta.setInterpolationStartDelta(0);
            meta.setRightRotation(new float[]{(float) q.x(), (float) q.y(), (float) q.z(), (float) q.w()});
            meta.setTranslation(diff);
            meta.setNotifyAboutChanges(true);

            movingTowards = finalPosition;
        }

        sendTick++;
    }

    @Override
    public CompletableFuture<Void> spawn(Instance instance, Point position) {
        this.rootPosition = position.add(model.getPosition());
        movingTowards = rootPosition;
        return super.spawn(instance, position);
    }

    @Override
    public void setState(String state) {
        if (this.stand != null && this.stand.getEntityMeta() instanceof ItemDisplayMeta meta) {
            if (state.equals("invisible")) {
                meta.setItemStack(ItemStack.AIR);
                meta.setViewRange(500);
                return;
            }

            var item = this.items.get(state);
            if (item != null) {
                meta.setItemStack(item);
            }
        }
    }
}
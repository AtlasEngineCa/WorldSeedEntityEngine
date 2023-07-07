package net.worldseed.multipart.model_bones.display_entity;

import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.entity.metadata.other.ArmorStandMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.worldseed.multipart.GenericModel;
import net.worldseed.multipart.ModelConfig;
import net.worldseed.multipart.Quaternion;
import net.worldseed.multipart.model_bones.ModelBone;
import net.worldseed.multipart.model_bones.ModelBoneImpl;
import net.worldseed.multipart.model_bones.ModelBoneViewable;

import java.util.concurrent.CompletableFuture;

public class ModelBonePartDisplay extends ModelBoneImpl implements ModelBoneViewable {
    int sendTick = 0;
    private Entity baseStand;
    private Pos lastPos = null;

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
            meta.setViewRange(1000);
            ModelBoneImpl.hookPart(this, forwardTo);
        }
    }

    private Entity getBaseStand() {
        if (this.getParent() instanceof ModelBonePartDisplay display) return display.getBaseStand();
        return baseStand;
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

    public void draw() {
        this.children.forEach(ModelBone::draw);
        if (this.offset == null) return;

        var position = calculatePosition();

        if (sendTick % 2 == 0 && this.stand != null && this.stand.getEntityMeta() instanceof ItemDisplayMeta meta) {
            Quaternion q = calculateFinalAngle(new Quaternion(getPropogatedRotation()));
            Quaternion pq = new Quaternion(new Vec(0, 180 - this.model.getGlobalRotation(), 0));
            q = pq.multiply(q);

            meta.setNotifyAboutChanges(false);
            meta.setInterpolationStartDelta(0);
            meta.setRightRotation(new float[]{(float) q.x(), (float) q.y(), (float) q.z(), (float) q.w()});
            meta.setTranslation(position);
            meta.setNotifyAboutChanges(true);
        }

        if (this.getParent() == null && !lastPos.samePoint(model.getPosition())) {
            this.baseStand.teleport(Pos.fromPoint(model.getPosition()).add(0, 1, 0));
            this.lastPos = Pos.fromPoint(model.getPosition());
        }

        sendTick++;
    }

    @Override
    public CompletableFuture<Void> spawn(Instance instance, Point position) {
        this.lastPos = Pos.fromPoint(position);

        return super.spawn(instance, position).whenCompleteAsync((v, e) -> {
            if (e != null) {
                e.printStackTrace();
                return;
            }

            if (this.getParent() == null) {
                this.baseStand = new Entity(EntityType.ARMOR_STAND);
                ArmorStandMeta meta = (ArmorStandMeta) this.baseStand.getEntityMeta();
                meta.setMarker(true);

                this.baseStand.setInstance(instance, position).join();

                if (this.stand != null) {
                    this.baseStand.setNoGravity(true);
                    this.baseStand.addPassenger(this.stand);
                }
            }

            MinecraftServer.getSchedulerManager().scheduleNextTick(() -> {
                getBaseStand().addPassenger(this.stand);
            });
        });
    }

    @Override
    public void setState(String state) {
        if (this.stand != null && this.stand.getEntityMeta() instanceof ItemDisplayMeta meta) {
            if (state.equals("invisible")) {
                meta.setItemStack(ItemStack.AIR);
                return;
            }

            var item = this.items.get(state);
            if (item != null) {
                meta.setItemStack(item);
            }
        }
    }
}
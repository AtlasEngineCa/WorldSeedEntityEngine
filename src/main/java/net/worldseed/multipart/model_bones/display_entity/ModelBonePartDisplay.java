package net.worldseed.multipart.model_bones.display_entity;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.entity.metadata.other.ArmorStandMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.packet.server.play.SetPassengersPacket;
import net.worldseed.multipart.GenericModel;
import net.worldseed.multipart.Quaternion;
import net.worldseed.multipart.model_bones.BoneEntity;
import net.worldseed.multipart.model_bones.ModelBone;
import net.worldseed.multipart.model_bones.ModelBoneImpl;
import net.worldseed.multipart.model_bones.ModelBoneViewable;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ModelBonePartDisplay extends ModelBoneImpl implements ModelBoneViewable {
    int sendTick = 0;
    private Entity baseStand;

    @Override
    public void addViewer(Player player) {
        if (this.stand != null) this.stand.addViewer(player);
        if (this.baseStand != null) this.baseStand.addViewer(player);
    }

    @Override
    public void setScale(float scale) {
        super.setScale(scale);

        if (this.stand != null) {
            var meta = (ItemDisplayMeta) this.stand.getEntityMeta();
            meta.setScale(new Vec(scale, scale, scale));
        }
    }

    @Override
    public void removeViewer(Player player) {
        if (this.stand != null) this.stand.removeViewer(player);
        if (this.baseStand != null) this.baseStand.removeViewer(player);
    }

    public ModelBonePartDisplay(Point pivot, String name, Point rotation, GenericModel model, float scale) {
        super(pivot, name, rotation, model, scale);

        if (this.offset != null) {
            this.stand = new BoneEntity(EntityType.ITEM_DISPLAY, model);

            var itemMeta = (ItemDisplayMeta) this.stand.getEntityMeta();

            itemMeta.setScale(new Vec(scale, scale, scale));
            itemMeta.setDisplayContext(ItemDisplayMeta.DisplayContext.THIRD_PERSON_LEFT_HAND);
            itemMeta.setTransformationInterpolationDuration(3);
            itemMeta.setPosRotInterpolationDuration(3);
            itemMeta.setViewRange(1000);
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        if (this.baseStand != null) {
            this.baseStand.remove();
        }
    }

    @Override
    public Pos calculatePosition() {
        return Pos.fromPoint(model.getPosition()).withView(0, 0);
    }

    private Pos calculatePositionInternal() {
        if (this.offset == null) return Pos.ZERO;
        Point p = this.offset;
        p = applyTransform(p);
        p = calculateGlobalRotation(p);
        return Pos.fromPoint(p).div(4).mul(scale).withView(0, 0);
    }

    @Override
    public Point calculateRotation() {
        Quaternion q = calculateFinalAngle(new Quaternion(getPropogatedRotation()));
        return q.toEuler();
    }

    public void draw() {
        if (this.baseStand != null && !baseStand.getPosition().samePoint(model.getPosition())) {
            this.baseStand.teleport(Pos.fromPoint(model.getPosition()));
        }

        this.children.forEach(ModelBone::draw);
        if (this.offset == null) return;

        if (sendTick % 2 == 0 && this.stand != null && this.stand.getEntityMeta() instanceof ItemDisplayMeta meta) {
            var position = calculatePositionInternal();
            Quaternion q = calculateFinalAngle(new Quaternion(getPropogatedRotation()));
            Quaternion pq = new Quaternion(new Vec(0, 180 - this.model.getGlobalRotation(), 0));
            q = pq.multiply(q);

            meta.setNotifyAboutChanges(false);
            meta.setTransformationInterpolationStartDelta(0);
            meta.setRightRotation(new float[]{(float) q.x(), (float) q.y(), (float) q.z(), (float) q.w()});
            meta.setTranslation(position);
            meta.setNotifyAboutChanges(true);
        }

        sendTick++;
    }

    @Override
    public CompletableFuture<Void> spawn(Instance instance, Point position) {
        return super.spawn(instance, position).whenCompleteAsync((v, e) -> {
            if (e != null) {
                e.printStackTrace();
                return;
            }

            if (!(this.getParent() instanceof ModelBonePartDisplay)) {
                this.baseStand = new BoneEntity(EntityType.ARMOR_STAND, model) {
                    @Override
                    public void updateNewViewer(@NotNull Player player) {
                        super.updateNewViewer(player);

                        List<Integer> parts = model.getParts().stream()
                                .map(ModelBone::getEntity)
                                .filter(e -> e != null && e.getEntityType() == EntityType.ITEM_DISPLAY)
                                .map(Entity::getEntityId)
                                .toList();
                        SetPassengersPacket packet = new SetPassengersPacket(baseStand.getEntityId(), parts);
                        player.sendPacket(packet);
                    }
                };
                ArmorStandMeta meta = (ArmorStandMeta) this.baseStand.getEntityMeta();
                meta.setMarker(true);

                this.baseStand.setInvisible(true);
                this.baseStand.setNoGravity(true);
                this.baseStand.setInstance(instance, position).join();
            }
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
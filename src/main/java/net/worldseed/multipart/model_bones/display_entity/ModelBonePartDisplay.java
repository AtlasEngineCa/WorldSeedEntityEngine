package net.worldseed.multipart.model_bones.display_entity;

import net.kyori.adventure.util.RGBLike;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Metadata;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.packet.server.play.EntityMetaDataPacket;
import net.worldseed.multipart.GenericModel;
import net.worldseed.multipart.Quaternion;
import net.worldseed.multipart.model_bones.BoneEntity;
import net.worldseed.multipart.model_bones.ModelBone;
import net.worldseed.multipart.model_bones.ModelBoneImpl;
import net.worldseed.multipart.model_bones.ModelBoneViewable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ModelBonePartDisplay extends ModelBoneImpl implements ModelBoneViewable {
    private final List<GenericModel> attached = new ArrayList<>();
    private Entity baseStand;

    public ModelBonePartDisplay(Point pivot, String name, Point rotation, GenericModel model, float scale) {
        super(pivot, name, rotation, model, scale);

        if (this.offset != null) {
            this.stand = new BoneEntity(EntityType.ITEM_DISPLAY, model, name);

            var itemMeta = (ItemDisplayMeta) this.stand.getEntityMeta();

            itemMeta.setScale(new Vec(scale, scale, scale));
            itemMeta.setDisplayContext(ItemDisplayMeta.DisplayContext.THIRD_PERSON_LEFT_HAND);
            itemMeta.setTransformationInterpolationDuration(2);
            itemMeta.setPosRotInterpolationDuration(2);
            itemMeta.setViewRange(1000);
        }
    }

    @Override
    public void addViewer(Player player) {
        if (this.stand != null) this.stand.addViewer(player);
        if (this.baseStand != null) this.baseStand.addViewer(player);
        this.attached.forEach(model -> model.addViewer(player));
    }

    @Override
    public void removeGlowing() {
        if (this.stand != null) {
            var meta = (ItemDisplayMeta) this.stand.getEntityMeta();
            meta.setHasGlowingEffect(false);
        }

        this.attached.forEach(GenericModel::removeGlowing);
    }

    @Override
    public void setGlowing(RGBLike color) {
        if (this.stand != null) {
            int rgb = 0;
            rgb |= color.red() << 16;
            rgb |= color.green() << 8;
            rgb |= color.blue();

            var meta = (ItemDisplayMeta) this.stand.getEntityMeta();
            meta.setHasGlowingEffect(true);
            meta.setGlowColorOverride(rgb);
        }

        this.attached.forEach(model -> model.setGlowing(color));
    }

    @Override
    public void removeGlowing(Player player) {
        if (this.stand == null)
            return;

        EntityMetaDataPacket oldMetadataPacket = this.stand.getMetadataPacket();
        Map<Integer, Metadata.Entry<?>> oldEntries = oldMetadataPacket.entries();
        byte previousFlags = oldEntries.containsKey(0)
                ? (byte) oldEntries.get(0).value()
                : 0;

        Map<Integer, Metadata.Entry<?>> entries = new HashMap<>(oldMetadataPacket.entries());
        entries.put(0, Metadata.Byte((byte) (previousFlags & ~0x40)));
        entries.put(22, Metadata.VarInt(-1));

        player.sendPacket(new EntityMetaDataPacket(this.stand.getEntityId(), entries));
        this.attached.forEach(model -> model.removeGlowing(player));
    }

    @Override
    public void setGlowing(Player player, RGBLike color) {
        if (this.stand == null)
            return;

        int rgb = 0;
        rgb |= color.red() << 16;
        rgb |= color.green() << 8;
        rgb |= color.blue();

        EntityMetaDataPacket oldMetadataPacket = this.stand.getMetadataPacket();
        Map<Integer, Metadata.Entry<?>> oldEntries = oldMetadataPacket.entries();
        byte previousFlags = oldEntries.containsKey(0)
                ? (byte) oldEntries.get(0).value()
                : 0;

        Map<Integer, Metadata.Entry<?>> entries = new HashMap<>(oldEntries);
        entries.put(0, Metadata.Byte((byte) (previousFlags | 0x40)));
        entries.put(22, Metadata.VarInt(rgb));

        player.sendPacket(new EntityMetaDataPacket(this.stand.getEntityId(), entries));
        this.attached.forEach(model -> model.setGlowing(player, color));
    }

    @Override
    public void attachModel(GenericModel model) {
        attached.add(model);
    }

    @Override
    public List<GenericModel> getAttachedModels() {
        return attached;
    }

    @Override
    public void detachModel(GenericModel model) {
        attached.remove(model);
    }

    @Override
    public void setGlobalRotation(double rotation) {
        if (this.stand != null) {
            var correctLocation = (180 + this.model.getGlobalRotation() + 360) % 360;
            this.stand.setView((float) correctLocation, 0);
        }
    }

    @Override
    public void removeViewer(Player player) {
        if (this.stand != null) this.stand.removeViewer(player);
        if (this.baseStand != null) this.baseStand.removeViewer(player);
        this.attached.forEach(model -> model.removeViewer(player));
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
        return Pos.fromPoint(p).div(4).mul(scale).withView(0, 0);
    }

    @Override
    public Point calculateRotation() {
        Quaternion q = calculateFinalAngle(new Quaternion(getPropogatedRotation()));
        return q.toEuler();
    }

    @Override
    public Point calculateScale() {
        return calculateFinalScale(getPropogatedScale());
    }

    @Override
    public void teleport(Point position) {
        if (this.baseStand != null) this.baseStand.teleport(Pos.fromPoint(position));
    }

    public void draw() {
        this.children.forEach(ModelBone::draw);
        if (this.offset == null) return;

        if (this.stand != null) {
            var position = calculatePositionInternal();
            var scale = calculateScale();

            if (this.stand.getEntityMeta() instanceof ItemDisplayMeta meta) {
                Quaternion q = calculateFinalAngle(new Quaternion(getPropogatedRotation()));

                meta.setNotifyAboutChanges(false);
                meta.setTransformationInterpolationStartDelta(0);
                meta.setScale(new Vec(scale.x() * this.scale, scale.y() * this.scale, scale.z() * this.scale));
                meta.setRightRotation(new float[]{(float) q.x(), (float) q.y(), (float) q.z(), (float) q.w()});
                meta.setTranslation(position);
                meta.setNotifyAboutChanges(true);

                attached.forEach(model -> {
                    model.setPosition(this.model.getPosition().add(calculateGlobalRotation(position)));
                    model.setGlobalRotation(-q.toEuler().x() + this.model.getGlobalRotation());
                    model.draw();
                });
            }
        }
    }

    @Override
    public CompletableFuture<Void> spawn(Instance instance, Pos position) {
        var correctLocation = (180 + this.model.getGlobalRotation() + 360) % 360;
        return super.spawn(instance, Pos.fromPoint(position).withYaw((float) correctLocation)).whenCompleteAsync((v, e) -> {
            if (e != null) {
                e.printStackTrace();
                return;
            }

            if (!(this.getParent() instanceof ModelBonePartDisplay)) {
                this.baseStand = model.generateRoot();
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

    @Override
    public Point getPosition() {
        return calculatePositionInternal().add(model.getPosition());
    }
}